package com.bluebite.mtag_sdk;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    MockActivity activity;

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

    @Before
    public void setUp() throws Exception {
        this.activity = new MockActivity();
    }

    /*
    * A url with a base36 mTag id should:
    *   Be successfully parsed from the url and converted to base 10.
    * */
    @Test
    public void testBase36Id() throws Exception {
        String base36Url = "https://mtag.io/njaix4";

    }
}