/*
* Copyright (C) 2013 - Gareth Llewellyn
*
* This file is part of Bowdlerize - https://bowdlerize.co.uk
*
* This program is free software: you can redistribute it and/or modify it
* under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License
* for more details.
*
* You should have received a copy of the GNU General Public License along with
* this program. If not, see <http://www.gnu.org/licenses/>
*/
package uk.bowdlerize.fragments;

import android.app.Fragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import uk.bowdlerize.MainActivity;
import uk.bowdlerize.R;
import uk.bowdlerize.service.CensorCensusService;
import uk.bowdlerize.support.Hashes;

public class CheckConfigFragment extends Fragment
{
    SharedPreferences settings;
    TextView seekValue;
    ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_checkconfig, container, false);

        settings = getActivity().getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);

        progressBar = ((ProgressBar) rootView.findViewById(R.id.progressBar));

        seekValue = (TextView) rootView.findViewById(R.id.delayValueLabel);

        seekValue.setText("Wait a minimum of " + settings.getInt("maxDelay",2) + " minutes between requests" );
        ((SeekBar) rootView.findViewById(R.id.seekBar)).setProgress(settings.getInt("maxDelay", 1));
        ((SeekBar) rootView.findViewById(R.id.seekBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                //seekValue.setText("Waiting....");
                seekValue.setText("Wait a minimum of " + Integer.toString(progress) + " minutes between requests");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                final Integer value;

                if(seekBar.getProgress() == 0)
                {
                    value = 2;
                }
                else
                {
                    value = seekBar.getProgress();
                }

                progressBar.setVisibility(View.VISIBLE);

                seekValue.setText("Wait a minimum of " + Integer.toString(value) + " minutes between requests");

                SharedPreferences.Editor editor = settings.edit();
                editor.putInt("maxDelay", value);
                editor.commit();

                new AsyncTask<Void, Void, Boolean>()
                {
                    @Override
                    protected Boolean doInBackground(Void... params)
                    {
                        DefaultHttpClient httpclient = new DefaultHttpClient();
                        JSONObject json;
                        HttpPost httpost = new HttpPost("https://bowdlerize.co.uk/api/1/settimings.php");

                        httpost.setHeader("Accept", "application/json");

                        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                        nvps.add(new BasicNameValuePair("delay",Integer.toString(value)));
                        nvps.add(new BasicNameValuePair("deviceid", Hashes.MD5(Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID))));

                        try
                        {
                            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

                            HttpResponse response = httpclient.execute(httpost);
                            String rawJSON = EntityUtils.toString(response.getEntity());
                            response.getEntity().consumeContent();
                            Log.e("rawJSON", rawJSON);
                            json = new JSONObject(rawJSON);
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                        }

                        return true;
                    }

                    @Override
                    protected void onPostExecute(Boolean success)
                    {
                        //TODO if it fails we should retry
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                }.execute(null, null, null);
            }
        });


        ((CheckBox) rootView.findViewById(R.id.ccEnabled)).setChecked(settings.getBoolean("ccEnabled", true));
        ((CheckBox) rootView.findViewById(R.id.ccEnabled)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked)
            {
                progressBar.setVisibility(View.VISIBLE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("ccEnabled",isChecked);
                editor.commit();

                if(!isChecked)
                    ((NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE)).cancel(CensorCensusService.NOTIFICATION_ID);

                new AsyncTask<Void, Void, Boolean>()
                {
                    @Override
                    protected Boolean doInBackground(Void... params)
                    {
                        DefaultHttpClient httpclient = new DefaultHttpClient();
                        JSONObject json;
                        HttpPost httpost;
                        if(isChecked)
                        {
                            httpost = new HttpPost("https://bowdlerize.co.uk/api/1/enablegcm.php");
                        }
                        else
                        {
                            httpost = new HttpPost("https://bowdlerize.co.uk/api/1/disablegcm.php");
                        }

                        httpost.setHeader("Accept", "application/json");

                        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                        nvps.add(new BasicNameValuePair("deviceid", Hashes.MD5(Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID))));

                        try
                        {
                            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

                            HttpResponse response = httpclient.execute(httpost);
                            String rawJSON = EntityUtils.toString(response.getEntity());
                            response.getEntity().consumeContent();
                            Log.e("rawJSON", rawJSON);
                            json = new JSONObject(rawJSON);
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                        }

                        return true;
                    }

                    @Override
                    protected void onPostExecute(Boolean success)
                    {
                        //TODO if it fails we should retry
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                }.execute(null, null, null);
            }
        });

        return rootView;

    }
}
