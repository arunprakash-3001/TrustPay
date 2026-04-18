package com.example.trustpay.ui.verification;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trustpay.R;
import com.example.trustpay.ui.liveness.LivenessActivity;
import com.google.android.material.button.MaterialButton;

public class VerificationActivity extends AppCompatActivity {

    MaterialButton btnVerify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        btnVerify = findViewById(R.id.btnVerify);

        btnVerify.setOnClickListener(v -> {
            String senderUpi = getIntent().getStringExtra("sender_upi");
            String receiverUpi = getIntent().getStringExtra("receiver_upi");
            String amount = getIntent().getStringExtra("amount");

            if (senderUpi == null || senderUpi.trim().isEmpty()) {
                Toast.makeText(
                        VerificationActivity.this,
                        "Missing transaction details for face verification",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Intent intent = new Intent(VerificationActivity.this, LivenessActivity.class);
            intent.putExtra("sender_upi", senderUpi);
            intent.putExtra("receiver_upi", receiverUpi);
            intent.putExtra("amount", amount);
            startActivity(intent);
            finish();
        });
    }
}
