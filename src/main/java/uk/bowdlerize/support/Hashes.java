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
package uk.bowdlerize.support;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hashes
{
    public static String MD5(String target)
    {
        MessageDigest mdEnc = null;
        try
        {
            mdEnc = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e)
        {
            //Log.e("Exception","Exception while encrypting to md5");
            e.printStackTrace();
        }

        mdEnc.update(target.getBytes(), 0, target.length());
        String md5 = new BigInteger(1, mdEnc.digest()).toString(16) ;
        return md5;
    }
}
