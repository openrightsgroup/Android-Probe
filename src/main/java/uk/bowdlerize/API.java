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
package uk.bowdlerize;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

public class API
{
    private Context mContext;
    public API(){}
    public static String USER_STATUS_PENDING = "pending";
    public static String USER_STATUS_OK = "ok";
    public static String USER_STATUS_FAILED = "failed";

    public static String SETTINGS_EMAIL_ADDRESS = "emailAddress";
    public static String SETTINGS_USER_PRIVATE_KEY = "userPrivKey";
    public static String SETTINGS_PROBE_PRIVATE_KEY = "probePrivKey";

    private SharedPreferences settings = null;

    public API(Context context)
    {
        this.mContext = context;
        settings = mContext.getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    public String registerUser(String emailAddress, String password)
    {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        JSONObject json;
        HttpPost httpost = new HttpPost("https://bowdlerize.co.uk/api/1.1/register/user");

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
                return json.getString("private_key");
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
        HttpPost httpost = new HttpPost("https://bowdlerize.co.uk/api/1.1/status/user");

        httpost.setHeader("Accept", "application/json");

        String emailAddress = settings.getString(SETTINGS_EMAIL_ADDRESS,"");

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("email", emailAddress));
        nvps.add(new BasicNameValuePair("signature", SignHeaders(emailAddress)));

        try
        {
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse response = httpclient.execute(httpost);
            String rawJSON = EntityUtils.toString(response.getEntity());
            response.getEntity().consumeContent();
            //Log.e("rawJSON",rawJSON);
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

    public String prepareProbe() throws NoSuchPaddingException, UnsupportedEncodingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException, SignatureException
    {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        JSONObject json;
        HttpPost httpost = new HttpPost("https://bowdlerize.co.uk/api/1.1/prepare/probe");

        httpost.setHeader("Accept", "application/json");

        String emailAddress = settings.getString(SETTINGS_EMAIL_ADDRESS,"");

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("email", emailAddress));
        nvps.add(new BasicNameValuePair("signature", SignHeaders(emailAddress)));

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
        HttpPost httpost = new HttpPost("https://bowdlerize.co.uk/api/1.1/register/probe");

        httpost.setHeader("Accept", "application/json");

        String emailAddress = settings.getString(SETTINGS_EMAIL_ADDRESS,"");

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("email", emailAddress));
        nvps.add(new BasicNameValuePair("probe_seed", probeSeed));
        nvps.add(new BasicNameValuePair("probe_uuid", probe_uuid));
        nvps.add(new BasicNameValuePair("type", "android"));
        nvps.add(new BasicNameValuePair("cc", country));
        nvps.add(new BasicNameValuePair("signature", SignHeaders(probe_uuid)));

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
                return json.getString("private_key");
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

    private String SignHeaders(String dataToSign) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, NoSuchProviderException, SignatureException
    {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.decode(settings.getString(SETTINGS_USER_PRIVATE_KEY,"").getBytes(), 0));
        KeyFactory kf = KeyFactory.getInstance("RSA","BC");
        PrivateKey pk = kf.generatePrivate(spec);
        byte[] signed = null;

        //Log.e("algorithm", pk.getAlgorithm());

        Signature instance = Signature.getInstance("SHA1withRSA");
        instance.initSign(pk);
        instance.update(dataToSign.getBytes());
        signed = instance.sign();

        //Log.e("privateKey",settings.getString(SETTINGS_USER_PRIVATE_KEY,""));
        //Log.e("Signature",Base64.encodeToString(signed, Base64.NO_WRAP));

        return Base64.encodeToString(signed, Base64.NO_WRAP);
    }
}
