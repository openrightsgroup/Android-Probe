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

import android.util.Log;

import org.apache.http.Header;

public class CensorPayload
{
    public static int CONFIDENCE_BLOCKED_STRING = 100;
    public static int CONFIDENCE_FORBIDDEN_STRING = 100;
    public static int CONFIDENCE_SUSPICIOUS_TIMEOUT = 90;
    public static int CONFIDENCE_SUSPICIOUS_NO_RESPONSE = 80;
    public static int CONFIDENCE_SUSPICIOUS_403 = 50;
    public static int CONFIDENCE_NONE = 0;

    private boolean isCensored = false;
    private boolean wasError = false;
    private int confidence = 0;
    private String confidenceReason = "None";
    private int returnCode = 200;
    public String URL = "";
    public String MD5 = "";
    int bytes = 0;


    public CensorPayload(String url)
    {
        URL = url;
    }

    public CensorPayload(boolean _censored, int _confidence)
    {
        isCensored = _censored;
        confidence = _confidence;
    }

    public CensorPayload(boolean _censored, int _confidence, String _reason, int response)
    {
        isCensored = _censored;
        confidence = _confidence;
        confidenceReason = _reason;
        returnCode = response;
    }


    public void consumeError(String reason)
    {
        isCensored = false;
        wasError = true;
        returnCode = -1;
        confidenceReason = reason;
    }

    public boolean wasError() { return wasError; }


    public void consumeCensoredException(CensoredException exception)
    {
        returnCode = exception.returnCode;
        isCensored = true;
        confidenceReason = exception.getMessage();
        confidence = exception.confidence;

        /*Log.e("Size", exception.httpResponse.toString());

        for(Header hdr : exception.httpResponse.getAllHeaders())
        {
            Log.e("intercepted header", hdr.getName().toString() + " / " + hdr.getValue().toString());
        }*/
    }

    public void setCensored(Boolean censor_state)
    {
        isCensored = censor_state;
    }

    public void setConfidence(int conf)
    {
        confidence = conf;
    }

    public void setReturnCode(int code){ returnCode = code; }

    public boolean isCensored() { return isCensored; }

    public boolean wasCensored() { return isCensored; }

    public int getConfidence() { return confidence; }

    public String getConfidenceReason()
    {
        return confidenceReason;
    }

    public int getReturnCode()
    {
        return returnCode;
    }
}
