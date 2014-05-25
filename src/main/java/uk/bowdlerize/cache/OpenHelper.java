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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class OpenHelper extends SQLiteOpenHelper
{
    private static final int DATABASE_VERSION = 6;
    public static final String DATABASE_NAME = "bowdlerizeCache";


    OpenHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        try
        {
            db.execSQL("CREATE  TABLE \"probeResults\" (\"id\" INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL  UNIQUE , \"isp\" INTEGER, \"testTime\" DATETIME DEFAULT CURRENT_TIMESTAMP, \"md5\" TEXT, \"url\" TEXT, \"result\" INTEGER DEFAULT 0)");
        }
        catch(SQLiteException s)
        {
            s.printStackTrace();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            db.execSQL("CREATE  TABLE \"isp\" (\"ispID\" INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL , \"ispName\" TEXT UNIQUE check(typeof(\"ispName\") = 'text') )");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        Log.v("onUpgrade", "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        try
        {
            db.execSQL("DROP TABLE IF EXISTS wifiNetworks");
            db.execSQL("DROP TABLE IF EXISTS isp");
            db.execSQL("DROP TABLE IF EXISTS probeResults");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        onCreate(db);
    }
}
