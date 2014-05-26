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
package uk.bowdlerize;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import uk.bowdlerize.fragments.CreateUserFragment;
import uk.bowdlerize.fragments.OrgFragment;


public class SetupActivity extends Activity implements ActionBar.TabListener, CreateUserFragment.Callbacks, OrgFragment.Callbacks
{
    SectionsPagerAdapter mSectionsPagerAdapter;
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    String SENDER_ID = "974943648388";
    static final String TAG = "MainActivity";
    AtomicInteger msgId = new AtomicInteger();
    SharedPreferences prefs;
    Context context;
    boolean mExplicitSignOut = false;
    String regid;
    Boolean mSignedIn = false;
    ViewPager mViewPager;
    private SharedPreferences settings = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        getActionBar().setTitle(getString(R.string.actionBarTitle));
        getActionBar().setSubtitle(getString(R.string.actionBarSubtitle));
        getActionBar().setIcon(R.drawable.ic_ab_alt);

        settings = getGCMPreferences(this);

        //Check if the user has agreed
        if(settings.getBoolean("agreed",false))
        {
            configureTabs();
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
                    Toast.makeText(SetupActivity.this, getString(R.string.warningAgreed), Toast.LENGTH_SHORT).show();
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

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction)
    {
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

    private SharedPreferences getGCMPreferences(Context context)
    {
        return getSharedPreferences(MainActivity.class.getSimpleName(),Context.MODE_PRIVATE);
    }

    @Override
    public void updateStatus(int type)
    {
        switch(type)
        {
            case CreateUserFragment.USER_NOTHING:
            {
                ((ProgressBar) findViewById(R.id.statusProgressBar)).setProgress(0);
                ((TextView) findViewById(R.id.progressStatusTV)).setText("Waiting on user details");
            }
            break;

            case CreateUserFragment.USER_PENDING:
            {
                ((ProgressBar) findViewById(R.id.statusProgressBar)).setProgress(1);
                ((TextView) findViewById(R.id.progressStatusTV)).setText("Waiting on account activation");
            }
            break;

            case CreateUserFragment.USER_COMPLETE:
            {
                ((ProgressBar) findViewById(R.id.statusProgressBar)).setProgress(2);
                ((TextView) findViewById(R.id.progressStatusTV)).setText("Account ready to register a probe");
                mViewPager.setCurrentItem(1);
            }
            break;

            case OrgFragment.PROBE_PENDING:
            {
                ((ProgressBar) findViewById(R.id.statusProgressBar)).setProgress(3);
                ((TextView) findViewById(R.id.progressStatusTV)).setText("Probe prepared");
                mViewPager.setCurrentItem(1);
            }
            break;

            case OrgFragment.PROBE_COMPLETE:
            {
                ((ProgressBar) findViewById(R.id.statusProgressBar)).setProgress(4);
                ((TextView) findViewById(R.id.progressStatusTV)).setText("Probe registration complete!");

                Intent in = new Intent();
                setResult(RESULT_OK,in);
                finish();
            }
            break;
        }

    }

    @Override
    public void onBackPressed()
    {
        //Return back to the launcher
        Intent in = new Intent();
        setResult(RESULT_CANCELED,in);
        finish();
    }

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
                    getFragment = new CreateUserFragment();
                }
                break;

                case 1:
                {
                    getFragment = new OrgFragment();
                }
                break;
            }

            return getFragment;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_user_setup).toUpperCase(l);
                case 1:
                    return getString(R.string.title_probe_setup).toUpperCase(l);
            }
            return null;
        }
    }
}
