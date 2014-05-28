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
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import uk.bowdlerize.MainActivity;
import uk.bowdlerize.R;

public class AboutFragment extends Fragment
{
    int tapCount = 0;

    public AboutFragment()
    {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_about, container, false);

        rootView.findViewById(R.id.callToActionButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://www.openrightsgroup.org/join/")));
            }
        });

        String aboutMeta = getString(R.string.aboutVersion) + " ";

        PackageInfo packageInfo = null;

        try {
            packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            aboutMeta += packageInfo.versionName + " ( " + Integer.toString(packageInfo.versionCode) + " )";
        } catch (Exception e) {
            aboutMeta += "Unknown";
        }
        ;

        aboutMeta += "\n\n";

        aboutMeta += getString(R.string.aboutCompiledBy) + " " + "Gareth Llewellyn\n\n";

        /*if (null != packageInfo)
        {
            aboutMeta += packageInfo.
        }*/

        ((TextView) rootView.findViewById(R.id.aboutMeta)).setText(aboutMeta);


        rootView.findViewById(R.id.aboutMeta).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                tapCount++;

                if(tapCount == 10)
                {
                    try
                    {
                        Toast.makeText(getActivity(), getString(R.string.aboutPowerUser), Toast.LENGTH_SHORT).show();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }

                if(tapCount == 20)
                {
                    try
                    {
                        Toast.makeText(getActivity(),getString(R.string.aboutPowerUserComplete),Toast.LENGTH_LONG).show();


                        SharedPreferences.Editor editor = getActivity().getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE).edit();
                        editor.putBoolean("poweruser", true);
                        editor.commit();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        });

        return rootView;
    }
}
