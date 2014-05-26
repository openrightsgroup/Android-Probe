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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;
import java.util.ArrayList;
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

                    gridView.setAdapter(new ResultsGridAdapter(getActivity(),resultMetas));
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

        ((Thread) new Thread(){
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
        }).start();
    }
}
