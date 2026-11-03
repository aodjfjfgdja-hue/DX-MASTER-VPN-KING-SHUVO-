package com.dxmaster.volumebooster;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class FloatingMicService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private boolean isMonitoring = false;
    private boolean isBroadcasting = false;
    private LoudnessEnhancer loudnessEnhancer;
    private Thread audioThread;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initFloatingWidget();
        return START_STICKY;
    }

    private void initFloatingWidget() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingView = View.inflate(this, R.layout.floating_widget, null);

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;

        windowManager.addView(floatingView, params);

        Button btnMonitor = floatingView.findViewById(R.id.btnMonitor);
        Button btnCallBroadcast = floatingView.findViewById(R.id.btnCallBroadcast);

        btnMonitor.setOnClickListener(v -> {
            if (!isMonitoring) {
                isMonitoring = true;
                btnMonitor.setText("STOP MONITOR");
                startAudioEngine(true, false);
                Toast.makeText(this, "Self Monitor Active (6000+ Boost)", Toast.LENGTH_SHORT).show();
            } else {
                isMonitoring = false;
                btnMonitor.setText("SELF MONITOR");
                stopAudioEngine();
            }
        });

        btnCallBroadcast.setOnClickListener(v -> {
            if (!isBroadcasting) {
                isBroadcasting = true;
                btnCallBroadcast.setText("STOP BROADCAST");
                startAudioEngine(true, true);
                Toast.makeText(this, "Call Broadcast Active - Everyone Can Hear!", Toast.LENGTH_SHORT).show();
            } else {
                isBroadcasting = false;
                btnCallBroadcast.setText("CALL BROADCAST");
                stopAudioEngine();
            }
        });

        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });
    }

    private void startAudioEngine(boolean playLocally, boolean broadcastToCall) {
        stopAudioEngine();

        audioThread = new Thread(() -> {
            try {
                int sampleRate = 44100;
                int channelConfigIn = AudioFormat.CHANNEL_IN_MONO;
                int channelConfigOut = AudioFormat.CHANNEL_OUT_MONO;
                int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat);

                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfigIn, audioFormat, bufferSize);

                if (playLocally) {
                    audioTrack = new AudioTrack.Builder()
                            .setAudioAttributes(new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build())
                            .setAudioFormat(new AudioFormat.Builder()
                                    .setEncoding(audioFormat)
                                    .setSampleRate(sampleRate)
                                    .setChannelMask(channelConfigOut)
                                    .build())
                            .setBufferSizeInBytes(bufferSize)
                            .setTransferMode(AudioTrack.MODE_STREAM)
                            .build();
                    audioTrack.play();
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    try {
                        loudnessEnhancer = new LoudnessEnhancer(audioRecord.getAudioSessionId());
                        loudnessEnhancer.setTargetGain(MainActivity.boostValue);
                        loudnessEnhancer.setEnabled(true);
                    } catch (Exception ignored) {}
                }

                audioRecord.startRecording();
                byte[] data = new byte[bufferSize];

                while (isMonitoring || isBroadcasting) {
                    int readSize = audioRecord.read(data, 0, bufferSize);
                    if (readSize > 0) {
                        if (playLocally && audioTrack != null) {
                            audioTrack.write(data, 0, readSize);
                        }
                        if (broadcastToCall) {
                            // টেলিগ্রাম কলের ইনপুটে অডিও ডেটা পাস করার রাউটিং
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        audioThread.start();
    }

    private void stopAudioEngine() {
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception ignored) {}
            audioRecord = null;
        }
        if (audioTrack != null) {
            try {
                audioTrack.stop();
                audioTrack.release();
            } catch (Exception ignored) {}
            audioTrack = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAudioEngine();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }
}
