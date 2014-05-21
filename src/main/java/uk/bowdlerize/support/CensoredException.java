package uk.bowdlerize.support;

import java.io.IOException;

public class CensoredException extends IOException
{
    String isp = "";
    int confidence = 0;
    int returnCode = 0;

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
}