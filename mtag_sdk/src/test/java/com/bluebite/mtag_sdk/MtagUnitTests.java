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

import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Unit tests verify our URL parsing, request body, and response handling are all working
 * as expected.
 */
public class MtagUnitTests {

    private MockActivity activity;
    private MockAPI api;

    private HashMap<String, String> expectedParams;
    private String expectedMtagId = "32403784";

    /**
     *
     */
    public class MockActivity implements BlueBiteInteractionDelegate {
        private API api;

        public MockActivity() {
            this.api = new API(this);
        }

        @Override
        public void interactionDataWasReceived(JSONObject results) {
            try {
                assertTrue(results.getBoolean("tagVerified"));
            } catch (JSONException e) {
                throw new RuntimeException("tagVerified was false");
            }
        }

        @Override
        public void interactionDidFail(String error) {
        }
    }

    /**
     * Creates a bare-bones mock response object.  Could be improved using an external JSON file
     * but ultimately all the excess fields aren't even checked so it isn't really necessary.
     * @return Mock successful response body.
     */
    private JSONObject mockResponse() {
        try {
            JSONObject response = new JSONObject();

            JSONObject device = new JSONObject();
            device.put("r", false);
            device.put("rd", true);
            device.put("country", "US");
            response.put("device", device);

            response.put("tag_verified", "true");

            response.put("impression_ids", null);
            response.put("track_location", true);
            response.put("payloads", null);
            response.put("rk", "abcdefghijklm12345");

            JSONObject location = new JSONObject();
            location.put("some stuff", "in here");
            response.put("location", location);

            JSONArray campaigns = new JSONArray();
            response.put("campaigns", campaigns);

            return response;
        } catch (JSONException e) {
            throw new RuntimeException("JSON mock response broke");
        }
    }

    /**
     * Override registerInteraction() so we can assert that the request body is properly
     * formatted without spamming prod with requests.
     */
    public class MockAPI extends API {

        public MockAPI(BlueBiteInteractionDelegate mDelegate) {
            super(mDelegate);
        }

        @Override
        protected void registerInteraction(String mTagId, RequestParams params) {
            assertEquals(mTagId, expectedMtagId);

            for (String key : expectedParams.keySet()) {
                assertTrue(params.has(key));
            }

            assertEquals(expectedParams, convertToHashMap(params));
        }
    }

    /**
     * Helper converts a RequestParams object to a more easily comparable
     * HashMap.
     * @param params RequestParams to convert.
     * @return HashMap.
     */
    private HashMap<String, String> convertToHashMap(RequestParams params) {
        String paramsAsString = params.toString();
        String[] split = paramsAsString.split("&");
        HashMap<String, String> paramsAsMap = new HashMap<>();
        for (String param : split) {
            String[] split_param = param.split("=");
            paramsAsMap.put(split_param[0], split_param[1]);
        }
        return paramsAsMap;
    }

    @Before
    public void setUp() {
        this.activity = new MockActivity();
        this.api = new MockAPI(activity);
    }

    /**
     * A url passed to parseMtagId should:
     *  Return the base 10 mTag ID from the Url.
     *  Return an empty string if there is no mTag ID in the URL.
     */
    @Test
    public void testParseMtagId() throws Exception {
        // base 36 basic url
        String base36Url = "https://mtag.io/njaix4";
        String mTagAsb10 = api.parseMTagId(base36Url);
        assertEquals(mTagAsb10, expectedMtagId);

        // base 10 basic url
        String base10Url = "https://mtag.io/n32403784";
        mTagAsb10 = api.parseMTagId(base10Url);
        assertEquals(mTagAsb10, expectedMtagId);

        // bad url
        String badUrl = "https://google.com";
        String badResponse = api.parseMTagId(badUrl);
        assertEquals(badResponse, "");
    }

    /**
     * Calls to interactionWasReceived should:
     * - parse the proper fields from the URL.
     * @throws Exception
     */
    @Test
    public void testRegisterInteraction() throws Exception {
        // smt counter url
        String counterUrl = "https://mtag.io/njaix4/0123456789x0002C42702";
        expectedParams = new HashMap<>();
        expectedParams.put("vid", "0123456789x0002C42702");
        api.interactionWasReceived(counterUrl);

        // auth url
        String rollingCodeUrl = "https://mtag.io/njaix4?id=12345678&sig=00000F1234567678";
        expectedParams = new HashMap<>();
        expectedParams.put("vid", "00000F1234567678");
        expectedParams.put("uid", "12345678");
        api.interactionWasReceived(rollingCodeUrl);

        // auth url with version
        String rollingCodeUrlWithNum = "https://mtag.io/njaix4?id=12345678&num=8675309&sig=00000F1234567678";
        expectedParams.put("tag_version", "8675309");
        api.interactionWasReceived(rollingCodeUrlWithNum);

        // hid url
        String hidUrl = "https://mtag.io/njaix4?tagID=12345678&tac=7C3CC5B3FEDD48EE2DA327DD";
        expectedParams = new HashMap<>();
        expectedParams.put("vid", "7C3CC5B3FEDD48EE2DA327DD");
        expectedParams.put("hid", "12345678");
        api.interactionWasReceived(hidUrl);
    }

    /**
     * Calls to parseResponse should:
     * - Populate a JSONObject with all available target fields.
     * @throws Exception
     */
    @Test
    public void testResponseParser() throws Exception {
        // good response
        JSONObject res = api.handleResponse(mockResponse());
        activity.interactionDataWasReceived(res);

        // bad/partial responses
        JSONObject partialRes = new JSONObject();
        JSONObject expectedRes = new JSONObject();

        // missing device country
        res = api.handleResponse(partialRes);
        assertEquals(res.toString(), partialRes.toString());

        // missing tagVerified
        partialRes.put("device", (new JSONObject()).put("country", "US"));
        expectedRes.put("deviceCountry", "US");
        res = api.handleResponse(partialRes);
        assertEquals(res.toString(), expectedRes.toString());

        // missing location
        partialRes.put("tag_verified", "true");
        expectedRes.put("tagVerified", "true");
        res = api.handleResponse(partialRes);
        assertEquals(res.toString(), expectedRes.toString());

        // missing campaigns
        partialRes.put("location", (new JSONObject()).put("whatever", ""));
        expectedRes.put("location", new JSONObject().put("whatever", ""));
        res = api.handleResponse(partialRes);
        assertEquals(res.toString(), expectedRes.toString());
    }
}