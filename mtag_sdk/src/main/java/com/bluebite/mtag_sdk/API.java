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

package com.bluebite.mtag_sdk;


import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/*
* Handles parsing, verifying, and passing results from an interaction to the InteractionDelegate.
* */
public class API {
    public static final String TAG = API.class.getSimpleName();

    private InteractionDelegate mDelegate;
    private Context mContext;

    private static int SMT_COUNTER_SEGMENTS = 5;
    private static int MTAG_ID_B10_LENGTH = 8;
    private static int MTAG_ID_B36_LENGTH = 6;
    private static int TECH_PREFIX_LENGTH = 1;

    public API(InteractionDelegate mDelegate, Context mContext) {
        this.mDelegate = mDelegate;
        this.mContext = mContext;
    }

    // TODO: check user agent
    /*
    * Method called by an activity which will handle parsing and verifying the provided
    * interaction url.
    *
    * Method does not return, instead passing its results to the mDelegate.
    * */
    public void interactionWasReceived(String url) {
        Log.v(TAG, "Interaction was received with url: " + url);
        String mTagId = parseMTagId(url);
        if (mTagId.equals("")) {
            mDelegate.interactionDidFail("Unable to parse mTag ID from Interaction URL: " + url);
        }

        String[] urlParts = url.split("/");
        String urlTail = urlParts[urlParts.length - 1];

        HashMap<String,String> params = new HashMap<>();
        if (urlParts.length == SMT_COUNTER_SEGMENTS) { // Counter URL
            params = handleCounterUrl(urlParts);
        }
        else if (urlTail.contains("&sig")) { // Auth Url
            HashMap<String, String> expectedQueryParams = new HashMap<>();
            expectedQueryParams.put("id", "uid");
            expectedQueryParams.put("num", "tag_version");
            expectedQueryParams.put("sig", "vid");
            params = handleAuthOrHidUrl(urlParts, expectedQueryParams);
        }
        else if (urlTail.contains("&tac")) { // HID URL
            HashMap<String, String> expectedQueryParams = new HashMap<>();
            expectedQueryParams.put("tagID", "hid");
            expectedQueryParams.put("tac", "vid");
            params = handleAuthOrHidUrl(urlParts, expectedQueryParams);
        }
        else {
            mDelegate.interactionDidFail("Invalid URL format for Interaction URL: " + url);
        }

        registerInteraction(mTagId, params);
    }

    private void registerInteraction(String mTagId, HashMap<String, String> params) {
        Log.d(TAG, "[registerInteraction]mTagId: " + mTagId + " params: " + params.toString());

        params.put("tag_id", mTagId);
        params.put("tech", "n");

        Log.d(TAG, "register params: " + params.toString());

        String targetUrl = "https://api.mtag.io/v2/interactions";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                targetUrl,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "Interactions response: " + response);
                        JSONObject parsedResponse = parseResponse(response);
                        mDelegate.interactionDataWasReceived(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "Interactions response error: " + error);
                        mDelegate.interactionDidFail(
                                error.getLocalizedMessage());
                    }
                });
        request.setShouldCache(false);
        Volley.newRequestQueue(mContext).add(request);
    }

    private JSONObject parseResponse(JSONObject response) {
        JSONObject formattedResponse = new JSONObject();
        try {
           JSONObject device = response.getJSONObject("device");
            formattedResponse.put("deviceCountry", device.getString("country"));
        } catch (JSONException e) {
            try {
                formattedResponse.put("deviceCountry", null);
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }

        // get tagverified
        try {
            formattedResponse.put("tagVerified", response.getString("tag_verified"));
        } catch (JSONException e) {
            try {
                formattedResponse.put("tagVerified", null);
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }

        // get the location info if any
        try {
            formattedResponse.put("location", response.getJSONObject("location"));
        } catch (JSONException e) {
            try {
                formattedResponse.put("location", null);
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }

        // get the campaign info if any
        try {
            formattedResponse.put("campaigns", response.getJSONArray("campaigns"));
        } catch (JSONException e) {
            try {
                formattedResponse.put("campaigns", null);
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }

        return formattedResponse;
    }

    /**
     Parses an Auth or HID URL into a dictionary.
     */
    private HashMap<String,String> handleAuthOrHidUrl(
            String[] urlParts, HashMap<String, String> expectedQueryParams) {
        Log.d(TAG, "handleAuthUrl" + Arrays.toString(urlParts));
        HashMap<String, String> params = new HashMap<>();

        //grab url query parameters
        String[] urlQueryParams = urlParts[urlParts.length - 1].split("\\?")[1].split("&");
        Log.d(TAG, "AuthUrl urlQUeryParams: " + Arrays.toString(urlQueryParams));


        for (String urlArg : urlQueryParams) {
            String[] splitArg = urlArg.split("=");
            String argName = splitArg[0];
            String argValue = splitArg[1];
            if (expectedQueryParams.containsKey(argName)) {
                params.put(expectedQueryParams.get(argName), argValue);
            }
        }
        return params;
    }

    /**
    * Handles parsing the relevant query parameters for a counter URL.
    * */
    private HashMap<String,String> handleCounterUrl(String[] urlParts) {
        Log.d(TAG, "handleCounterUrl" + Arrays.toString(urlParts));
        String urlTail = urlParts[urlParts.length - 1];
        String fullVID = urlTail.split("\\?")[0];
        HashMap<String, String> params = new HashMap<>();
        params.put("vid", fullVID);

        return params;
    }

    /*
    * Parses the mTag ID from the Interaction URL.
    * */
    private String parseMTagId(String url) {
        String[] urlParts = url.split("/");
        String urlTail = urlParts[urlParts.length - 1];
        String idAndTechType = "";

        if (urlTail.length() <= MTAG_ID_B10_LENGTH + TECH_PREFIX_LENGTH
                || urlTail.length() <= MTAG_ID_B36_LENGTH + TECH_PREFIX_LENGTH) {
            // basic mTag structure
            idAndTechType = urlTail;
        } else if (urlTail.contains("&sig") || urlTail.contains("&tac")) {
            // auth or hid tag
            idAndTechType = urlTail.split("\\?")[0];
        } else if (urlParts.length == SMT_COUNTER_SEGMENTS) {
            // smt counter tag
            String[] smtUrlAsArray = url.split("/");
            idAndTechType = smtUrlAsArray[smtUrlAsArray.length - 2];
        }
        Log.d(TAG, "idAndTechType: " + (idAndTechType != null ? idAndTechType : "IS NULL"));
        String mTagId = idAndTechType.substring(1);
        return convertIdToBase10(mTagId);
    }

    private String convertIdToBase10(String mTagId) {
        // assume its b10 even though it probably isn't
        int idAsBase10;

        try {
            idAsBase10 = Integer.parseInt(mTagId);
        } catch (NumberFormatException e) {
            // is base36 like we expected
            idAsBase10 = Integer.parseInt(mTagId, 36);
        }
        Log.d(TAG, "convertIdToBase10 b10: " + idAsBase10);
        return "" + idAsBase10;
    }



}
