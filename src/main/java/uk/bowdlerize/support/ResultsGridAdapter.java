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

package uk.bowdlerize.support;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import uk.bowdlerize.R;
import uk.bowdlerize.cache.LocalCache;

public class ResultsGridAdapter extends BaseAdapter
{
    private Context mContext;
    private ArrayList<ResultMeta> resultMetas;

    public ResultsGridAdapter(Context context, ArrayList<ResultMeta> results)
    {
        mContext = context;
        resultMetas = results;
    }

    @Override
    public int getCount()
    {
        return resultMetas.size();
    }

    @Override
    public Object getItem(int position)
    {
        return resultMetas.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    public void addResult(ResultMeta rm)
    {
       resultMetas.add(0, rm);
       notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ResultMeta rm = resultMetas.get(position);

        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.grid_results, null);
        }


        ((TextView) convertView.findViewById(R.id.urlTV)).setText(rm.URL.replace("http://","").replace("https://",""));
        ((TextView) convertView.findViewById(R.id.ispTV)).setText(rm.ISP);
        ((TextView) convertView.findViewById(R.id.dateTV)).setText(rm.Date);

        ImageView ooni = ((ImageView) convertView.findViewById(R.id.ooniImage));
        ooni.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if(rm.result == LocalCache.RESULT_OK)
        {
            ooni.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_ooni_large));
        }
        else
        {
            ooni.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_ooni_large_censored));
        }

        return convertView;
    }
}
