package com.bluebite.mtag_sdk;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.RequestParams;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.message.BasicNameValuePair;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    MockActivity activity;
    MockAPI api;

    HashMap<String, String> expectedParams;
    String expectedMtagId = "32403784";

    public class MockActivity implements InteractionDelegate {
        private API api;

        public MockActivity() {
            this.api = new API(this);
        }

        @Override
        public void interactionDataWasReceived(JSONObject results) {
        }

        @Override
        public void interactionDidFail(String error) {
        }
    }

    public class MockAPI extends API {

        public MockAPI(InteractionDelegate mDelegate) {
            super(mDelegate);
        }

        @Override
        protected void registerInteraction(String mTagId, RequestParams params) {
            assertEquals(mTagId, expectedMtagId);

            for (String key : expectedParams.keySet()) {
                assertTrue(params.has(key));
                System.out.println("params to string: " + params.toString());
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

    @Test
    public void testRegisterInteraction() throws Exception {
        // smt counter url
        // TODO: spoof these url values
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
}