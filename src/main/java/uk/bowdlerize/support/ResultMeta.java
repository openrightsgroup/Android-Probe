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
