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
package uk.bowdlerize;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import uk.bowdlerize.cache.LocalCache;
import uk.bowdlerize.support.CensorPayload;
import uk.bowdlerize.support.CensoredException;
import uk.bowdlerize.support.ISPMeta;

public class API
{
    private Context mContext;
    public API(){}
    public static String USER_STATUS_PENDING = "pending";
    public static String USER_STATUS_OK = "ok";
    public static String USER_STATUS_FAILED = "failed";

    public static final String SETTINGS_EMAIL_ADDRESS = "emailAddress";
    public static final String SETTINGS_USER_PRIVATE_KEY = "userPrivKey";
    public static final String SETTINGS_PROBE_PRIVATE_KEY = "probePrivKey";
    public static final String SETTINGS_GCM_PREFERENCE = "gcm_pref";
    public static final String SETTINGS_UUID = "probe_uuid";
    public static final String SETTINGS_FREQUENCY = "frequency";
    public static final String SETTINGS_FREQUENCY_SEEK = "frequency_seek";

    public static final int FILTERING_NONE = 0;
    public static final int FILTERING_MEDIUM = 1;
    public static final int FILTERING_STRICT = 2;

    public static final String EXTRA_POLL = "poll_for_url";
    public static final String EXTRA_GCM_TICKLE = "gcm_tickle";

    public static final String PROPERTY_REG_ID = "registration_id";

    public static final int SETTINGS_GCM_FULL = 0;
    public static final int SETTINGS_GCM_PARTIAL = 1;
    public static final int SETTINGS_GCM_DISABLED = 2;

    private final static char[] hexArray = "0123456789abcdef".toCharArray();

    private SharedPreferences settings = null;

    SimpleDateFormat sDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public API(Context context)
    {
        this.mContext = context;
        settings = mContext.getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
        sDF.setTimeZone(TimeZone.getTimeZone("utc"));
    }

    public String registerUser(String emailAddress, String password)
    {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        JSONObject json;
        HttpPost httpost = new HttpPost("https://api.blocked.org.uk/1.2/register/user");

        httpost.setHeader("Accept", "application/json");

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("email", emailAddress));
        nvps.add(new BasicNameValuePair("password", password));

        try
        {
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse response = httpclient.execute(httpost);
            String rawJSON = EntityUtils.toString(response.getEntity());
            response.getEntity().consumeContent();
            Log.e("rawJSON",rawJSON);
            json = new JSONObject(rawJSON);

            if(json.getBoolean("success"))
            {
                return json.getString("secret");
            }
            else
            {
                return null;
            }

        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public String getUserStatus() throws NoSuchPaddingException, UnsupportedEncodingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException, SignatureException
    {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        JSONObject json;
        /*HttpPost httpost = new HttpPost("https://api.blocked.org.uk/1.2/status/user");
        httpost.setHeader("Accept", "application/json");*/

        HttpGet httpGet = new HttpGet("https://api.blocked.org.uk/1.2/status/user");
        httpGet.setHeader("Accept", "application/json");

        String emailAddress = settings.getString(SETTINGS_EMAIL_ADDRESS,"");

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("email", emailAddress));
        nvps.add(new BasicNameValuePair("date", sDF.format(new Date())));
        nvps.add(new BasicNameValuePair("signature", SignHeaders(settings.getString(SETTINGS_USER_PRIVATE_KEY,""),nvps)));

        try
        {
            httpGet.setURI(new URI(httpGet.getURI() + "?" + URLEncodedUtils.format(nvps, "utf-8")));
            HttpResponse response = httpclient.execute(httpGet);
            String rawJSON = EntityUtils.toString(response.getEntity());
            response.getEntity().consumeContent();
            Log.e("rawJSON",rawJSON);
            /*String sb = rawJSON;
            if (sb.length() > 4000) {
                Log.v("rawJSON", "sb.length = " + sb.length());
                int chunkCount = sb.length() / 4000;     // integer division
                for (int i = 0; i <= chunkCount; i++)
                {
                    int max = 4000 * (i + 1);
                    if (max >= sb.length()) {
                        Log.v("rawJSON", "chunk " + i + " of " + chunkCount + ":" + sb.substring(4000 * i));
                    } else {
                        Log.v("rawJSON", "chunk " + i + " of " + chunkCount + ":" + sb.substring(4000 * i, max));
                    }
                }
            }*/

            json = new JSONObject(rawJSON);

            if(json.getBoolean("success"))
            {
                return json.getString("status");
            }
            else
            {
                return null;
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public String getURLBasic() throws NoSuchPaddingException, UnsupportedEncodingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException, SignatureException
    {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        JSONObject json;

        //TelephonyManager telephonyManager =((TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE));
        //String mobileNet = telephonyManager.getNetworkOperatorName();

        String uuid = settings.getString(SETTINGS_UUID,"");
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("probe_uuid", uuid));
        nvps.add(new BasicNameValuePair("signature", SignHeaders(settings.getString(SETTINGS_PROBE_PRIVATE_KEY,""),nvps)));

        ISPMeta ispMeta = getISPMeta();

        nvps.add(new BasicNameValuePair("network_name", ispMeta.ispName));
        nvps.add(new BasicNameValuePair("basic", "true"));

        HttpGet httpGet = new HttpGet("https://api.blocked.org.uk/1.2/request/httpt");

        httpGet.setHeader("Accept", "application/json");

        try
        {
            //httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            httpGet.setURI(new URI(httpGet.getURI() + "?" + URLEncodedUtils.format(nvps, "utf-8")));
            HttpResponse response = httpclient.execute(httpGet);
            String rawJSON = EntityUtils.toString(response.getEntity());
            response.getEntity().consumeContent();
            Log.e("rawJSON",rawJSON);
            json = new JSONObject(rawJSON);

            if(json.getBoolean("success"))
            {
                return json.getString("url");
            }
            else
            {
                return null;
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public ISPMeta getISPMeta()
    {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        JSONObject json;

        try
        {
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("date", sDF.format(new Date())));
            nvps.add(new BasicNameValuePair("signature", SignHeaders(settings.getString(SETTINGS_PROBE_PRIVATE_KEY,""),nvps)));
            nvps.add(new BasicNameValuePair("probe_uuid", settings.getString(SETTINGS_UUID,"")));

            HttpGet httpGet = new HttpGet("https://api.blocked.org.uk/1.2/status/ip");
            httpGet.setHeader("Accept", "application/json");

            httpGet.setURI(new URI(httpGet.getURI() + "?" + URLEncodedUtils.format(nvps, "utf-8")));
            HttpResponse response = httpclient.execute(httpGet);
            String rawJSON = EntityUtils.toString(response.getEntity());
            response.getEntity().consumeContent();
            Log.e("IP Raw JSON",rawJSON);
            json = new JSONObject(rawJSON);

            if(json.getBoolean("success"))
            {
                return new ISPMeta(json.getString("ip"),json.getString("isp"));
            }
            else
            {
                return null;
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public boolean submitURL(String url) throws NoSuchPaddingException, UnsupportedEncodingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException, SignatureException
    {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        JSONObject json;
        HttpPost httpost = new HttpPost("https://api.blocked.org.uk/1.2/submit/url");

        httpost.setHeader("Accept", "application/json");

        String emailAddress = settings.getString(SETTINGS_EMAIL_ADDRESS,"");

        if(!url.startsWith("http"))
            url = "http://" + url;

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("url", url));
        nvps.add(new BasicNameValuePair("signature", SignHeaders(settings.getString(SETTINGS_USER_PRIVATE_KEY,""),nvps)));

        nvps.add(new BasicNameValuePair("email", emailAddress));

        try
        {
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse response = httpclient.execute(httpost);
            String rawJSON = EntityUtils.toString(response.getEntity());
            response.getEntity().consumeContent();
            Log.e("submit URL rawJSON",rawJSON);
            json = new JSONObject(rawJSON);

            return json.getBoolean("success");
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public String prepareProbe() throws NoSuchPaddingException, UnsupportedEncodingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException, SignatureException
    {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        JSONObject json;
        HttpPost httpost = new HttpPost("https://api.blocked.org.uk/1.2/prepare/probe");

        httpost.setHeader("Accept", "application/json");

        String emailAddress = settings.getString(SETTINGS_EMAIL_ADDRESS,"");

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("email", emailAddress));
        nvps.add(new BasicNameValuePair("date", sDF.format(new Date())));
        nvps.add(new BasicNameValuePair("signature", SignHeaders(settings.getString(SETTINGS_USER_PRIVATE_KEY,""),nvps)));

        try
        {
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse response = httpclient.execute(httpost);
            String rawJSON = EntityUtils.toString(response.getEntity());
            response.getEntity().consumeContent();
            Log.e("rawJSON",rawJSON);
            json = new JSONObject(rawJSON);

            if(json.getBoolean("success"))
            {
                return json.getString("probe_hmac");
            }
            else
            {
                return null;
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public String registerProbe(String probeSeed, String probe_uuid, String country) throws NoSuchPaddingException, UnsupportedEncodingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException, SignatureException
    {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        JSONObject json;
        HttpPost httpost = new HttpPost("https://api.blocked.org.uk/1.2/register/probe");

        httpost.setHeader("Accept", "application/json");

        String emailAddress = settings.getString(SETTINGS_EMAIL_ADDRESS,"");

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("probe_uuid", probe_uuid));
        nvps.add(new BasicNameValuePair("signature", SignHeaders(settings.getString(SETTINGS_USER_PRIVATE_KEY,""),nvps)));

        nvps.add(new BasicNameValuePair("email", emailAddress));
        nvps.add(new BasicNameValuePair("probe_seed", probeSeed));
        nvps.add(new BasicNameValuePair("type", "android"));
        nvps.add(new BasicNameValuePair("cc", country));
        //nvps.add(new BasicNameValuePair("signature", SignHeaders(probe_uuid)));



        try
        {
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse response = httpclient.execute(httpost);
            String rawJSON = EntityUtils.toString(response.getEntity());
            response.getEntity().consumeContent();
            Log.e("rawJSON",rawJSON);
            json = new JSONObject(rawJSON);

            if(json.getBoolean("success"))
            {
                return json.getString("secret");
            }
            else
            {
                return null;
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public void notifyBackEnd(CensorPayload censorPayload)
    {
        if(null == censorPayload || null == censorPayload.URL || censorPayload.URL.isEmpty() ||  censorPayload.URL.equals(""))
            return;

        DefaultHttpClient httpclient = new DefaultHttpClient();
        JSONObject json;
        ISPMeta ispMeta = getISPMeta();
        HttpPost httpost = new HttpPost("https://api.blocked.org.uk/1.2/response/httpt");
        LocalCache lc;
        int resultForDB = -1;

        if(!censorPayload.URL.startsWith("http"))
            censorPayload.URL = "http://" + censorPayload.URL;

        httpost.setHeader("Accept", "application/json");

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("probe_uuid", settings.getString(API.SETTINGS_UUID, "")));
        nvps.add(new BasicNameValuePair("url", censorPayload.URL));
        if((censorPayload.isCensored()))
        {
            nvps.add(new BasicNameValuePair("status", "blocked"));
            Log.e("wasError","Blocked");
            resultForDB = LocalCache.RESULT_BLOCKED;
        }
        else
        {
            if(censorPayload.wasError())
            {
                nvps.add(new BasicNameValuePair("status", "error"));
                Log.e("wasError","True");
                resultForDB = LocalCache.RESULT_ERROR;
            }
            else
            {
                nvps.add(new BasicNameValuePair("status", "ok"));
                Log.e("wasError","False");
                resultForDB = LocalCache.RESULT_OK;
            }
        }

        SimpleDateFormat sDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sDF.setTimeZone(TimeZone.getTimeZone("utc"));
        nvps.add(new BasicNameValuePair("date", sDF.format(new Date())));
        nvps.add(new BasicNameValuePair("config", "2014052101"));
        try
        {
            nvps.add(new BasicNameValuePair("signature", SignHeaders(settings.getString(API.SETTINGS_PROBE_PRIVATE_KEY, ""), nvps)));
        }
        catch (Exception e)
        {
            nvps.add(new BasicNameValuePair("signature",""));
        }

        try
        {
            nvps.add(new BasicNameValuePair("ip_network",ispMeta.ipAddress));
        }
        catch (Exception e)
        {
            nvps.add(new BasicNameValuePair("ip_network","0.0.0.0"));
        }

        try
        {
            nvps.add(new BasicNameValuePair("network_name", ispMeta.ispName));
        }
        catch (Exception e)
        {
            nvps.add(new BasicNameValuePair("network_name", "ether"));
        }

        nvps.add(new BasicNameValuePair("http_status", Integer.toString(censorPayload.getReturnCode())));

        nvps.add(new BasicNameValuePair("confidence", Integer.toString(censorPayload.getConfidence())));

        try
        {
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

            HttpResponse response = httpclient.execute(httpost);
            String rawJSON = EntityUtils.toString(response.getEntity());
            response.getEntity().consumeContent();
            Log.e("API Raw JSON",rawJSON);
            json = new JSONObject(rawJSON);

            //TODO In future versions we'll check for success and store it for later if it failed
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            lc = new LocalCache(mContext);
            lc.open();
            lc.addResult(censorPayload.URL,censorPayload.MD5,ispMeta.ispName,resultForDB);
            lc.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public Boolean updateGCM(int type, int frequency) throws NoSuchPaddingException, UnsupportedEncodingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException, SignatureException
    {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        JSONObject json;
        HttpPost httpost = new HttpPost("https://api.blocked.org.uk/1.2/update/gcm");

        httpost.setHeader("Accept", "application/json");

        String emailAddress = settings.getString(SETTINGS_EMAIL_ADDRESS,"");

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("gcm_id", settings.getString(API.PROPERTY_REG_ID,"")));
        nvps.add(new BasicNameValuePair("signature", SignHeaders(settings.getString(SETTINGS_PROBE_PRIVATE_KEY,""),nvps)));

        nvps.add(new BasicNameValuePair("frequency", Integer.toString(frequency)));
        nvps.add(new BasicNameValuePair("probe_uuid", settings.getString(API.SETTINGS_UUID,"")));
        nvps.add(new BasicNameValuePair("gcm_type", Integer.toString(type)));
        //nvps.add(new BasicNameValuePair("signature", SignHeaders(settings.getString(API.PROPERTY_REG_ID,""),false)));

        Log.e("GCM",settings.getString(API.PROPERTY_REG_ID,""));
        try
        {
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse response = httpclient.execute(httpost);
            String rawJSON = EntityUtils.toString(response.getEntity());
            response.getEntity().consumeContent();
            Log.e("update gcm json",rawJSON);
            json = new JSONObject(rawJSON);

            try
            {
                return json.getBoolean("success");
            }
            catch (Exception e)
            {
                return false;
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    private static String createSignatureHash(String value, String key) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException
    {
        String type = "HmacSHA512";
        SecretKeySpec secret = new SecretKeySpec(key.getBytes(), type);
        Mac mac = Mac.getInstance(type);
        mac.init(secret);
        byte[] bytes = mac.doFinal(value.getBytes());
        return bytesToHex(bytes);
    }

    public static String SignHeaders(String secret, List <NameValuePair> data) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String SignData = "";
        Iterator<NameValuePair> iterator = data.iterator();
        Log.e("NameValuePair", "Secret : " + secret);
        while (iterator.hasNext())
        {
            NameValuePair nameValuePair = iterator.next();

            Log.e("NameValuePair", nameValuePair.getName() + " : " + nameValuePair.getValue());

            SignData += nameValuePair.getValue();

            if(iterator.hasNext())
                SignData += ":";
        }

        return createSignatureHash(SignData, secret);
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public int ascertainFilteringLevel()
    {
        String checkURL = "http://www.reddit.com";
        int returnInt = FILTERING_STRICT;

        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(checkURL);
        httpGet.setHeader("User-Agent", "OONI Android Probe");

        try
        {
            client.addResponseInterceptor(new HttpResponseInterceptor()
            {
                @Override
                public void process(HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException
                {
                    if (httpResponse.getStatusLine().getStatusCode() == 302 || httpResponse.getStatusLine().getStatusCode() == 301)
                    {
                        for (Header hdr : httpResponse.getAllHeaders())
                        {
                            if (hdr.getName().equals("Location"))
                            {
                                checkHeader(hdr);
                            }
                        }
                    }
                }
            });
        }
        catch (Exception e) { e.printStackTrace(); return -1; }

        try { client.execute(httpGet); }
        catch (CensoredException ce) { return returnInt; }
        catch (Exception e) { e.printStackTrace(); }

        returnInt = FILTERING_MEDIUM;
        checkURL = "http://www.reddit.com/r/nsfw/";
        httpGet = new HttpGet(checkURL);

        try { client.execute(httpGet); }
        catch (CensoredException ce) { return returnInt; }
        catch (Exception e) { e.printStackTrace(); }

        return FILTERING_NONE;
    }

    public int checkHeader(Header header) throws CensoredException
    {
        if (header.getName().equals("Location"))
        {
            if (header.getValue().equals("http://ee-outage.s3.amazonaws.com/content-blocked/content-blocked-v1.html") ||
                    header.getValue().contains("http://ee-outage.s3.amazonaws.com")) {
                Log.e("Blocked", "Blocked by EE");
                throw new CensoredException("Blocked by EE", "EE", 100);
            } else if (header.getValue().contains("http://www.t-mobile.co.uk/service/wnw-mig/entry/") ||
                    header.getValue().contains("http://tmobile.ee.co.uk/common/system_error_pages/outage_wnw.html")) {
                Log.e("Blocked", "Blocked by TMobile");
                throw new CensoredException("Blocked by TMobile", "TMobile", 100);
            } else if (header.getValue().contains("http://online.vodafone.co.uk/dispatch/Portal/ContentControlServlet?type=restricted")) {
                Log.e("Blocked", "Blocked by Vodafone");
                throw new CensoredException("Blocked by Vodafone", "Vodafone", 100);
            } else if (header.getValue().contains("http://blockpage.bt.com/pcstaticpage/blocked.html")) {
                Log.e("Blocked", "Blocked by BT");
                throw new CensoredException("Blocked by BT", "BT", 100);
            } else if (header.getValue().contains("http://www.talktalk.co.uk/notice/parental-controls?accessurl")) {
                Log.e("Blocked", "Blocked by TalkTalk");
                throw new CensoredException("Blocked by TalkTalk", "TalkTalk", 100);
            } else if (header.getValue().contains("http://www.plus.net/support/security/abuse/blocked.shtml")) {
                Log.e("Blocked", "Blocked by PlusNet");
                throw new CensoredException("Blocked by PlusNet", "PlusNet", 100);
            } else if (header.getValue().contains("http://mobile.three.co.uk/pc/Live/pcreator/live/100004/pin/blocked?")) {
                Log.e("Blocked", "Blocked by Three");
                throw new CensoredException("Blocked by Three", "Three", 100);
            } else if (header.getValue().contains("http://m.virginmedia.com/MiscPages/AdultWarning.aspx")) {
                Log.e("Blocked", "Blocked by VirginMobile");
                throw new CensoredException("Blocked by VirginMobile", "VirginMobile", 100);
            } else if (header.getValue().contains("http://assets.o2.co.uk/18plusaccess/")) {
                Log.e("Blocked", "Blocked by O2");
                throw new CensoredException("Blocked by O2", "O2", 100);
            } else if (header.getName().contains("http://assets.virginmedia.com/parental.html")) {
                Log.e("Blocked", "Blocked by Virgin Media");
                throw new CensoredException("Blocked by Virgin Media", "VirginMedia", 100);
            } else if (header.getName().contains("http://block.nb.sky.com")) {
                Log.e("Blocked", "Blocked by Sky");
                throw new CensoredException("Blocked by Sky", "Sky", 100);
            }
            else
            {
                return 0;
            }
        }
        else
        {
            return -1;
        }
    }

















    @Deprecated
    public void notifyBackEnd(String url, String hmac, Pair<Boolean,Integer> results,String ISP, String SIM)
    {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        JSONObject json;
        ISPMeta ispMeta = getISPMeta();
        HttpPost httpost = new HttpPost("https://api.blocked.org.uk/1.2/response/httpt");

        if(!url.startsWith("http"))
            url = "http://" + url;

        httpost.setHeader("Accept", "application/json");

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("probe_uuid", settings.getString(API.SETTINGS_UUID, "")));
        nvps.add(new BasicNameValuePair("url", url));
        if((results.first))
        {
            nvps.add(new BasicNameValuePair("status", "blocked"));
        }
        else
        {
            nvps.add(new BasicNameValuePair("status", "ok"));
        }
        SimpleDateFormat sDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sDF.setTimeZone(TimeZone.getTimeZone("utc"));
        nvps.add(new BasicNameValuePair("date", sDF.format(new Date())));
        nvps.add(new BasicNameValuePair("config", "2014051801"));
        try
        {
            nvps.add(new BasicNameValuePair("signature", SignHeaders(settings.getString(API.SETTINGS_PROBE_PRIVATE_KEY, ""), nvps)));
        }
        catch (Exception e)
        {
            nvps.add(new BasicNameValuePair("signature",""));
        }

        try
        {
            nvps.add(new BasicNameValuePair("ip_network",ispMeta.ipAddress));
        }
        catch (Exception e)
        {
            nvps.add(new BasicNameValuePair("ip_network","0.0.0.0"));
        }

        try
        {
            nvps.add(new BasicNameValuePair("network_name", ispMeta.ispName));
        }
        catch (Exception e)
        {
            nvps.add(new BasicNameValuePair("network_name", "ether"));
        }

        nvps.add(new BasicNameValuePair("http_status", "0"));

        nvps.add(new BasicNameValuePair("confidence", Integer.toString(results.second)));

        try
        {
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

            HttpResponse response = httpclient.execute(httpost);
            String rawJSON = EntityUtils.toString(response.getEntity());
            response.getEntity().consumeContent();
            Log.e("API Raw JSON",rawJSON);
            json = new JSONObject(rawJSON);

            //TODO In future versions we'll check for success and store it for later if it failed
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Deprecated
    private String SignHeaders(String dataToSign) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, NoSuchProviderException, SignatureException
    {
        Log.e("SignHeaders",dataToSign);
        return SignHeaders(dataToSign,true);
    }

    @Deprecated
    private String SignHeaders(String dataToSign, boolean isUser) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, NoSuchProviderException, SignatureException
    {
        PKCS8EncodedKeySpec spec;
        if(isUser)
        {
            spec = new PKCS8EncodedKeySpec(Base64.decode(settings.getString(SETTINGS_USER_PRIVATE_KEY,"").getBytes(), 0));
        }
        else
        {
            spec = new PKCS8EncodedKeySpec(Base64.decode(settings.getString(SETTINGS_PROBE_PRIVATE_KEY,"").getBytes(), 0));
        }

        KeyFactory kf = KeyFactory.getInstance("RSA","BC");
        PrivateKey pk = kf.generatePrivate(spec);
        byte[] signed = null;

        //Log.e("algorithm", pk.getAlgorithm());

        Signature instance = Signature.getInstance("SHA1withRSA");
        instance.initSign(pk);
        instance.update(dataToSign.getBytes());
        signed = instance.sign();

        Log.e("privateKey",settings.getString(SETTINGS_USER_PRIVATE_KEY,""));
        Log.e("privateKey",settings.getString(SETTINGS_PROBE_PRIVATE_KEY,""));
        //Log.e("Signature",Base64.encodeToString(signed, Base64.NO_WRAP));

        return Base64.encodeToString(signed, Base64.NO_WRAP);
    }


}
