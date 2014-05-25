package uk.bowdlerize.support;

import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

import uk.bowdlerize.R;
import uk.bowdlerize.cache.LocalCache;

/**
 * Created by Gareth on 25/05/2014.
 */
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ResultMeta rm = resultMetas.get(position);

        if (convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.grid_results, null);
        }


        ((TextView) convertView.findViewById(R.id.urlTV)).setText(rm.MD5);
        ((TextView) convertView.findViewById(R.id.ispTV)).setText(rm.ISP);
        ((TextView) convertView.findViewById(R.id.dateTV)).setText(rm.Date);

        ImageView ooni = ((ImageView) convertView.findViewById(R.id.ooniImage));

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
