package com.bluebite.mtagsdk;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.bluebite.mtag_sdk.API;
import com.bluebite.mtag_sdk.InteractionDelegate;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements InteractionDelegate {
    public static final String TAG = MainActivity.class.getSimpleName();

    TextView textView;
    Button basicTagButton;
    Button rollingTagButton;

    private API api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        api = new API(this, this);

        textView = findViewById(R.id.textView);
        basicTagButton = findViewById(R.id.basicTagButton);
        rollingTagButton = findViewById(R.id.rollingKeyButton);

        basicTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String basicUrl = "https://mtag.io/njaix4";
                api.interactionWasReceived(basicUrl);
            }
        });
        rollingTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String rollingExampleUrl = "https://mtag.io/nilpps/042E276AA24881x0001A22702";
                api.interactionWasReceived(rollingExampleUrl);
            }
        });
    }

    @Override
    public void interactionDataWasReceived(JSONObject results) {
        Log.i(TAG, "Interaction received: " + results);
        textView.setText("" + results);
    }

    @Override
    public void interactionDidFail(String error) {
        Log.i(TAG, "Interaction did fail: " + error);
        textView.setText("" + error);
    }
}
