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

import org.apache.http.HttpResponse;

import java.io.IOException;

public class CensoredException extends IOException
{
    String isp = "";
    int confidence = 0;
    int returnCode = 0;
    HttpResponse httpResponse = null;

    public CensoredException(String message)
    {
        super(message);
    }

    public CensoredException(String message, String ISP, int Confidence)
    {
        super(message);
        isp = ISP;
        confidence = Confidence;
    }

    public CensoredException(String message, String ISP, int Confidence,HttpResponse httpResp)
    {
        super(message);
        isp = ISP;
        confidence = Confidence;
        httpResponse = httpResp;
    }
}