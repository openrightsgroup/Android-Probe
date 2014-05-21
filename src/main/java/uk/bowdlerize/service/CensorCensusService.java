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
package uk.bowdlerize.service;


import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Pair;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import uk.bowdlerize.API;
import uk.bowdlerize.MainActivity;
import uk.bowdlerize.R;
import uk.bowdlerize.support.CensoredException;

import static uk.bowdlerize.support.Hashes.MD5;

public class CensorCensusService extends Service
{
    public static int NOTIFICATION_ID = 9000;
    static int NOTIFICATION_ID_COMPLETE = 9001;
    static String INTENT_FILTER = "uk.bowdlerize.service.censorcensusservice.CANCEL";
    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;
    Context mContext;
    BroadcastReceiver bR;
    int checkedCount = 0;
    int censoredCount = 0;
    boolean sendtoORG = false;
    DefaultHttpClient client;
    HttpHead headRequest;
    HttpGet httpGet;
    HttpResponse response = null;
    API api = null;

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId)
    {
        if(null == mNotifyManager)
            mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if(null == mBuilder)
            mBuilder = new NotificationCompat.Builder(this);

        if(null == api)
            api = new API(this);

        mContext = this;

        checkedCount = getPreferences(this).getInt("checkedCount", 0);
        censoredCount = getPreferences(this).getInt("censoredCount", 0);
        sendtoORG  = getPreferences(this).getBoolean("sendToOrg", false);

        //Lets findout why we've been started
        if(intent.getBooleanExtra(API.EXTRA_POLL,false) || intent.getBooleanExtra(API.EXTRA_GCM_TICKLE,false))
        {
            prepProbe(intent);
        }
        else if(intent.hasExtra("url") && !intent.getStringExtra("url").equals(""))
        {
            performProbe(intent);
        }
        else
        {
            onProbeFinish();
        }

        //If we're polling we probably want to stay alive
        if(getSharedPreferences(MainActivity.class.getSimpleName(),Context.MODE_PRIVATE).getInt(API.SETTINGS_GCM_PREFERENCE,API.SETTINGS_GCM_FULL) == API.SETTINGS_GCM_DISABLED)
        {
            return START_STICKY;
        }
        else
        {
            return START_NOT_STICKY;
        }
    }

    private void prepProbe(final Intent intent)
    {
        new Thread()
        {
            public void run()
            {
                String url;
                String hash;
                try
                {
                    url = api.getURLBasic();
                    hash = MD5(url);

                    intent.putExtra("url",url);
                    intent.putExtra("hash",hash);

                    performProbe(intent);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    url = null;
                    hash = ".....";
                    warnOnError();
                }
            }
        }.start();
    }

    private void warnOnError()
    {
        mBuilder.setStyle(new NotificationCompat.InboxStyle()
                .setBigContentTitle("Censor Census - Error")
                .addLine("There was an error getting the URL")
                .addLine("--------------")
                .setSummaryText(Integer.toString(checkedCount) + " Checked / " + Integer.toString(censoredCount) + " Possibly Censored"))
                .setSmallIcon(R.drawable.ic_stat_in_progress)
                .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_ooni_large))
                .setPriority(Notification.PRIORITY_MAX)
                .setTicker("Censor Census - Error")
                .setAutoCancel(false);

        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

    }

    private void performProbe(final Intent intent)
    {
        //If this is user submitted send it up for further research
        if(intent.getBooleanExtra("local",false))
        {
            new Thread()
            {
                public void run()
                {
                    try { api.submitURL(intent.getStringExtra("url")); } catch (Exception e) { e.printStackTrace(); }
                }
            }.start();
        }

        mBuilder.setStyle(new NotificationCompat.InboxStyle()
                .setBigContentTitle("Censor Census - URL Received")
                .addLine("Received a new url!")
                .addLine("Performing sanity checks...")
                .setSummaryText(Integer.toString(checkedCount) + " Checked / " + Integer.toString(censoredCount) + " Possibly Censored"))
                .setSmallIcon(R.drawable.ic_stat_in_progress)
                .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_ooni_large))
                .setPriority(Notification.PRIORITY_MAX)
                .setTicker("Censor Census - URL Received")
                .setAutoCancel(false);

        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        new Thread
        (
                new Runnable()
                {
                    @Override
                    public void run()
                    {
                        String url = intent.getStringExtra("url");
                        String hash = intent.getStringExtra("hash");

                        Pair<Boolean,Integer> wasCensored;

                        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

                        mBuilder.setStyle(new NotificationCompat.InboxStyle()
                                .setBigContentTitle("Censor Census - Checking URL")
                                .addLine("Started at " + currentDateTimeString)
                                .addLine("Checking URL.....")
                                .addLine("MD5: " + hash)
                                .setSummaryText(Integer.toString(checkedCount) + " Checked / " + Integer.toString(censoredCount) + " Possibly Censored")
                        );
                        mBuilder.setProgress(2,1,true);
                        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

                        try
                        {
                            if(null == url)
                                throw new NullPointerException();

                            //Do the actual check
                            wasCensored = checkURL(url);

                            //We're complete - update the time
                            currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

                            //Update our local stats
                            setCounts(wasCensored.first);

                            if(wasCensored.first)
                            {
                                mBuilder.setTicker("Found a possibly censored URL!");
                                mBuilder.setStyle(new NotificationCompat.InboxStyle()
                                        .setBigContentTitle("Censor Census - Waiting")
                                        .addLine("Last check: " + currentDateTimeString)
                                        .addLine("Last URL was possibly censored!")
                                        .addLine("MD5: " + intent.getStringExtra("hash"))
                                        .setSummaryText(Integer.toString(checkedCount) + " Checked / " + Integer.toString(censoredCount) + " Possibly Censored")

                                );
                                mBuilder.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_ooni_large_censored));
                            }
                            else
                            {
                                mBuilder.setStyle(new NotificationCompat.InboxStyle()
                                        .setBigContentTitle("Censor Census - Waiting")
                                        .addLine("Last check: " + currentDateTimeString)
                                        .addLine("Last URL wasn't censored!")
                                        .setSummaryText(Integer.toString(checkedCount) + " Checked / " + Integer.toString(censoredCount) + " Possibly Censored")
                                );
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();

                            //We're complete - update the time
                            currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

                            mBuilder.setStyle(new NotificationCompat.InboxStyle()
                                    .setBigContentTitle("Censor Census - Error")
                                    .addLine("Last check: " + currentDateTimeString)
                                    .addLine("An exception was encountered during the last check")
                                    .setSummaryText(Integer.toString(checkedCount) + " Checked / " + Integer.toString(censoredCount) + " Possibly Censored")
                            );

                            wasCensored = new Pair(false,0);
                        }


                        mBuilder.setProgress(0,0,false);
                        mBuilder.setSmallIcon(R.drawable.ic_stat_waiting);
                        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

                        //Send the details back regardless (will chew DB space but will give a clearer picture)
                        notifyBackEnd(url,"",wasCensored, intent.getStringExtra("isp"), intent.getStringExtra("sim"));

                        /*if(sendtoORG)
                            notifyOONIDirectly(url,wasCensored, intent.getStringExtra("isp"), intent.getStringExtra("sim"));
                        */
                        onProbeFinish();
                    }
                }
        ).start();
    }

    private void onProbeFinish()
    {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent i = new Intent(CensorCensusService.this, CensorCensusService.class);
        i.putExtra(API.EXTRA_POLL,true);
        PendingIntent pi = PendingIntent.getService(CensorCensusService.this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        //If we are polling lets set our next tick
        if(getSharedPreferences(MainActivity.class.getSimpleName(),Context.MODE_PRIVATE).getInt(API.SETTINGS_GCM_PREFERENCE,API.SETTINGS_GCM_FULL) == API.SETTINGS_GCM_DISABLED)
        {
            am.cancel(pi); // cancel any existing alarms
            long repeat = (long) (getPreferences(CensorCensusService.this).getInt(API.SETTINGS_FREQUENCY, 1) * 60000);
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime() + repeat, pi);
        }
        else
        {
            am.cancel(pi); // cancel any existing alarms
            stopSelf();
        }
    }

    private void notifyBackEnd(String url, String hmac, Pair<Boolean,Integer> results,String ISP, String SIM)
    {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        JSONObject json;
        HttpPost httpost = new HttpPost("https://api.blocked.org.uk/1.2/response/httpt");

        //Log.e("notifyBackend",url + " / " + Boolean.toString(results.first));
        httpost.setHeader("Accept", "application/json");

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("probe_uuid", getSharedPreferences(MainActivity.class.getSimpleName(),Context.MODE_PRIVATE).getString(API.SETTINGS_UUID,"")));
        nvps.add(new BasicNameValuePair("url", url));
        if(((Boolean) results.first) == true)
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
        nvps.add(new BasicNameValuePair("config", "1"));
        try
        {
            nvps.add(new BasicNameValuePair("signature", API.SignHeaders(getSharedPreferences(MainActivity.class.getSimpleName(),Context.MODE_PRIVATE).getString(API.SETTINGS_PROBE_PRIVATE_KEY, ""), nvps)));
        }
        catch (Exception e)
        {
            nvps.add(new BasicNameValuePair("signature",""));
        }

        nvps.add(new BasicNameValuePair("ip_network", "1.1.1.1"));
        nvps.add(new BasicNameValuePair("network_name", ISP));
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

    private void notifyOONIDirectly(String url, Pair<Boolean,Integer> results,String ISP, String SIM)
    {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        JSONObject json;
        HttpPost httpost = new HttpPost("https://blocked.org.uk/ooni-backend/submit");

        httpost.setHeader("Accept", "application/json");

        //I don't like YAML
        JSONObject ooniPayload = new JSONObject();

        //Lazy mass try / catch
        try
        {
            ooniPayload.put("agent","Fake Agent");
            ooniPayload.put("body_length_match",false);
            ooniPayload.put("body_proportion",1.0);
            ooniPayload.put("control_failure",null);
            ooniPayload.put("experiment_failure",null);
            ooniPayload.put("factor",0.8);
            //ooniPayload.put("headers_diff",);
            ooniPayload.put("headers_match",false);

            //We only handle one request at the time but the spec specifies an array
            JSONObject ooniRequest = new JSONObject();
            ooniRequest.put("body",null);
            JSONObject requestHeaders = new JSONObject();
            for(Header hdr : headRequest.getAllHeaders())
            {
                requestHeaders.put(hdr.getName().toString(),hdr.getValue().toString());
            }
            ooniRequest.put("headers",requestHeaders);
            ooniRequest.put("method","HEAD");
            ooniRequest.put("url",url);

            JSONObject ooniResponse = new JSONObject();
            ooniResponse.put("body","");
            ooniResponse.put("code",response.getStatusLine().getStatusCode());
            JSONObject responseHeaders = new JSONObject();
            for(Header hdr : response.getAllHeaders())
            {
                responseHeaders.put(hdr.getName().toString(),hdr.getValue().toString());
            }

            ooniRequest.put("response",ooniResponse);

            JSONArray ooniRequests = new JSONArray();
            ooniRequests.put(ooniRequest);

            ooniPayload.put("requests",ooniRequests);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            httpost.setEntity(new StringEntity(ooniPayload.toString(), HTTP.UTF_8));

            HttpResponse response = httpclient.execute(httpost);
            String rawJSON = EntityUtils.toString(response.getEntity());
            response.getEntity().consumeContent();
            Log.e("ooni rawJSON",rawJSON);
            json = new JSONObject(rawJSON);

            //TODO In future versions we'll check for success and store it for later if it failed
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private void setCounts(boolean Censored)
    {
        checkedCount++;

        if(Censored)
            censoredCount++;

        saveCounts();
    }

    private int getCensoredCount()
    {
        return censoredCount;
    }

    private int getCheckedCount()
    {
        return checkedCount;
    }

    //Return whether the URL is censored and a confidence level (1-100)
    private Pair<Boolean,Integer> checkURL(String checkURL) throws IllegalArgumentException, URISyntaxException
    {
        if(!checkURL.startsWith("http"))
            checkURL = "http://" + checkURL;

        Uri mUri = Uri.parse(checkURL);

        if(null == mUri.getEncodedQuery())
        {
            checkURL = mUri.getScheme() + "://" + mUri.getHost() + mUri.getPath();
        }
        else
        {
            checkURL = mUri.getScheme() + "://" + mUri.getHost() + mUri.getPath() +"?" + URLEncoder.encode(mUri.getEncodedQuery());
        }

        Log.e("Checking url",checkURL);

        client = new DefaultHttpClient();

        /*headRequest = new HttpHead(checkURL);
        headRequest.setHeader("User-Agent", "OONI Android Probe");*/

        httpGet = new HttpGet(checkURL);
        httpGet.setHeader("User-Agent", "OONI Android Probe");

        try
        {
            //response = client.execute(headRequest);
            client.addResponseInterceptor( new HttpResponseInterceptor() {
                @Override
                public void process(HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException
                {
                    if(httpResponse.getStatusLine().getStatusCode() == 302 || httpResponse.getStatusLine().getStatusCode() == 301)
                    {
                        for(Header hdr : httpResponse.getAllHeaders())
                        {
                            if(hdr.getName().toString().equals("Location"))
                            {
                                if(hdr.getValue().equals("http://ee-outage.s3.amazonaws.com/content-blocked/content-blocked-v1.html"))
                                {
                                    Log.e("Blocked", "Blocked by EE");
                                    throw new CensoredException("Blocked by EE","EE",100);
                                }
                                else if (hdr.getValue().contains("http://www.t-mobile.co.uk/service/wnw-mig/entry/") || hdr.getValue().contains("http://tmobile.ee.co.uk/common/system_error_pages/outage_wnw.html"))
                                {
                                    Log.e("Blocked", "Blocked by TMobile");
                                    throw new CensoredException("Blocked by TMobile","TMobile",100);
                                }
                                else if (hdr.getValue().contains("http://online.vodafone.co.uk/dispatch/Portal/ContentControlServlet?type=restricted"))
                                {
                                    Log.e("Blocked", "Blocked by Vodafone");
                                    throw new CensoredException("Blocked by Vodafone","Vodafone",100);
                                }
                                else if (hdr.getValue().contains("http://blockpage.bt.com/pcstaticpage/blocked.html"))
                                {
                                    Log.e("Blocked", "Blocked by BT");
                                    throw new CensoredException("Blocked by BT","BT",100);
                                }
                                else if (hdr.getValue().contains("http://www.talktalk.co.uk/notice/parental-controls?accessurl"))
                                {
                                    Log.e("Blocked", "Blocked by TalkTalk");
                                    throw new CensoredException("Blocked by TalkTalk","TalkTalk",100);
                                }
                                else if (hdr.getValue().equals("http://www.plus.net/support/security/abuse/blocked.shtml"))
                                {
                                    Log.e("Blocked", "Blocked by PlusNet");
                                    throw new CensoredException("Blocked by PlusNet","PlusNet",100);
                                }
                            }
                        }
                    }

                    /*Log.e("intercepted return code",httpResponse.getStatusLine().toString());

                    for(Header hdr : httpResponse.getAllHeaders())
                    {
                        Log.e("intercepted header",hdr.getName().toString() + " / " + hdr.getValue().toString());
                    }
                    Log.e("intercepted header","------------------\r\n------------------\r\n------------------\r\n------------------\r\n------------------\r\n");*/

                }
            });
            response = client.execute(httpGet);
        }
        //This is the best case scenario!
        catch (CensoredException CE)
        {
            return new Pair(true,100);
        }
        catch(UnknownHostException uhe)
        {
            uhe.printStackTrace();
            return new Pair(false,50);
        }
        catch(ConnectTimeoutException CTE)
        {
            CTE.printStackTrace();
            return new Pair(true,5);
        }
        catch (NoHttpResponseException NHRE)
        {
            NHRE.printStackTrace();
            return new Pair(true,5);
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
            return new Pair(false,0);
        }
        catch (IllegalStateException ise)
        {
            ise.printStackTrace();
            return new Pair(false,0);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new Pair(false,0);
        }

        int statusCode = response.getStatusLine().getStatusCode();

        Log.e("checkURL code",Integer.toString(statusCode));
        if(statusCode == 403 || statusCode == 404 )
            return new Pair(true,25);

        String phrase = response.getStatusLine().getReasonPhrase();
        Log.e("checkURL phrase",phrase);
        if(phrase.contains("orbidden"))
            return new Pair(true,50);

        if(phrase.contains("blocked"))
            return new Pair(true,100);

        for(Header hdr : response.getAllHeaders())
        {
            Log.e("checkURL header",hdr.getName().toString() + " / " + hdr.getValue().toString());
        }



        return new Pair(false,1);
    }

    private void saveCounts()
    {
        SharedPreferences prefs = getPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("censoredCount", censoredCount);
        editor.putInt("checkedCount", checkedCount);
        editor.commit();
    }

    @Override
    public void onDestroy()
    {
        Log.e("onDestroy","Bye bye");
        saveCounts();
        super.onDestroy();
    }

    private SharedPreferences getPreferences(Context context)
    {
        return getSharedPreferences(MainActivity.class.getSimpleName(),Context.MODE_PRIVATE);
    }
}
