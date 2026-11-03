package com.dxmaster.volumebooster;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etPassword;
    private Button btnLogin, btnStartMic;
    private LinearLayout layoutLogin, layoutControl;
    private SeekBar seekBarBoost;
    public static int boostValue = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnStartMic = findViewById(R.id.btnStartMic);
        layoutLogin = findViewById(R.id.layoutLogin);
        layoutControl = findViewById(R.id.layoutControl);
        seekBarBoost = findViewById(R.id.seekBarBoost);

        btnLogin.setOnClickListener(v -> {
            String pass = etPassword.getText().toString().trim();
            if (pass.equals("DX MASTER")) {
                layoutLogin.setVisibility(View.GONE);
                layoutControl.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Access Granted - DX MASTER", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Wrong Password! Use: DX MASTER", Toast.LENGTH_SHORT).show();
            }
        });

        btnStartMic.setOnClickListener(v -> {
            boostValue = seekBarBoost.getProgress();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 1234);
            } else {
                startFloatingService();
            }
        });
    }

    private void startFloatingService() {
        Intent serviceIntent = new Intent(this, FloatingMicService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Toast.makeText(this, "Floating Mic Activated!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
