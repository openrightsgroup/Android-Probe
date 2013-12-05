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
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import uk.bowdlerize.MainActivity;
import uk.bowdlerize.R;
import uk.bowdlerize.cache.LocalCache;

public class WirelessConfigFragment extends Fragment
{
    SharedPreferences settings;
    EditText ISPName;
    View saveData;
    View newNetworkIcon;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_wireless, container, false);

        settings = getActivity().getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);

        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //Mobile stuff
        TelephonyManager telephonyManager =((TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE));

        String mobileNet = telephonyManager.getNetworkOperatorName();

        if(mobileNet.equals(""))
            mobileNet = "Unknown";

        ((TextView) rootView.findViewById(R.id.mobileNetwork)).setText(mobileNet);

        String simNet = telephonyManager.getSimOperatorName();

        if(simNet.equals(""))
            simNet = "Unknown";

        ((TextView) rootView.findViewById(R.id.simNetwork)).setText(simNet);


        //WiFi Stuff
        WifiManager wifiMgr = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        String wifiName = wifiInfo.getSSID();

        Log.e("WiFi", wifiName + " / " + wifiInfo.getBSSID() + " / " + wifiInfo.getNetworkId());

        ((TextView) rootView.findViewById(R.id.wifiNetwork)).setText(wifiName.replaceAll("\"",""));

        //If wifi is new name
        LocalCache lc = null;
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

        newNetworkIcon = rootView.findViewById(R.id.newNetworkIcon);
        if(seenBefore.first)
        {
            ((EditText) rootView.findViewById(R.id.WiFiISPET)).setText(seenBefore.second);
            rootView.findViewById(R.id.WiFiISPET).setEnabled(false);
            newNetworkIcon.setVisibility(View.GONE);
            rootView.findViewById(R.id.SSIDNameSaveButton).setVisibility(View.INVISIBLE);
        }
        else
        {
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
        }

        ISPName = ((EditText) rootView.findViewById(R.id.WiFiISPET));
        rootView.findViewById(R.id.SSIDNameSaveButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v)
            {
                LocalCache lc = null;

                ISPName.setEnabled(false);

                try
                {
                    WifiManager wifiMgr = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

                    lc = new LocalCache(getActivity());
                    lc.open();
                    lc.addSSID(wifiInfo.getSSID().replaceAll("\"",""),ISPName.getText().toString());

                    Animation animationFadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.fadeout);
                    v.startAnimation(animationFadeOut);
                    newNetworkIcon.startAnimation(animationFadeOut);
                    animationFadeOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation)
                        {
                            v.setVisibility(View.INVISIBLE);
                            newNetworkIcon.setVisibility(View.INVISIBLE);

                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    v.setEnabled(false);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                if(null != lc)
                    lc.close();
            }
        });

        //Checkbox for sending this along
        saveData = rootView.findViewById(R.id.progressBar);
        ((CheckBox) rootView.findViewById(R.id.sendDataCB)).setChecked(settings.getBoolean("sendISPMeta", true));
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
        });

        return rootView;
    }
}
