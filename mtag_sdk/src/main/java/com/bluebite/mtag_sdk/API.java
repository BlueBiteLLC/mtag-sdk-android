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


import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;


/**
 * Handles parsing, verifying, and passing results from an interaction to the BlueBiteInteractionDelegate.
 * */
public class API {
    public static final String TAG = API.class.getSimpleName();

    private BlueBiteInteractionDelegate mDelegate;
    private AsyncHttpClient client = new AsyncHttpClient();

    public static int SMT_COUNTER_SEGMENTS = 5;
    private static int MTAG_ID_B10_LENGTH = 8;
    private static int MTAG_ID_B36_LENGTH = 6;
    private static int TECH_PREFIX_LENGTH = 1;

    /**
     * Error message to filter for if you want to handle a non-verifiable but otherwise
     * valid URL.
     */
    public static String ERROR_NON_AUTH_URL = "Invalid URL format for Interaction URL: ";

    public API(BlueBiteInteractionDelegate mDelegate) {
        this.mDelegate = mDelegate;
    }

    /**
     * Method called by an activity which will handle parsing and verifying the provided
     * interaction url.
     * Method does not return, instead passing its results to the mDelegate.
     * @param url Interaction URL/URL to verify.
     */
    public void interactionWasReceived(String url) {
        Log.v(TAG, "Interaction was received with url: " + url);
        String mTagId = parseMTagId(url);
        if (mTagId.equals("")) {
            mDelegate.interactionDidFail(ERROR_NON_AUTH_URL + url);
        }

        String[] urlParts = url.split("/");
        String urlTail = urlParts[urlParts.length - 1];

        HashMap<String,String> params = new HashMap<>();
        if (urlParts.length == SMT_COUNTER_SEGMENTS) { // Counter URL
            Log.d(TAG, "SMT Counter");
            params = handleCounterUrl(urlParts);
        }
        else if (urlTail.contains("&sig")) { // Auth Url
            Log.d(TAG, "Auth RC");
            HashMap<String, String> expectedQueryParams = new HashMap<>();
            expectedQueryParams.put("id", "uid");
            expectedQueryParams.put("num", "tag_version");
            expectedQueryParams.put("sig", "vid");
            params = handleAuthOrHidUrl(urlParts, expectedQueryParams);
        }
        else if (urlTail.contains("&tac")) { // HID URL
            Log.d(TAG, "HID RC");
            HashMap<String, String> expectedQueryParams = new HashMap<>();
            expectedQueryParams.put("tagID", "hid");
            expectedQueryParams.put("tac", "vid");
            params = handleAuthOrHidUrl(urlParts, expectedQueryParams);
        }
        else {
            Log.d(TAG, "NO AUTH");
            mDelegate.interactionDidFail(ERROR_NON_AUTH_URL + url);
            return;
        }

        RequestParams requestParams = new RequestParams(params);
        registerInteraction(mTagId, requestParams);
    }

    /**
     * Handles the actual POSTing to the verification route.
     * @param mTagId ID in base 10 of the tapped tag.
     * @param params Request parameters parsed from the Interaction URL.
     */
    protected void registerInteraction(String mTagId, RequestParams params) {
        Log.d(TAG, "[registerInteraction]mTagId: " + mTagId + " params: " + params.toString());

        params.put("tag_id", mTagId);
        params.put("tech", "n");

        Log.d(TAG, "register params: " + params.toString());

        String targetUrl = "https://api.mtag.io/v2/interactions";

        JsonHttpResponseHandler responseHandler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d(TAG, "Interactions response: " + response);
                JSONObject parsedResponse = handleResponse(response);
                mDelegate.interactionDataWasReceived(parsedResponse);
            }

            // TODO: clean up error handling in these responses
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Log.e(TAG, "DID FAIL with status code: " + statusCode + " And cause " + errorResponse.toString());
                mDelegate.interactionDidFail(errorResponse.toString());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.e(TAG, "DID FAIL with status code: " + statusCode + " And cause " + responseString);
                mDelegate.interactionDidFail(responseString);
            }
        };

        client.post(targetUrl, params, responseHandler);
    }

    /**
     * Receives the valid response JSON body and parses out the relevant fields.  Fields that are
     * not found in the response body, or are null, are omitted.  A null tag_verified field is
     * treated as a failure, but should be shown to the user as a potential service hiccup and not
     * a true authentication failure.
     * @param response JSON body from the registerInteraction call.
     * @return Formatted JSON response passed to the BlueBiteInteractionDelegate.
     */
    protected JSONObject handleResponse(JSONObject response) {
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
            formattedResponse.put("tagVerified", response.getBoolean("tag_verified"));
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
     * Parses an Auth or HID URL into a Map.
     * @param urlParts The Interaction URL split on "/" for easier parsing.
     * @param expectedQueryParams A Map relating the authentication API's expected param names
     *                            and their URL query parameter key names.
     * @return A Map of the API's expected param names with the value of their corresponding
     * query parameter names.
     */
    private HashMap<String,String> handleAuthOrHidUrl(
            String[] urlParts, HashMap<String, String> expectedQueryParams) {
        Log.d(TAG, "handleAuthUrl" + Arrays.toString(urlParts));
        HashMap<String, String> params = new HashMap<>();

        //grab url query parameters
        String[] urlQueryParams = urlParts[urlParts.length - 1].split("\\?")[1].split("&");
        Log.d(TAG, "AuthUrl urlQueryParams: " + Arrays.toString(urlQueryParams));


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
     * @param urlParts The Interaction URL split on "/" for easier parsing.
     * @return A Map of the API's expected param names with the value of their corresponding
     * query parameter names.
     */
    private HashMap<String,String> handleCounterUrl(String[] urlParts) {
        Log.d(TAG, "handleCounterUrl" + Arrays.toString(urlParts));
        String urlTail = urlParts[urlParts.length - 1];
        // trim any extra query params
        String fullVID = urlTail.split("\\?")[0];
        HashMap<String, String> params = new HashMap<>();
        params.put("vid", fullVID);

        return params;
    }

    /**
     * Takes the Interaction URL and attempts to parse the mTag ID (base 36 or base 10) from it.
     * If it finds the ID, attempts to convert it to base 10.
     * @param url Interaction URL to parse.
     * @return Base 10 mTag URL as a String.
     */
    protected String parseMTagId(String url) {
        String[] urlParts = url.split("/");
        String urlTail = urlParts[urlParts.length - 1];
        String idAndTechType;

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
        } else {  // it isn't a verifiable tag
            return "";
        }
        String mTagId = idAndTechType.substring(1);
        return convertIdToBase10(mTagId);
    }

    /**
     * Accepts the mTag ID discovered by parseMtagID and attempts to convert it to base 10.
     * @param mTagId Base 36 or base 10 mTag ID.
     * @return Base 10 mTag ID as String.
     */
    protected String convertIdToBase10(String mTagId) {
        // assume its b10 even though it probably isn't
        int idAsBase10;

        try {
            idAsBase10 = Integer.parseInt(mTagId);
        } catch (NumberFormatException e) {
            // is base36 like we expected
            idAsBase10 = Integer.parseInt(mTagId, 36);
        }
        return "" + idAsBase10;
    }
}
