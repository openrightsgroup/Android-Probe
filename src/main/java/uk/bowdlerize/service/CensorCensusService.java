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
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
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
import java.util.Date;

import uk.bowdlerize.API;
import uk.bowdlerize.MainActivity;
import uk.bowdlerize.R;
import uk.bowdlerize.cache.LocalCache;
import uk.bowdlerize.fragments.ProgressFragment;
import uk.bowdlerize.fragments.ResultsGrid;
import uk.bowdlerize.support.CensorPayload;
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
    int bytes = 0;

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
        Intent newIntent = new Intent();
        newIntent.setAction(ProgressFragment.ORG_BROADCAST);
        newIntent.putExtra(ProgressFragment.ORG_BROADCAST,ProgressFragment.TALKING_TO_ORG);
        sendBroadcast(newIntent);

        new Thread()
        {
            public void run()
            {
                String url;
                String hash;
                try
                {
                    warnOnPrep();
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
                    onProbeFinish();
                }

            }
        }.start();
    }

    private void warnOnPrep()
    {
        mBuilder.setStyle(new NotificationCompat.InboxStyle()
                .setBigContentTitle(getString(R.string.notifTitle) + " - " + getString(R.string.notifRequstURL))
                .addLine(getString(R.string.notifRequesting))
                .addLine("( "+ getString(R.string.notifPolling) +" blocked.org.uk )")
                .setSummaryText(Integer.toString(checkedCount) + " " + getString(R.string.notifChecked) + " / " + Integer.toString(censoredCount) + " " + getString(R.string.notifPossBlock)))
                .setSmallIcon(R.drawable.ic_stat_in_progress)
                .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_ooni_large))
                .setPriority(Notification.PRIORITY_MAX)
                .setTicker(getString(R.string.notifTitle) + " - " + getString(R.string.notifRequstURL))
                .setAutoCancel(false);
        mBuilder.setProgress(2,1,true);
        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        /*Intent newIntent = new Intent();
        newIntent.setAction(ProgressFragment.ORG_BROADCAST);
        newIntent.putExtra(ProgressFragment.ORG_BROADCAST,ProgressFragment.TALKING_TO_ORG);
        sendBroadcast(newIntent);*/
    }

    private void warnOnError()
    {
        mBuilder.setStyle(new NotificationCompat.InboxStyle()
                .setBigContentTitle(getString(R.string.notifTitle) + " - No URLs")
                .addLine(getString(R.string.notifNoURLToCheck))
                .addLine(getString(R.string.notifAddURL))
                .setSummaryText(Integer.toString(checkedCount) + " " + getString(R.string.notifChecked) + " / " + Integer.toString(censoredCount) + " " + getString(R.string.notifPossBlock)))
                .setSmallIcon(R.drawable.ic_stat_waiting)
                .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_ooni_large))
                .setPriority(Notification.PRIORITY_MAX)
                .setTicker(getString(R.string.notifTitle) + " - No URLs")
                .setAutoCancel(false);
        mBuilder.setProgress(0,0,false);

        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

        Intent newIntent = new Intent();
        newIntent.setAction(ProgressFragment.ORG_BROADCAST);
        newIntent.putExtra(ProgressFragment.ORG_BROADCAST,ProgressFragment.NO_URLS);
        sendBroadcast(newIntent);
    }

    private void performProbe(final Intent intent)
    {
        Intent newIntent = new Intent();
        newIntent.setAction(ProgressFragment.ORG_BROADCAST);
        newIntent.putExtra(ProgressFragment.ORG_BROADCAST,ProgressFragment.TALKING_TO_ISP);
        sendBroadcast(newIntent);

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
                .setBigContentTitle(getString(R.string.notifTitle) + " - " + getString(R.string.notifURLRecv))
                .addLine(getString(R.string.notifNewURL))
                .addLine(getString(R.string.notifSanityCheck))
                .setSummaryText(Integer.toString(checkedCount) + " " + getString(R.string.notifChecked) + " / " + Integer.toString(censoredCount) + " " + getString(R.string.notifPossBlock)))
                .setSmallIcon(R.drawable.ic_stat_in_progress)
                .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_ooni_large))
                .setPriority(Notification.PRIORITY_MAX)
                .setTicker(getString(R.string.notifTitle) + " - " + getString(R.string.notifURLRecv))
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

                        //Pair<Boolean,Integer> wasCensored;
                        CensorPayload censorPayload;

                        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

                        mBuilder.setStyle(new NotificationCompat.InboxStyle()
                                .setBigContentTitle(getString(R.string.notifTitle) + " - " + getString(R.string.notifCheckURL))
                                .addLine(getString(R.string.notifStartAt)+ " " + currentDateTimeString)
                                .addLine(getString(R.string.notifCheckURL) + ".....")
                                .addLine("MD5: " + hash)
                                .setSummaryText(Integer.toString(checkedCount) + " " + getString(R.string.notifChecked) + " / " + Integer.toString(censoredCount) + " " + getString(R.string.notifPossBlock))
                        );
                        mBuilder.setProgress(2,1,true);
                        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

                        try
                        {
                            if(null == url)
                                throw new NullPointerException();

                            //Do the actual check
                            censorPayload = checkURL(url);

                            //We're complete - update the time
                            currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

                            //Update our local stats
                            setCounts(censorPayload.wasCensored());

                            censorPayload.MD5 = hash;

                            Intent ORGCensorIntent = new Intent();
                            ORGCensorIntent.setAction(ProgressFragment.ORG_BROADCAST);
                            ORGCensorIntent.putExtra("url",url);
                            ORGCensorIntent.putExtra("hash",hash);
                            ORGCensorIntent.putExtra("date",currentDateTimeString);

                            if(censorPayload.wasCensored())
                            {
                                ORGCensorIntent.putExtra(ProgressFragment.ORG_BROADCAST,ProgressFragment.BLOCKED);
                                ORGCensorIntent.putExtra(ResultsGrid.INTENT_FILTER, LocalCache.RESULT_BLOCKED);

                                mBuilder.setTicker(getString(R.string.notifFoundBlock));
                                mBuilder.setStyle(new NotificationCompat.InboxStyle()
                                        .setBigContentTitle(getString(R.string.notifTitle) + " - " + getString(R.string.notifWaiting))
                                        .addLine(getString(R.string.notifLastChk) + ": " + currentDateTimeString)
                                        .addLine(getString(R.string.notifLastURLBlocked))
                                        .addLine("MD5: " + intent.getStringExtra("hash"))
                                        .setSummaryText(Integer.toString(checkedCount) + " " + getString(R.string.notifChecked) + " / " + Integer.toString(censoredCount) + " " + getString(R.string.notifPossBlock))

                                );
                                mBuilder.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_ooni_large_censored));
                            }
                            else
                            {

                                ORGCensorIntent.putExtra(ProgressFragment.ORG_BROADCAST,ProgressFragment.OK);
                                ORGCensorIntent.putExtra(ResultsGrid.INTENT_FILTER,LocalCache.RESULT_OK);

                                mBuilder.setTicker(getString(R.string.notifTitle) + " - " + getString(R.string.notifLastChkNotBlock));
                                mBuilder.setStyle(new NotificationCompat.InboxStyle()
                                        .setBigContentTitle(getString(R.string.notifTitle) + " - " + getString(R.string.notifWaiting))
                                        .addLine(getString(R.string.notifLastChk) + ": " + currentDateTimeString)
                                        .addLine(getString(R.string.notifLastChkNotBlock))
                                        .setSummaryText(Integer.toString(checkedCount) + " " + getString(R.string.notifChecked) + " / " + Integer.toString(censoredCount) + " " + getString(R.string.notifPossBlock))
                                );
                            }

                            sendBroadcast(ORGCensorIntent);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();

                            //We're complete - update the time
                            currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

                            mBuilder.setStyle(new NotificationCompat.InboxStyle()
                                    .setBigContentTitle(getString(R.string.notifTitle) + " - " + getString(R.string.notifError))
                                    .addLine(getString(R.string.notifLastChk) + ": " + currentDateTimeString)
                                    .addLine(getString(R.string.notifException))
                                    .setSummaryText(Integer.toString(checkedCount) + " " + getString(R.string.notifChecked) + " / " + Integer.toString(censoredCount) + " " + getString(R.string.notifPossBlock))
                            );

                            censorPayload = null;
                        }


                        mBuilder.setProgress(0,0,false);
                        mBuilder.setSmallIcon(R.drawable.ic_stat_waiting);
                        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());

                        //Send the details back regardless (will chew DB space but will give a clearer picture)
                        //api.notifyBackEnd(url,"",wasCensored, intent.getStringExtra("isp"), intent.getStringExtra("sim"));
                        api.notifyBackEnd(censorPayload);

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
            //TODO Setback to 60000
            long repeat = (long) (getPreferences(CensorCensusService.this).getInt(API.SETTINGS_FREQUENCY, 1) * 60000);//60000 or 5000
            Log.e("onProbeFinish",Long.toString(repeat));
            Log.e("onProbeFinish","          -         ");
            Log.e("onProbeFinish","          -         ");
            Log.e("onProbeFinish","          -         ");
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime() + repeat, pi);
        }
        else
        {
            Log.e("onProbeFinish","Cancel everything!");
            Log.e("onProbeFinish","          -         ");
            Log.e("onProbeFinish","          -         ");
            Log.e("onProbeFinish","          -         ");
            am.cancel(pi); // cancel any existing alarms
            stopSelf();
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
                requestHeaders.put(hdr.getName(),hdr.getValue());
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
                responseHeaders.put(hdr.getName(),hdr.getValue());
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
    private CensorPayload checkURL(String checkURL) throws IllegalArgumentException, URISyntaxException
    {
        if (!checkURL.startsWith("http"))
            checkURL = "http://" + checkURL;

        CensorPayload censorPayload = new CensorPayload(checkURL);

        Uri mUri = Uri.parse(checkURL);

        if (null == mUri.getEncodedQuery()) {
            checkURL = mUri.getScheme() + "://" + mUri.getHost() + mUri.getPath();
        } else {
            checkURL = mUri.getScheme() + "://" + mUri.getHost() + mUri.getPath() + "?" + URLEncoder.encode(mUri.getEncodedQuery());
        }

        Log.e("Checking url", checkURL);

        client = new DefaultHttpClient();

        /*headRequest = new HttpHead(checkURL);
        headRequest.setHeader("User-Agent", "OONI Android Probe");*/

        httpGet = new HttpGet(checkURL);
        httpGet.setHeader("User-Agent", "OONI Android Probe");

        try {
            //response = client.execute(headRequest);
            client.addResponseInterceptor(new HttpResponseInterceptor() {
                @Override
                public void process(HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException
                {
                    if (httpResponse.getStatusLine().getStatusCode() == 302 || httpResponse.getStatusLine().getStatusCode() == 301)
                    {
                        for (Header hdr : httpResponse.getAllHeaders())
                        {
                            if (hdr.getName().equals("Location"))
                            {
                                /*if (hdr.getValue().equals("http://ee-outage.s3.amazonaws.com/content-blocked/content-blocked-v1.html") ||
                                    hdr.getValue().contains("http://ee-outage.s3.amazonaws.com"))
                                {
                                    Log.e("Blocked", "Blocked by EE");
                                    throw new CensoredException("Blocked by EE", "EE", 100);
                                }
                                else if (hdr.getValue().contains("http://www.t-mobile.co.uk/service/wnw-mig/entry/") ||
                                         hdr.getValue().contains("http://tmobile.ee.co.uk/common/system_error_pages/outage_wnw.html"))
                                {
                                    Log.e("Blocked", "Blocked by TMobile");
                                    throw new CensoredException("Blocked by TMobile", "TMobile", 100);
                                }
                                else if (hdr.getValue().contains("http://online.vodafone.co.uk/dispatch/Portal/ContentControlServlet?type=restricted"))
                                {
                                    Log.e("Blocked", "Blocked by Vodafone");
                                    throw new CensoredException("Blocked by Vodafone", "Vodafone", 100);
                                }
                                else if (hdr.getValue().contains("http://blockpage.bt.com/pcstaticpage/blocked.html"))
                                {
                                    Log.e("Blocked", "Blocked by BT");
                                    throw new CensoredException("Blocked by BT", "BT", 100);
                                }
                                else if (hdr.getValue().contains("http://www.talktalk.co.uk/notice/parental-controls?accessurl"))
                                {
                                    Log.e("Blocked", "Blocked by TalkTalk");
                                    throw new CensoredException("Blocked by TalkTalk", "TalkTalk", 100);
                                }
                                else if (hdr.getValue().contains("http://www.plus.net/support/security/abuse/blocked.shtml"))
                                {
                                    Log.e("Blocked", "Blocked by PlusNet");
                                    throw new CensoredException("Blocked by PlusNet", "PlusNet", 100);
                                }
                                else if (hdr.getValue().contains("http://mobile.three.co.uk/pc/Live/pcreator/live/100004/pin/blocked?"))
                                {
                                    Log.e("Blocked", "Blocked by Three");
                                    throw new CensoredException("Blocked by Three", "Three", 100);
                                }
                                else if (hdr.getValue().contains("http://m.virginmedia.com/MiscPages/AdultWarning.aspx"))
                                {
                                    Log.e("Blocked", "Blocked by VirginMobile");
                                    throw new CensoredException("Blocked by VirginMobile", "VirginMobile", 100);
                                }
                                else if (hdr.getValue().contains("http://assets.o2.co.uk/18plusaccess/"))
                                {
                                    Log.e("Blocked", "Blocked by O2");
                                    throw new CensoredException("Blocked by O2", "O2", 100);
                                }*/
                                api.checkHeader(hdr);
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
        catch (CensoredException CE) {
            censorPayload.consumeCensoredException(CE);
            return censorPayload;
        }
        catch (UnknownHostException uhe)
        {
            uhe.printStackTrace();
            censorPayload.consumeError(uhe.getMessage());
            return censorPayload;
        }
        catch (ConnectTimeoutException CTE)
        {
            CTE.printStackTrace();
            censorPayload.consumeError(CTE.getMessage());
            return censorPayload;
        }
        catch (NoHttpResponseException NHRE)
        {
            NHRE.printStackTrace();
            censorPayload.consumeError(NHRE.getMessage());
            return censorPayload;
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
            censorPayload.consumeError(ioe.getMessage());
            return censorPayload;
        }
        catch (IllegalStateException ise)
        {
            ise.printStackTrace();
            censorPayload.setCensored(false);
            censorPayload.setConfidence(0);
            return censorPayload;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            censorPayload.setCensored(false);
            censorPayload.setConfidence(0);
            return censorPayload;
        }

        int statusCode = response.getStatusLine().getStatusCode();

        censorPayload.setReturnCode(statusCode);

        Log.e("checkURL code", Integer.toString(statusCode));
        if (statusCode == 403 || statusCode == 404)
        {
            censorPayload.setCensored(true);
            censorPayload.setConfidence(25);
            return censorPayload;
        }
        else if(statusCode == 504 || statusCode == 503 || statusCode == 500)
        {
            censorPayload.consumeError("Server Issue " + Integer.toString(statusCode));
            return censorPayload;
        }

        String phrase = response.getStatusLine().getReasonPhrase();

        Log.e("checkURL phrase", phrase);

        if (phrase.contains("orbidden"))
        {
            censorPayload.setCensored(true);
            censorPayload.setConfidence(50);
            return censorPayload;
        }

        if (phrase.contains("blocked")) {
            censorPayload.setCensored(true);
            censorPayload.setConfidence(100);
            return censorPayload;
        }

        for(Header hdr : response.getAllHeaders())
        {
            Log.e("checkURL header",hdr.getName() + " / " + hdr.getValue());
        }

        censorPayload.setCensored(false);
        censorPayload.setConfidence(1);
        return censorPayload;
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
