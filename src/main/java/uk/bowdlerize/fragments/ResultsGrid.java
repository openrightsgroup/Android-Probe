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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import uk.bowdlerize.R;
import uk.bowdlerize.cache.LocalCache;
import uk.bowdlerize.support.ResultMeta;
import uk.bowdlerize.support.ResultsGridAdapter;

public class ResultsGrid extends Fragment
{
    public ResultsGrid()
    {
    }

    GridView gridView;
    Handler handler;
    ArrayList<ResultMeta> resultMetas;
    BroadcastReceiver receiver;
    IntentFilter filter;
    ResultsGridAdapter adapter;
    static public String INTENT_FILTER = "result_grid";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_results_grid, container, false);

        //SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);

        gridView = (GridView) rootView.findViewById(R.id.gridView);

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        handler = new Handler() {
            public void handleMessage(Message msg) {
                try
                {
                    gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                            Toast.makeText(getActivity(), "" + position, Toast.LENGTH_SHORT).show();
                        }
                    });
                    adapter = new ResultsGridAdapter(getActivity(),resultMetas);
                    gridView.setAdapter(adapter);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                try {
                    if (null != intent &&
                            intent.getIntExtra(ResultsGrid.INTENT_FILTER, -1) == LocalCache.RESULT_BLOCKED ||
                            intent.getIntExtra(ResultsGrid.INTENT_FILTER, -1) == LocalCache.RESULT_OK) {
                        try
                        {
                            if (null != adapter)
                                adapter.addResult(new ResultMeta(intent.getStringExtra("url"), intent.getStringExtra("hash"), intent.getStringExtra("date"), intent.getIntExtra(ResultsGrid.INTENT_FILTER, LocalCache.RESULT_OK)));
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    public void onResume()
    {
        super.onResume();

        new Thread(){
            public void run()
            {
                try
                {
                    LocalCache lc = new LocalCache(getActivity());
                    lc.open();
                    resultMetas = lc.getResults();

                    if(null != lc)
                        lc.close();
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }

                handler.sendEmptyMessage(1);
            }
        }.start();

        filter = new IntentFilter();
        filter.addAction(ProgressFragment.ORG_BROADCAST);
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
}
