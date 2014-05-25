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

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
import android.widget.EditText;
import android.widget.Toast;
import uk.bowdlerize.cache.LocalCache;
import uk.bowdlerize.fragments.CheckConfigFragment;
import uk.bowdlerize.fragments.ProgressFragment;
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
    private static int SETUP_INTENT = 2000;

    String SENDER_ID = "974943648388";
    static final String TAG = "MainActivity";
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
        getActionBar().setIcon(R.drawable.ic_ab_alt);

        settings = getGCMPreferences(this);

        Log.e("ProbeUUID",settings.getString(API.SETTINGS_UUID,"No key found"));


        Log.e("VERSION","DOESN'T HAVE GOOGLE PLAY");
        //Define that this is the version that DOESN'T use google play services
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("no_google_play_services",true);
        editor.commit();

        //Check if the user has agreed to the T&Cs / has a private key for themselves and the probe
        if(settings.getBoolean("agreed",false) && !settings.getString(API.SETTINGS_USER_PRIVATE_KEY,"").equals("") && !settings.getString(API.SETTINGS_PROBE_PRIVATE_KEY,"").equals(""))
        {
            onConfirmed();
        }
        else
        {
            Intent SetupIntent = new Intent(MainActivity.this, SetupActivity.class);
            startActivityForResult(SetupIntent, SETUP_INTENT);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == SETUP_INTENT)
        {
            if (resultCode == RESULT_OK)
            {
                onConfirmed();
            }
            else
            {
                finish();
            }
        }
    }

    private void onConfirmed()
    {
        configureTabs();
    }

    private void sendRegistrationIdToBackend()
    {
        new Thread()
        {
            public void run()
            {
                API api;
                try
                {
                    api = new API(MainActivity.this);
                    api.updateGCM(settings.getInt(API.SETTINGS_GCM_PREFERENCE,API.SETTINGS_GCM_FULL),settings.getInt("maxDelay",2));
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private static int getAppVersion(Context context)
    {
        return 1;
    }

    private SharedPreferences getGCMPreferences(Context context)
    {
        return getSharedPreferences(MainActivity.class.getSimpleName(),Context.MODE_PRIVATE);
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
    public boolean onOptionsItemSelected(MenuItem item)
    {
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

            case R.id.action_get_url:
            {
                Intent getURLIntent = new Intent(MainActivity.this, CensorCensusService.class);
                Bundle extras = new Bundle();
                extras.putBoolean(API.EXTRA_POLL,true);
                getURLIntent.putExtras(extras);
                startService(getURLIntent);
                return true;
            }

            case R.id.action_privatekey:
            {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setPositiveButton(R.string.action_user_key, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        //String probeKey = "-----BEGIN RSA PRIVATE KEY-----\n" + settings.getString(API.SETTINGS_USER_PRIVATE_KEY,"No key found") + "\n-----END RSA PRIVATE KEY-----";
                        String probeKey = settings.getString(API.SETTINGS_USER_PRIVATE_KEY,"No key found");

                        ClipData clip = ClipData.newPlainText("Censorship Monitoring Project Probe Private Key",probeKey);
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(MainActivity.this, "Key has been added to the clipboard.", Toast.LENGTH_LONG).show();
                    }
                });

                builder.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.cancel();
                    }
                });
                builder.setTitle(getString(R.string.privKeyDialogTitle));
                builder.setMessage(getString(R.string.privKeyDialogMessage));
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
                    getFragment = new ProgressFragment();
                }
                break;
            }

            return getFragment;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
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
                    return getString(R.string.title_section4).toUpperCase(l);
            }
            return null;
        }
    }
}
