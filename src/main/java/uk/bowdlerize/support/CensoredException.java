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