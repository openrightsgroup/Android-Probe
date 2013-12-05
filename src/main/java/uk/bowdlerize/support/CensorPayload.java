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

public class CensorPayload
{
    public static int CONFIDENCE_BLOCKED_STRING = 100;
    public static int CONFIDENCE_FORBIDDEN_STRING = 100;
    public static int CONFIDENCE_SUSPICIOUS_TIMEOUT = 90;
    public static int CONFIDENCE_SUSPICIOUS_NO_RESPONSE = 80;
    public static int CONFIDENCE_SUSPICIOUS_403 = 50;
    public static int CONFIDENCE_NONE = 0;

    private boolean isCensored = false;
    private int confidence = 0;
    private String confidenceReason = "None";
    private int returnCode = 200;

    public CensorPayload(boolean _censored, int _confidence)
    {
        isCensored = _censored;
        confidence = _confidence;
    }

    public CensorPayload(boolean _censored, int _confidence, String _reason,int response)
    {
        isCensored = _censored;
        confidence = _confidence;
        confidenceReason = _reason;
        returnCode = response;
    }

    public boolean isCensored()
    {
        return isCensored;
    }

    public boolean wasCensored()
    {
        return isCensored;
    }

    public String getConfidenceReason()
    {
        return confidenceReason;
    }

    public int getReturnCode()
    {
        return returnCode;
    }
}
