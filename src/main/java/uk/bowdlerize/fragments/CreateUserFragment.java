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
package uk.bowdlerize.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.internal.ed;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import uk.bowdlerize.API;
import uk.bowdlerize.MainActivity;
import uk.bowdlerize.R;


public class CreateUserFragment extends Fragment
{
    View progressBar;
    EditText emailAddressET,passwordET,privKeyET;
    Button registerButton, loginButton, checkButton;
    TextView userIntro;
    private Callbacks mCallbacks = sDummyCallbacks;
    public static final int USER_NOTHING = 0;
    public static final int USER_PENDING = 1;
    public static final int USER_COMPLETE = 2;
    API api;
    SharedPreferences settings = null;


    public CreateUserFragment()
    {
    }


    public interface Callbacks
    {
        /**
         * Callback for when an item has been selected.
         */
        public void updateStatus(int type);
    }

    private static Callbacks sDummyCallbacks = new Callbacks()
    {
        @Override
        public void updateStatus(int type)
        {
        }
    };

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks))
        {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        api = new API(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_createuser, container, false);

        settings = getActivity().getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);

        progressBar = rootView.findViewById(R.id.progressBar);
        emailAddressET = (EditText) rootView.findViewById(R.id.emailAddressET);
        passwordET = (EditText) rootView.findViewById(R.id.passwordET);
        privKeyET = (EditText) rootView.findViewById(R.id.privKeyET);

        registerButton = (Button) rootView.findViewById(R.id.RegisterButton);
        loginButton = (Button) rootView.findViewById(R.id.LoginButton);
        checkButton = (Button) rootView.findViewById(R.id.CheckStatusButton);

        userIntro = (TextView) rootView.findViewById(R.id.userIntro);

        String privKey = settings.getString(API.SETTINGS_USER_PRIVATE_KEY,"");

        if(privKey.equals(""))
        {
            mCallbacks.updateStatus(USER_NOTHING);
        }
        else
        {
            //Set the EditText variables we need
            privKeyET.setText("-----BEGIN RSA PRIVATE KEY-----\n" + privKey + "\n-----END RSA PRIVATE KEY-----");
            privKeyET.setEnabled(false);
            emailAddressET.setText(settings.getString(API.SETTINGS_EMAIL_ADDRESS,""));
            emailAddressET.setEnabled(false);

            //Hide UI we don't need
            registerButton.setVisibility(View.GONE);
            passwordET.setVisibility(View.GONE);
            loginButton.setVisibility(View.GONE);
            checkButton.setVisibility(View.VISIBLE);

            //Update the main UI to indicate we're waiting for authorisation
            mCallbacks.updateStatus(USER_PENDING);

            //Check our pending status
            checkPending();
        }

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                registerNewAccount();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                loginAccount();
            }
        });

        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                checkPending();
            }
        });
        return rootView;
    }

    private void registerNewAccount()
    {
        progressBar.setVisibility(View.VISIBLE);

        new AsyncTask<Void, Void, String>()
        {
            @Override
            protected String doInBackground(Void... params)
            {
                return api.registerUser(emailAddressET.getText().toString(),passwordET.getText().toString());
            }

            @Override
            protected void onPostExecute(String privKey)
            {
                progressBar.setVisibility(View.GONE);
                if(null == privKey)
                {
                    Toast.makeText(getActivity(),"An error occured registering.\nTry again later",Toast.LENGTH_LONG).show();
                }
                else if(privKey.equals(""))
                {
                    Toast.makeText(getActivity(),"Private Key was invalid",Toast.LENGTH_LONG).show();
                }
                else
                {
                    String tmpPK = privKey.replace("-----BEGIN RSA PRIVATE KEY-----", "");
                    tmpPK = tmpPK.replace("-----END RSA PRIVATE KEY-----", "");
                    tmpPK = tmpPK.replace("\n", "");

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(API.SETTINGS_USER_PRIVATE_KEY,tmpPK);
                    editor.putString(API.SETTINGS_EMAIL_ADDRESS,emailAddressET.getText().toString());
                    editor.commit();

                    registerButton.setVisibility(View.GONE);
                    loginButton.setVisibility(View.GONE);
                    checkButton.setVisibility(View.VISIBLE);
                    passwordET.setVisibility(View.GONE);
                    privKeyET.setText(privKey);
                    privKeyET.setEnabled(false);
                    emailAddressET.setEnabled(false);
                    mCallbacks.updateStatus(USER_PENDING);
                }
            }
        }.execute(null, null, null);
    }

    private void loginAccount()
    {

    }

    private void checkPending()
    {
        progressBar.setVisibility(View.VISIBLE);

        new AsyncTask<Void, Void, String>()
        {
            @Override
            protected String doInBackground(Void... params)
            {
                try
                {
                    return api.getUserStatus();
                }
                catch (Exception e)
                {
                    //Lazy I know
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String status)
            {
                progressBar.setVisibility(View.GONE);

                if(null == status)
                {
                    Toast.makeText(getActivity(),"An error occurred checking your account.\nTry again later",Toast.LENGTH_LONG).show();
                }
                else if(status.equals("ok"))
                {
                    mCallbacks.updateStatus(USER_COMPLETE);
                    checkButton.setVisibility(View.GONE);
                    userIntro.setText(getString(R.string.createUserIntroComplete));
                }
                else
                {
                    Toast.makeText(getActivity(),"Your account is currently " + status,Toast.LENGTH_LONG).show();
                }
            }
        }.execute(null, null, null);
    }
}