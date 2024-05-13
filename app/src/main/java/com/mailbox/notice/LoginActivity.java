package com.mailbox.notice;

import android.content.Intent;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText editTextCode;
    private Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextCode = findViewById(R.id.editTextCode);
        buttonLogin = findViewById(R.id.buttonLogin);

        // Check if there's already a saved code
        String savedCode = getSavedCode();
        if (!savedCode.isEmpty()) {
            // If there's a saved code, populate the EditText with it
            editTextCode.setText(savedCode);
        }

       setonclick();

    }

    private String getSavedCode() {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        return sharedPreferences.getString("code", "");
    }
    public void setonclick(){
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Hello","hiew");
                String code = editTextCode.getText().toString().trim();
                verifyCode(code);
            }
        });
    }

    private void saveCode(String code) {
        try {
            Log.d(TAG, "Saving code: " + code);
            SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("code", code);
            editor.apply();
            Log.d(TAG, "Code saved successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error saving code: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void verifyCode(String code) {
        Log.d(TAG, "Verifying code: " + code);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("mailboxes").document(code)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String apiKey = document.getString("apiKey");
                            String channelId = document.getString("channelId");
                            if (apiKey != null && channelId != null) {
                                // Save code only if verified successfully
                                saveCode(code);
                                Log.d(TAG, "Code verified successfully. Saving code.");
                                // Pass API key and channel ID to ResultsActivity
                                Intent intent = new Intent(LoginActivity.this, ResultsActivity.class);
                                intent.putExtra("apiKey", apiKey);
                                intent.putExtra("channelId", channelId);
                                startActivity(intent);
                            } else {
                                Toast.makeText(LoginActivity.this, "Invalid code", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Invalid code", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Failed to verify code", task.getException());
                        Toast.makeText(LoginActivity.this, "Failed to verify code", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
