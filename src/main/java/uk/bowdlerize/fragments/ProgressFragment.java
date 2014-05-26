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
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import uk.bowdlerize.MainActivity;
import uk.bowdlerize.R;

public class ProgressFragment extends Fragment
{
    float px = 0;
    ImageView bouncerUserORG;
    ImageView bouncerUserISP;
    ImageView bouncerISPWebsite;
    ImageView ispIV;
    ImageView serverIV;
    BroadcastReceiver receiver;
    static public String ORG_BROADCAST = "org_broadcast";
    static public int TALKING_TO_ORG = 0;
    static public int TALKING_TO_ISP = 1;
    static public int BLOCKED = 2;
    static public int OK = 3;
    static public int NO_URLS = 4;
    IntentFilter filter;

    public ProgressFragment()
    {
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if(intent.getIntExtra(ORG_BROADCAST,0) == TALKING_TO_ORG)
                {
                    AnimateUserToORG(true);
                    ispIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_neutral));
                    serverIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_browser_neutral));
                }
                else if(intent.getIntExtra(ORG_BROADCAST,0) == TALKING_TO_ISP)
                {
                    AnimateUserToISP(true);
                    ispIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_neutral));
                    serverIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_browser_neutral));
                }
                else if(intent.getIntExtra(ORG_BROADCAST,0) == BLOCKED)
                {
                    AnimateUserToISP(false);
                    ispIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_blocked));
                    serverIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_browser_neutral));
                }
                else if(intent.getIntExtra(ORG_BROADCAST,0) == OK)
                {
                    AnimateUserToISP(false);
                    ispIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_ok));
                    serverIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_browser_ok));
                }
                else if(intent.getIntExtra(ORG_BROADCAST,0) == NO_URLS)
                {
                    Reset();
                }
            }
        };
    }

    @Override
    public void onResume()
    {
        super.onResume();
        filter = new IntentFilter();
        filter.addAction(ORG_BROADCAST);
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
        View rootView = inflater.inflate(R.layout.fragment_progress, container, false);

        SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);

        bouncerUserORG = (ImageView) rootView.findViewById(R.id.bouncerUserORG);
        bouncerUserISP = (ImageView) rootView.findViewById(R.id.bouncerUserISP);
        //bouncerISPWebsite = (ImageView) rootView.findViewById(R.id.bouncerUserORG);
        ispIV = (ImageView) rootView.findViewById(R.id.ispIV);
        serverIV = (ImageView) rootView.findViewById(R.id.serverIV);

        //rootView.findViewById(R.id.blockedLabel).setRotation(90);

        Resources r = getResources();
        px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, r.getDisplayMetrics());

        return rootView;
    }

    private void AnimateUserToORG(boolean on)
    {
        if (on)
        {
            bouncerUserORG.setVisibility(View.VISIBLE);
            bouncerUserISP.setVisibility(View.VISIBLE);
            TranslateAnimation animation = new TranslateAnimation(0.0f, px * 2, 0.0f, 0.0f);          //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
            animation.setDuration(1000);  // animation duration
            animation.setRepeatCount(-1);  // animation repeat count
            animation.setRepeatMode(1);   // repeat animation (left to right, right to left )
            animation.setZAdjustment(-1);
            bouncerUserORG.startAnimation(animation);
        }
        else
        {
            bouncerUserORG.setVisibility(View.GONE);
            bouncerUserORG.setAnimation(null);
        }
    }

    private void AnimateUserToISP(boolean on)
    {
        if (on)
        {
            bouncerUserORG.setVisibility(View.VISIBLE);
            bouncerUserISP.setVisibility(View.VISIBLE);
            TranslateAnimation animation = new TranslateAnimation(0.0f, 0.0f, 0.0f, px * 4);          //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
            animation.setDuration(1000);  // animation duration
            animation.setRepeatCount(-1);  // animation repeat count
            animation.setRepeatMode(1);   // repeat animation (left to right, right to left )
            animation.setZAdjustment(-1);
            bouncerUserORG.startAnimation(animation);
        }
        else
        {
            bouncerUserORG.setVisibility(View.GONE);
            bouncerUserORG.setAnimation(null);
            bouncerUserISP.setVisibility(View.GONE);
            bouncerUserISP.setAnimation(null);
        }
    }

    private void Reset()
    {
        bouncerUserORG.setVisibility(View.GONE);
        bouncerUserORG.setAnimation(null);
        bouncerUserISP.setVisibility(View.GONE);
        bouncerUserISP.setAnimation(null);
        ispIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_neutral));
        serverIV.setImageDrawable(getResources().getDrawable(R.drawable.ic_browser_neutral));
    }

}
