package uk.bowdlerize.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import uk.bowdlerize.MainActivity;
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
