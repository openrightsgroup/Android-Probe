package uk.bowdlerize.support;

import android.database.Cursor;

import uk.bowdlerize.cache.LocalCache;

/**
 * Created by Gareth on 25/05/2014.
 */
public class ResultMeta
{
    public String ISP = "Test";
    public String MD5  = "Test";
    public String URL  = "Test";
    public int result  = LocalCache.RESULT_BLOCKED;
    public String Date = "01/01/1970";

    public ResultMeta(Cursor cursor)
    {
        //{"id","isp","testTime","md5","url","result"};

        ISP = cursor.getString(1);
        Date = cursor.getString(2);
        MD5 = cursor.getString(3);
        URL = cursor.getString(4);
        result = cursor.getInt(5);
    }
}
