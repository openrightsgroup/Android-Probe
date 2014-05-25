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
package uk.bowdlerize.cache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;

import uk.bowdlerize.support.ResultMeta;


public class LocalCache
{
    private SQLiteDatabase database;
    private OpenHelper dbHelper;

    private String[] wifiColumns = {"SSID","ISP"};
    private String[] resultColumns = {"id","isp","testTime","md5","url","result"};
    private String[] ispColumns = {"ispID","ispName"};

    static public int RESULT_BLOCKED = 0;
    static public int RESULT_OK = 1;
    static public int RESULT_ERROR = 2;

    public LocalCache(Context context)
    {
        dbHelper = new OpenHelper(context);
    }

    public void open() throws SQLException
    {
        if(null != dbHelper)
            database = dbHelper.getWritableDatabase();
    }

    public void close()
    {
        if(null != dbHelper)
            dbHelper.close();
    }

    public boolean addResult(String URL, String MD5, String ISP, int Result)
    {
        boolean success = false;

        try
        {
            ContentValues values = new ContentValues(4);
            values.put("isp", addISP(ISP));
            values.put("md5", MD5);
            values.put("url", URL);
            values.put("result", Result);

            database.beginTransaction();
            database.insert("probeResults", null, values);
            database.setTransactionSuccessful();
            success = true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            database.endTransaction();
        }

        Log.e("addResult",Boolean.toString(success));

        return success;
    }

    public long addISP(String ISP)
    {
        long ispID = -1;
        try
        {
            ContentValues values = new ContentValues(1);
            values.put("ispName", ISP);
            database.beginTransaction();
            ispID = database.insertWithOnConflict ("isp", null, values, SQLiteDatabase.CONFLICT_IGNORE);

            if(ispID != -1)
                database.setTransactionSuccessful();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            database.endTransaction();
        }

        try {
            if (ispID == -1)
                ispID = getISPID(ISP);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            ispID = -1;
        }

        Log.v("addISP", "Added " + ISP + " and got ID " + Long.toString(ispID));
        return ispID;
    }

    public int getISPID(String ISP)
    {
        Cursor cursor = null;

        try
        {
            cursor = database.query("isp", ispColumns,  "ispName=?", new String[]{ISP}, null, null, null);

            if(cursor.moveToFirst())
            {
                //Log.e("findSSID", cursor.getString(1));
                return cursor.getInt(0);
            }
            else
            {
                //Log.e("findSSID", "Nope");
                return -1;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return -1;
        }
        finally
        {
            if(null != cursor && !cursor.isClosed())
            cursor.close();
        }
    }

    public ArrayList<ResultMeta> getResults()
    {
        Cursor dbResults = null;
        ArrayList<ResultMeta> resultMetas = null;

        try
        {
            //dbResults = database.query("probeResults", resultColumns,  null, null, null, null, null);

            //"id","isp","testTime","md5","url","result"
            dbResults = database.rawQuery("select id,ispName,testTime,md5,url,result from probeResults inner join isp on isp = ispID",null,null);

            if(dbResults.getCount() > 0)
            {
                resultMetas = new ArrayList<ResultMeta>();

                while (dbResults.moveToNext())
                {
                    resultMetas.add(new ResultMeta(dbResults));
                }

                return resultMetas;
            }
            else
            {
                return null;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    @Deprecated
    public boolean addSSID(String SSID, String ISP)
    {
        boolean success = false;
        //Log.e("addSSID", SSID + " / " + ISP);
        try
        {
            database.beginTransaction();
            ContentValues values = new ContentValues(2);
            values.put("SSID", SSID);
            values.put("ISP", ISP);

            database.insert("wifiNetworks", null, values);
            database.setTransactionSuccessful();
            success = true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            database.endTransaction();
        }

        //Log.e("addSSID",Boolean.toString(success));

        return success;
    }

    @Deprecated
    public Pair<Boolean,String> findSSID(String SSID)
    {
        try
        {
            //Log.e("findSSID", SSID);
            Cursor cursor = database.query("wifiNetworks", wifiColumns,  "SSID=?", new String[]{SSID}, null, null, null);

            if(cursor.moveToFirst())
            {
                //Log.e("findSSID", cursor.getString(1));
                return new Pair(true,cursor.getString(1));
            }
            else
            {
                //Log.e("findSSID", "Nope");
                return new Pair(false,"");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new Pair(false,"");
        }
    }
}
