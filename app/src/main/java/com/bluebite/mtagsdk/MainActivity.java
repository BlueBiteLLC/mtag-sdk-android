//Copyright 2018 Blue Bite LLC.
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package com.bluebite.mtagsdk;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Patterns;
import android.widget.TextView;

import com.bluebite.mtag_sdk.API;
import com.bluebite.mtag_sdk.BlueBiteInteractionDelegate;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements BlueBiteInteractionDelegate {
    public static final String TAG = MainActivity.class.getSimpleName();
    private static final String NDEF_DISCOVERED = NfcAdapter.ACTION_NDEF_DISCOVERED;

    TextView responseTextView;
    ConstraintLayout bg;

    private int red;
    private int green;

    private NfcAdapter mNfcAdapter;
    private API api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bg = findViewById(R.id.background);
        responseTextView = findViewById(R.id.textView);
        responseTextView.setMovementMethod(new ScrollingMovementMethod());

        red = ContextCompat.getColor(this, R.color.lightRed);
        green = ContextCompat.getColor(this, R.color.lightGreen);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        api = new API(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupForegroundDispatch();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopForegroundDispatch();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        responseTextView.setText("Processing Tag...");
        if (NDEF_DISCOVERED.equals(intent.getAction())) {
            String url = intent.getDataString();
            if (url != null && Patterns.WEB_URL.matcher(url).matches()) {
                api.interactionWasReceived(url);
            } else {
                Log.e(TAG, "invalid tag data: " + intent.getDataString());
                interactionDidFail("Invalid tag data: \"" + intent.getDataString() + "\"");
            }
        }
    }

    @Override
    public void interactionDataWasReceived(JSONObject results) {
        Log.i(TAG, "Interaction received: " + results);
        try {
            responseTextView.setText("Response:  " + results.toString(2));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            if (results.getBoolean("tagVerified")) {
                bg.setBackgroundColor(green);
            } else {
                bg.setBackgroundColor(red);
            }
        } catch (JSONException e) {
            Log.e(TAG, "No tag_verified key in results");
            bg.setBackgroundColor(red);
            e.printStackTrace();
        }

    }

    @Override
    public void interactionDidFail(String error) {
        Log.i(TAG, "Interaction did fail: " + error);
        responseTextView.setText("" + error);
        bg.setBackgroundColor(red);
    }

    /**
     * gives the application priority for handling discovered NFC tags with http(s) data schemes.
     */
    public void setupForegroundDispatch() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if(mNfcAdapter == null) {
            mNfcAdapter = NfcAdapter.getDefaultAdapter(this); //get devices default NFC adapter
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        String[][] techListsArray = new String[][] { new String[] { NfcF.class.getName() } };

        IntentFilter[] filters = new IntentFilter[2];

        // https
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        filters[0].addDataScheme("https");

        //http
        filters[1] = new IntentFilter();
        filters[1].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[1].addCategory(Intent.CATEGORY_DEFAULT);
        filters[1].addDataScheme("http");

        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, techListsArray);
    }

    /**
     * takes away priority for applications handling discovered NFC tags with http(s) data schemes.
     */
    public void stopForegroundDispatch() {
        mNfcAdapter.disableForegroundDispatch(this);
    }
}
