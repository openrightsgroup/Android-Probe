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

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import uk.bowdlerize.API;
import uk.bowdlerize.MainActivity;
import uk.bowdlerize.R;
import uk.bowdlerize.support.Hashes;

public class OrgFragment extends Fragment
{
    public static final int PROBE_PENDING = 3;
    public static final int PROBE_COMPLETE = 4;
    private View progressBar;
    API api;
    private Callbacks mCallbacks = sDummyCallbacks;
    SharedPreferences settings;

    public interface Callbacks
    {
        /**
         * Callback for when an item has been selected.
         */
        public void updateStatus(int type);
    }

    private static Callbacks sDummyCallbacks = new Callbacks()
    {
        @Override
        public void updateStatus(int type)
        {
        }
    };

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks))
        {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    public OrgFragment()
    {
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        api = new API(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_org, container, false);

        settings = getActivity().getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);

        progressBar = rootView.findViewById(R.id.progressBar);

        ((Button) rootView.findViewById(R.id.GenProbeButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                progressBar.setVisibility(View.VISIBLE);
                registerProbe();
            }
        });


        return rootView;
    }

    private void registerProbe()
    {
        new AsyncTask<Void, Void, String>()
        {
            @Override
            protected String doInBackground(Void... params)
            {
                try
                {
                    String probeHMAC = api.prepareProbe();
                    if(null == probeHMAC || probeHMAC.equals(""))
                    {
                        return null;
                    }
                    else
                    {
                        publishProgress();
                        String probe_seed = Hashes.MD5(Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID));
                        String UUID = Hashes.MD5(probe_seed + "-" +  probeHMAC);

                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(API.SETTINGS_UUID,UUID);
                        editor.commit();

                        return api.registerProbe(probe_seed, UUID,"");
                    }
                }
                catch (Exception e)
                {
                    //Lazy I know
                    e.printStackTrace();
                    return null;
                }
            }


            protected void onProgressUpdate(Void... test)
            {
                mCallbacks.updateStatus(PROBE_PENDING);
            }

            @Override
            protected void onPostExecute(String probePrivKey)
            {
                progressBar.setVisibility(View.GONE);

                if(null == probePrivKey || probePrivKey.equals(""))
                {
                    Toast.makeText(getActivity(),"There was a problem getting the private key for this probe",Toast.LENGTH_LONG).show();
                }
                else
                {
                    mCallbacks.updateStatus(PROBE_COMPLETE);

                    /*String tmpPK = probePrivKey.replace("-----BEGIN RSA PRIVATE KEY-----", "");
                    tmpPK = tmpPK.replace("-----END RSA PRIVATE KEY-----", "");
                    tmpPK = tmpPK.replace("\n", "");*/

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(API.SETTINGS_PROBE_PRIVATE_KEY,probePrivKey);
                    editor.commit();
                    Toast.makeText(getActivity(),"Probe registration complete!",Toast.LENGTH_LONG).show();

                    Intent in = new Intent();
                    getActivity().setResult(Activity.RESULT_OK,in);
                    getActivity().finish();
                }
            }
        }.execute(null, null, null);
    }
}