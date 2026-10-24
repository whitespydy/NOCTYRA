package com.noctyra.voice;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int MIC_PERMISSION = 10;
    private static final int SPEECH_REQUEST = 11;

    private TextView status;
    private TextView transcript;
    private Button talkButton;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(28, 48, 28, 28);
        layout.setBackgroundColor(0xFF10131C);

        TextView title = new TextView(this);
        title.setText("NOCTYRA");
        title.setTextSize(32);
        title.setTextColor(0xFF75D9FF);
        title.setGravity(Gravity.CENTER);
        layout.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 100));

        status = new TextView(this);
        status.setText("Your assistant is starting...");
        status.setTextColor(0xFFFFFFFF);
        status.setTextSize(16);
        layout.addView(status);

        transcript = new TextView(this);
        transcript.setTextColor(0xFFE1E7F0);
        transcript.setTextSize(18);
        transcript.setPadding(0, 30, 0, 30);
        layout.addView(transcript, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        talkButton = new Button(this);
        talkButton.setText("TALK TO NOCTYRA");
        layout.addView(talkButton);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);

        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                int language = tts.setLanguage(Locale.UK);
                ttsReady = language != TextToSpeech.LANG_MISSING_DATA
                        && language != TextToSpeech.LANG_NOT_SUPPORTED;
                if (ttsReady) {
                    status.setText("Ready. Tap the button and speak.");
                } else {
                    status.setText("English voice unavailable on this device.");
                }
            } else {
                status.setText("Speech output could not start.");
            }
        });

        talkButton.setOnClickListener(v -> startListening());
    }

    private void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText("Android speech recognition is unavailable.");
            return;
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MIC_PERMISSION);
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-GB");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        status.setText("Listening...");
        startActivityForResult(intent, SPEECH_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != SPEECH_REQUEST) return;

        if (resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);

            if (results != null && !results.isEmpty()) {
                String spoken = results.get(0);
                transcript.setText("You: " + spoken
                        + "\n\nNOCTYRA: I heard you. My AI conversation engine "
                        + "is not connected yet.");
                status.setText("Speech recognised.");

                if (ttsReady) {
                    tts.speak(
                            "I heard you. My conversation engine is not connected yet.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "noctyra-response");
                }
            } else {
                status.setText("No speech was recognised. Try again.");
            }
        } else {
            status.setText("Speech input cancelled or not recognised.");
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MIC_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else if (requestCode == MIC_PERMISSION) {
            status.setText("Microphone permission is required to talk.");
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
