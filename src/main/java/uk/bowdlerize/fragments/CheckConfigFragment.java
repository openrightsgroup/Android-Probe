/*
* Copyright (C) 2014 - Gareth Llewellyn
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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import uk.bowdlerize.API;
import uk.bowdlerize.MainActivity;
import uk.bowdlerize.R;
import uk.bowdlerize.service.CensorCensusService;


public class CheckConfigFragment extends Fragment
{
    SharedPreferences settings;
    TextView seekValue;
    ProgressBar progressBar;
    String[] strTimes = new String[]{"1 mins", "5 mins", "15 mins", "30 mins", "45 mins", "1 hour", "2 hours", "4 hours", "8 hours", "16 hours", "24 hours"};
    int[] intTimes = new int[]{1, 5, 15, 30, 45, 60, 120, 240, 480, 960, 1440};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_checkconfig, container, false);

        settings = getActivity().getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);

        progressBar = ((ProgressBar) rootView.findViewById(R.id.progressBar));

        seekValue = (TextView) rootView.findViewById(R.id.delayValueLabel);

        seekValue.setText("Wait a minimum of " + strTimes[settings.getInt(API.SETTINGS_FREQUENCY_SEEK,0)]  + " between requests" );

        ((SeekBar) rootView.findViewById(R.id.seekBar)).setProgress(settings.getInt(API.SETTINGS_FREQUENCY_SEEK, 0));

        ((SeekBar) rootView.findViewById(R.id.seekBar)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                seekValue.setText("Wait a minimum of " + strTimes[progress] + " between requests");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                //seekValue.setText("Wait a minimum of " +  strTimes[progress] + " minutes between requests");
                final int value = intTimes[seekBar.getProgress()];

                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(API.SETTINGS_FREQUENCY_SEEK, seekBar.getProgress());
                editor.putInt(API.SETTINGS_FREQUENCY, value);
                editor.commit();

                Intent pollingIntent = new Intent(getActivity(), CensorCensusService.class);
                pollingIntent.putExtra(API.EXTRA_POLL,true);
                getActivity().startService(pollingIntent);

                new AsyncTask<Void, Void, Boolean>()
                {
                    @Override
                    protected Boolean doInBackground(Void... params)
                    {
                        API api = new API(getActivity());

                        try
                        {
                            api.updateGCM(settings.getInt(API.SETTINGS_GCM_PREFERENCE,API.SETTINGS_GCM_FULL),value);
                            return true;
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            return false;
                        }
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

        int GCMPreference = settings.getInt(API.SETTINGS_GCM_PREFERENCE,API.SETTINGS_GCM_FULL);

        RadioButton gcmFull = (RadioButton) rootView.findViewById(R.id.gcmFull);
        RadioButton gcmPartial = (RadioButton) rootView.findViewById(R.id.gcmPartial);
        RadioButton gcmNone = (RadioButton) rootView.findViewById(R.id.gcmNone);

        if(GCMPreference == API.SETTINGS_GCM_PARTIAL)
        {
            gcmFull.setChecked(false);
            gcmPartial.setChecked(true);
            gcmNone.setChecked(false);
        }
        else if(GCMPreference == API.SETTINGS_GCM_DISABLED)
        {
            gcmFull.setChecked(false);
            gcmPartial.setChecked(false);
            gcmNone.setChecked(true);
        }
        else
        {
            gcmFull.setChecked(true);
            gcmPartial.setChecked(false);
            gcmNone.setChecked(false);
        }

        //If this is the version without Google Play services then disable the GCM options
        if(settings.getBoolean("no_google_play_services",false))
        {
            gcmFull.setEnabled(false);
            gcmPartial.setEnabled(false);
            //gcmNone.setChecked(true);
        }

        ((RadioGroup) rootView.findViewById(R.id.radioGroupConfig)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                SharedPreferences.Editor editor = settings.edit();
                final API api = new API(getActivity());
                final int progress = progressBar.getProgress();

                Intent pollingIntent = new Intent(getActivity(), CensorCensusService.class);
                //receiveURLIntent.putExtra("url","http://google.com");
                if(checkedId == R.id.gcmFull)
                {
                    editor.putInt(API.SETTINGS_GCM_PREFERENCE, API.SETTINGS_GCM_FULL);
                    pollingIntent.putExtra(API.EXTRA_POLL,false);

                    new Thread()
                    {
                        public void run()
                        {
                            try
                            {
                                api.updateGCM(API.SETTINGS_GCM_FULL,progress);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
                else if(checkedId == R.id.gcmPartial)
                {
                    editor.putInt(API.SETTINGS_GCM_PREFERENCE, API.SETTINGS_GCM_PARTIAL);
                    pollingIntent.putExtra(API.EXTRA_POLL,false);

                    new Thread()
                    {
                        public void run()
                        {
                            try
                            {
                                api.updateGCM(API.SETTINGS_GCM_PARTIAL,progress);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
                else
                {
                    editor.putInt(API.SETTINGS_GCM_PREFERENCE, API.SETTINGS_GCM_DISABLED);
                    pollingIntent.putExtra(API.EXTRA_POLL,true);

                    new Thread()
                    {
                        public void run()
                        {
                            try
                            {
                                api.updateGCM(API.SETTINGS_GCM_DISABLED,progress);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
                //Start the service with the new settings
                getActivity().startService(pollingIntent);
                editor.commit();
            }
        });



        return rootView;

    }

    /*private void updateGCM(final int type)
    {
        progressBar.setVisibility(View.VISIBLE);

        new AsyncTask<Void, Void, Boolean>()
        {
            @Override
            protected Boolean doInBackground(Void... params)
            {
                DefaultHttpClient httpclient = new DefaultHttpClient();
                JSONObject json;
                HttpPost httpost;

                httpost = new HttpPost("https://bowdlerize.co.uk/api/1.1/update/gcm");

                httpost.setHeader("Accept", "application/json");

                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new BasicNameValuePair("probe_uuid", settings.getString(API.SETTINGS_UUID,"")));
                nvps.add(new BasicNameValuePair("frequency", Integer.toString(progressBar.getProgress())));
                nvps.add(new BasicNameValuePair("gcm_id", settings.getString(API.PROPERTY_REG_ID,"")));
                nvps.add(new BasicNameValuePair("gcm_type", Integer.toString(type)));

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
    }*/
}
