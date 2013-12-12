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
package uk.bowdlerize.gcm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;
import uk.bowdlerize.API;
import uk.bowdlerize.MainActivity;
import uk.bowdlerize.cache.LocalCache;
import uk.bowdlerize.service.CensorCensusService;

public class CCBroadcastReceiver extends BroadcastReceiver
{
    static final String TAG = "CCBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        //This should never get called
    }
}
