package com.mailbox.notice;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.mailbox.notice.R;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResultsActivity extends AppCompatActivity {

    private TextView textViewResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        textViewResult = findViewById(R.id.textViewResult);

        // Get API key and channel ID from intent
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            String apiKey = extras.getString("apiKey");
            String channelId = extras.getString("channelId");
            // Connect to ThingSpeak
            connectToThingSpeak(apiKey, channelId);
        } else {
            // Handle case where extras are null
            Toast.makeText(this, "API key and channel ID not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToThingSpeak(String apiKey, String channelId) {
        String urlString = "https://api.thingspeak.com/channels/" + channelId + "/feeds/last.json?api_key=" + apiKey;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            String response = null;
            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    in.close();
                    response = responseBuilder.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            String finalResponse = response;
            handler.post(() -> {
                if (finalResponse != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(finalResponse);
                        String field1Value = jsonObject.getString("field1");
                        if (field1Value.equals("1")) {
                            // Field 1 indicates the mailbox was open
                            String createdTime = jsonObject.getString("created_at");
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                            Date dateTime = sdf.parse(createdTime);
                            SimpleDateFormat displayFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                            String formattedDateTime = displayFormat.format(dateTime);
                            String result = "The mailbox was open on: " + formattedDateTime;
                            textViewResult.setText(result);
                        } else {
                            textViewResult.setText("The mailbox was not open.");
                        }
                    } catch (JSONException | ParseException e) {
                        e.printStackTrace();
                        Toast.makeText(ResultsActivity.this, "Failed to parse ThingSpeak response", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ResultsActivity.this, "Failed to fetch data from ThingSpeak", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
