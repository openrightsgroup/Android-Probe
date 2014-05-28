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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import uk.bowdlerize.API;
import uk.bowdlerize.MainActivity;
import uk.bowdlerize.R;
import uk.bowdlerize.cache.LocalCache;
import uk.bowdlerize.support.ISPMeta;

public class WirelessConfigFragment extends Fragment
{
    SharedPreferences settings;
    TextView ISPName;
    View saveData;
    View newNetworkIcon;
    BroadcastReceiver receiver;
    IntentFilter filter;
    TextView mobileNet;
    TextView simNet;
    TextView wifiNet;
    TextView censorLevel;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                String action = intent.getAction();

                if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION))
                {
                    return;
                }

                boolean noConnectivity = intent.getBooleanExtra( ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                NetworkInfo aNetworkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

                boolean isDead = false;

                if (!noConnectivity)
                {
                    if ((aNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) || (aNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI))
                    {
                        // Handle connected case
                        Log.e("Connectivity","We're connected!");
                        isDead = false;
                    }
                }
                else
                {
                    if ((aNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) || (aNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI))
                    {
                        // Handle disconnected case
                        Log.e("Connectivity","Everything is dead Jim!");
                        isDead = true;
                    }
                }

                refreshNetMeta(isDead);
            }
        };
    }

    @Override
    public void onResume()
    {
        super.onResume();
        filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        getActivity().registerReceiver(receiver, filter);
    }

    @Override
    public void onPause()
    {
        Log.w("onPause", "Pausing, unregistering...");
        super.onPause();
        try
        {
            getActivity().unregisterReceiver(receiver);
        }
        catch (IllegalArgumentException e)
        {
            if (e.getMessage().contains("Receiver not registered"))
            {
                // Ignore this exception. This is exactly what is desired
                Log.w("onPause", "Tried to unregister the receiver when it's not registered");
            }
            else
            {
                // unexpected, re-throw
                throw e;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_wireless, container, false);

        settings = getActivity().getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);


        //Checkbox for sending this along
        /*((CheckBox) rootView.findViewById(R.id.sendDataCB)).setChecked(settings.getBoolean("sendISPMeta", true));
        ((CheckBox) rootView.findViewById(R.id.sendDataCB)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                saveData.setVisibility(View.VISIBLE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("sendISPMeta",isChecked);
                editor.commit();
                saveData.setVisibility(View.INVISIBLE);
            }
        });*/
        mobileNet = ((TextView) rootView.findViewById(R.id.mobileNetwork));
        simNet = ((TextView) rootView.findViewById(R.id.simNetwork));
        wifiNet = ((TextView) rootView.findViewById(R.id.wifiNetwork));
        ISPName = ((TextView) rootView.findViewById(R.id.WiFiISPET));
        censorLevel = ((TextView) rootView.findViewById(R.id.censorLevel));

        newNetworkIcon = rootView.findViewById(R.id.newNetworkIcon);
        saveData = rootView.findViewById(R.id.progressBar);

        return rootView;
    }

    private void refreshNetMeta(boolean allDead)
    {
        if(allDead)
        {
            mobileNet.setText(getString(R.string.offlineLabel));
            simNet.setText(R.string.offlineLabel);
            wifiNet.setText(R.string.offlineLabel);
            ISPName.setText(R.string.offlineLabel);

            return;
        }

        ISPName.setText("");

        //Mobile stuff
        try
        {
            TelephonyManager telephonyManager =((TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE));

            String mobileNetName = telephonyManager.getNetworkOperatorName();

            if(mobileNetName.equals(""))
                mobileNetName = getString(R.string.unknownLabel);

            mobileNet.setText(mobileNetName);

            String simNetName = telephonyManager.getSimOperatorName();

            if(simNetName.equals("") || simNetName.isEmpty())
            {
                simNet.setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));
                simNet.setText(getString(R.string.unknownLabel));
            }
            else
            {
                simNet.setText(simNetName);
                simNet.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            }

            Log.e("SIM",simNetName);
        }
        catch (NullPointerException npe)
        {
            npe.printStackTrace();
        }

        //WiFi Stuff
        String wifiName = "";

        try
        {
            WifiManager wifiMgr = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

            Log.e("WiFi", wifiName + " / " + wifiInfo.getBSSID() + " / " + wifiInfo.getNetworkId());

            if(wifiInfo.getNetworkId() == -1)
            {
                wifiNet.setText(getString(R.string.disconLabel));
                wifiNet.setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));
            }
            else
            {
                wifiName = wifiInfo.getSSID();
                wifiNet.setText(wifiName.replaceAll("\"", ""));
                wifiNet.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        //Lets Poll for who the new ISP is
        saveData.setVisibility(View.VISIBLE);
        /*newNetworkIcon.setVisibility(View.VISIBLE);
        Animation shakeIt = AnimationUtils.loadAnimation(getActivity(), R.anim.wobble);
        newNetworkIcon.startAnimation(shakeIt);

        newNetworkIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), getString(R.string.wirelessNew), Toast.LENGTH_SHORT).show();
                Animation shakeIt = AnimationUtils.loadAnimation(getActivity(), R.anim.wobble);
                v.startAnimation(shakeIt);
            }
        });*/

        saveData.setVisibility(View.VISIBLE);

        new AsyncTask<Void, Void, Pair<String,String>>()
        {
            @Override
            protected Pair<String,String> doInBackground(Void... params)
            {
                API api = new API(getActivity());
                ISPMeta ispMeta = api.getISPMeta();
                return new Pair<String,String>(ispMeta.ipAddress,ispMeta.ispName);
            }

            @Override
            protected void onPostExecute(Pair<String,String> ispMeta)
            {
                LocalCache lc = null;
                try
                {
                    //ISP is no longer tied to the SSID
                    /*WifiManager wifiMgr = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

                    lc = new LocalCache(getActivity());
                    lc.open();
                    lc.addSSID(wifiInfo.getSSID().replaceAll("\"",""),ispMeta.second);*/

                    ISPName.setText(ispMeta.second);
                    /*Animation animationFadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.fadeout);
                    newNetworkIcon.startAnimation(animationFadeOut);
                    animationFadeOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation)
                        {
                            newNetworkIcon.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });

                    saveData.setVisibility(View.INVISIBLE);*/

                    Animation animationFadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.fadeout);
                    saveData.startAnimation(animationFadeOut);
                    animationFadeOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) { }

                        @Override
                        public void onAnimationEnd(Animation animation){ saveData.setVisibility(View.INVISIBLE); }

                        @Override
                        public void onAnimationRepeat(Animation animation) {}
                    });
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }.execute();


        new AsyncTask<Void, Void, Integer>()
        {
            @Override
            protected Integer doInBackground(Void... params)
            {
                try {
                    API api = new API(getActivity());
                    return api.ascertainFilteringLevel();
                }
                catch (Exception e)
                {
                    return -1;
                }
            }

            @Override
            protected void onPostExecute(Integer filteringLevel)
            {
                if(null == filteringLevel || filteringLevel < 0)
                {
                    censorLevel.setText(getString(R.string.filterIndeterLabel));
                }
                else if(filteringLevel == API.FILTERING_STRICT)
                {
                    censorLevel.setText(getString(R.string.filterStrictLabel));
                }
                else if(filteringLevel == API.FILTERING_MEDIUM)
                {
                    censorLevel.setText(getString(R.string.filterMedLabel));
                }
                else
                {
                    censorLevel.setText(getString(R.string.filterNoneLabel));
                }
            }
        }.execute();

        //If wifi is a new name
        /*LocalCache lc = null;
        Pair<Boolean,String> seenBefore = null;
        try
        {
            lc = new LocalCache(getActivity());
            lc.open();
            seenBefore = lc.findSSID(wifiName.replaceAll("\"",""));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if(null != lc)
            lc.close();

        if(seenBefore.first)
        {
            ISPName.setText(seenBefore.second);
            ISPName.setEnabled(false);
            newNetworkIcon.setVisibility(View.GONE);
        }
        else
        {
            saveData.setVisibility(View.VISIBLE);
            newNetworkIcon.setVisibility(View.VISIBLE);
            Animation shakeIt = AnimationUtils.loadAnimation(getActivity(), R.anim.wobble);
            newNetworkIcon.startAnimation(shakeIt);

            newNetworkIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getActivity(), getString(R.string.wirelessNew), Toast.LENGTH_SHORT).show();
                    Animation shakeIt = AnimationUtils.loadAnimation(getActivity(), R.anim.wobble);
                    v.startAnimation(shakeIt);
                }
            });

            saveData.setVisibility(View.VISIBLE);

            new AsyncTask<Void, Void, Pair<String,String>>()
            {
                @Override
                protected Pair<String,String> doInBackground(Void... params)
                {
                    API api = new API(getActivity());
                    ISPMeta ispMeta = api.getISPMeta();
                    return new Pair<String,String>(ispMeta.ipAddress,ispMeta.ispName);
                }

                @Override
                protected void onPostExecute(Pair<String,String> ispMeta)
                {
                    LocalCache lc = null;
                    try
                    {
                        WifiManager wifiMgr = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
                        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

                        lc = new LocalCache(getActivity());
                        lc.open();
                        lc.addSSID(wifiInfo.getSSID().replaceAll("\"",""),ispMeta.second);

                        ISPName.setText(ispMeta.second);
                        Animation animationFadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.fadeout);
                        newNetworkIcon.startAnimation(animationFadeOut);
                        animationFadeOut.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation)
                            {
                                newNetworkIcon.setVisibility(View.INVISIBLE);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });

                        saveData.setVisibility(View.INVISIBLE);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }.execute();
        }*/
    }
}
