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
package uk.bowdlerize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v13.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import uk.bowdlerize.cache.LocalCache;
import uk.bowdlerize.fragments.CheckConfigFragment;
import uk.bowdlerize.fragments.OrgFragment;
import uk.bowdlerize.fragments.StatsFragment;
import uk.bowdlerize.fragments.WirelessConfigFragment;
import uk.bowdlerize.service.CensorCensusService;
import static uk.bowdlerize.support.Hashes.MD5;

public class MainActivity extends Activity implements ActionBar.TabListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    String SENDER_ID = "974943648388";
    static final String TAG = "MainActivity";
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    SharedPreferences prefs;
    Context context;
    boolean mExplicitSignOut = false;
    String regid;
    Boolean mSignedIn = false;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    private SharedPreferences settings = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getActionBar().setTitle(getString(R.string.actionBarTitle));
        getActionBar().setSubtitle(getString(R.string.actionBarSubtitle));

        settings = getGCMPreferences(this);

        //Check if the user has agreed
        if(settings.getBoolean("agreed",false))
        {
            onConfirmed();
        }
        else
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.warningMessage).setTitle(R.string.warningTitle);
            builder.setPositiveButton(R.string.warningAgree, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id)
                {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("agreed", true);
                    editor.commit();
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this,getString(R.string.warningAgreed),Toast.LENGTH_SHORT).show();
                    configureTabs();
                }
            });
            builder.setNegativeButton(R.string.warningExit, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.dismiss();
                    finish();
                }
            });
            builder.show();
        }
    }

    private void onConfirmed()
    {
        if (checkPlayServices())
        {

            configureTabs();

            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);

            if (regid.isEmpty())
            {
                registerInBackground();
            }
            else
            {
                Log.i(TAG, "regid:"+regid);
                sendRegistrationIdToBackend();
            }
        }
        else
        {
            Log.i(TAG, "No valid Google Play Services APK found.");
            try
            {
                Toast.makeText(MainActivity.this,"Google Play Services is needed for this game",Toast.LENGTH_LONG).show();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            finish();
        }
    }

    private boolean checkPlayServices()
    {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS)
        {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
            {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
            else
            {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
    private String getRegistrationId(Context context)
    {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    private void registerInBackground()
    {
        new AsyncTask<Void, Void, String>()
        {
            @Override
            protected String doInBackground(Void... params)
            {
                String msg = "";
                try
                {
                    if (gcm == null)
                    {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.
                    // The request to your server should be authenticated if your app
                    // is using accounts.
                    sendRegistrationIdToBackend();

                    // For this demo: we don't need to send it because the device
                    // will send upstream messages to a server that echo back the
                    // message using the 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                }
                catch (IOException ex)
                {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }

                return msg;
            }

            @Override
            protected void onPostExecute(String msg)
            {
                //mDisplay.append(msg + "\n");
            }
        }.execute(null, null, null);
    }

    private void sendRegistrationIdToBackend()
    {
        new AsyncTask<Void, Void, Boolean>()
        {
            @Override
            protected Boolean doInBackground(Void... params)
            {
                DefaultHttpClient httpclient = new DefaultHttpClient();
                JSONObject json;
                HttpPost httpost = new HttpPost("https://bowdlerize.co.uk/api/1/updategcm.php");

                httpost.setHeader("Accept", "application/json");

                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new BasicNameValuePair("gcmid", regid));
                nvps.add(new BasicNameValuePair("deviceid", MD5(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID))));

                try
                {
                    httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

                    HttpResponse response = httpclient.execute(httpost);
                    String rawJSON = EntityUtils.toString(response.getEntity());
                    response.getEntity().consumeContent();
                    Log.e("rawJSON",rawJSON);
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
                //TODO if it fails we should reregister
            }
        }.execute(null, null, null);
    }

    private static int getAppVersion(Context context)
    {
        return 1;
    }

    private SharedPreferences getGCMPreferences(Context context)
    {
        return getSharedPreferences(MainActivity.class.getSimpleName(),Context.MODE_PRIVATE);
    }

    private void storeRegistrationId(Context context, String regId)
    {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        Log.i(TAG, "regId" + regId);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    private void configureTabs()
    {
        // Set up the action bar.
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++)
        {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId())
        {
            case R.id.action_add:
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                // Get the layout inflater
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.dialog_add_url, null);

                final EditText urlET = (EditText) dialogView.findViewById(R.id.urlET);

                builder.setView(dialogView)
                .setPositiveButton(R.string.action_add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Bundle extras = new Bundle();
                        Intent receiveURLIntent = new Intent(MainActivity.this, CensorCensusService.class);

                        extras.putString("url", urlET.getText().toString());
                        extras.putString("hash", MD5(urlET.getText().toString()));
                        extras.putInt("urgency", 0);
                        extras.putBoolean("local", true);

                        //Add our extra info
                        if(getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE).getBoolean("sendISPMeta",true))
                        {
                            WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                            TelephonyManager telephonyManager =((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE));

                            if(wifiMgr.isWifiEnabled() && null != wifiInfo.getSSID())
                            {
                                LocalCache lc = null;
                                Pair<Boolean,String> seenBefore = null;
                                try
                                {
                                    lc = new LocalCache(MainActivity.this);
                                    lc.open();
                                    seenBefore = lc.findSSID(wifiInfo.getSSID().replaceAll("\"",""));
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }

                                if(null != lc)
                                    lc.close();

                                if(seenBefore.first)
                                {
                                    extras.putString("isp",seenBefore.second);
                                }
                                else
                                {
                                    extras.putString("isp","unknown");
                                }

                                try
                                {
                                    extras.putString("sim",telephonyManager.getSimOperatorName());
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                            else
                            {
                                try
                                {
                                    extras.putString("isp",telephonyManager.getNetworkOperatorName());
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }

                                try
                                {
                                    extras.putString("sim",telephonyManager.getSimOperatorName());
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        }

                        receiveURLIntent.putExtras(extras);
                        startService(receiveURLIntent);
                        dialog.cancel();
                    }
                })
                .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                builder.show();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        checkPlayServices();
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            Fragment getFragment = null;

            switch(position)
            {
                case 0:
                {
                    getFragment = new WirelessConfigFragment();
                }
                break;

                case 1:
                {
                    getFragment = new CheckConfigFragment();
                }
                break;

                case 2:
                {
                    getFragment = new OrgFragment();
                }
                break;

                case 3:
                {
                    getFragment = new StatsFragment();
                }
                break;
            }

            return getFragment;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
                case 3:
                    return getString(R.string.title_section4).toUpperCase(l);
            }
            return null;
        }
    }
}
