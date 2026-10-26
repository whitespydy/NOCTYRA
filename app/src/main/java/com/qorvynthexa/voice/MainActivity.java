package com.qorvynthexa.voice;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.AlarmClock;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final int MIC_PERMISSION = 10;
    private static final int SPEECH_REQUEST = 11;

    private TextView status;
    private TextView transcript;
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
        title.setText("QORVYNTHEXA");
        title.setTextSize(30);
        title.setTextColor(0xFF75D9FF);
        title.setGravity(Gravity.CENTER);
        layout.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 100));

        status = new TextView(this);
        status.setText("Starting assistant...");
        status.setTextColor(0xFFFFFFFF);
        status.setTextSize(16);
        layout.addView(status);

        transcript = new TextView(this);
        transcript.setTextColor(0xFFE1E7F0);
        transcript.setTextSize(18);
        transcript.setPadding(0, 30, 0, 30);
        layout.addView(transcript, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        Button talkButton = new Button(this);
        talkButton.setText("TALK TO QORVYNTHEXA");
        layout.addView(talkButton);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);

        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                int language = tts.setLanguage(Locale.UK);
                ttsReady = language != TextToSpeech.LANG_MISSING_DATA
                        && language != TextToSpeech.LANG_NOT_SUPPORTED;
                status.setText(ttsReady
                        ? "Ready. Tap and give a command."
                        : "English speech output unavailable.");
            } else {
                status.setText("Speech output could not start.");
            }
        });

        talkButton.setOnClickListener(v -> startListening());
    }

    private void startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText("Speech recognition is unavailable.");
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
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
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
                transcript.setText("You: " + spoken);
                processCommand(spoken);
            } else {
                respond("I didn't catch that. Please try again.");
            }
        } else {
            status.setText("Speech cancelled or not recognised.");
        }
    }

    private void processCommand(String spoken) {
        String command = spoken.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}:\\s]", " ")
                .trim()
                .replaceAll("\\s+", " ");

        if (command.startsWith("please ")) {
            command = command.substring(7).trim();
        }
        if (command.startsWith("open ")) {
            command = command.substring(5).trim();
        }
        if (command.startsWith("launch ")) {
            command = command.substring(7).trim();
        }
        if (command.startsWith("start ")) {
            command = command.substring(6).trim();
        }


        Matcher alarm = Pattern.compile(
                "(?:set|create|make|schedule)\\s+(?:an?\\s+)?alarm"
                + "(?:\\s+for)?\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b",
                Pattern.CASE_INSENSITIVE).matcher(command);

        if (alarm.matches()) {
            int hour = Integer.parseInt(alarm.group(1));
            int minute = alarm.group(2) == null
                    ? 0 : Integer.parseInt(alarm.group(2));
            String period = alarm.group(3).toLowerCase(Locale.ROOT);

            if (hour < 1 || hour > 12 || minute > 59) {
                respond("That alarm time is invalid. Please try again.");
                return;
            }

            int hour24 = (hour % 12) + (period.equals("pm") ? 12 : 0);
            Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
            alarmIntent.putExtra(AlarmClock.EXTRA_HOUR, hour24);
            alarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, minute);
            alarmIntent.putExtra(AlarmClock.EXTRA_MESSAGE, "QORVYNTHEXA");

            if (alarmIntent.resolveActivity(getPackageManager()) == null) {
                respond("No compatible alarm app is available.");
                return;
            }

            try {
                startActivity(alarmIntent);
                respond("Opening the alarm screen for "
                        + alarm.group(1) + ":"
                        + String.format(Locale.ROOT, "%02d", minute)
                        + " " + period.toUpperCase(Locale.ROOT)
                        + ". Confirm the alarm on your phone.");
            } catch (Exception e) {
                respond("Android could not open the alarm screen.");
            }
            return;
        }

        switch (command) {
            case "chrome":
                openApp("com.android.chrome", "Chrome");
                return;
            case "whatsapp":
                openApp("com.whatsapp", "WhatsApp");
                return;
            case "youtube":
                openApp("com.google.android.youtube", "YouTube");
                return;
            case "clock":
            case "clock app":
                openApp("com.google.android.deskclock", "Clock");
                return;
            case "settings":
            case "phone settings":
                launchIntent(new Intent(
                        android.provider.Settings.ACTION_SETTINGS),
                        "Settings");
                return;
            case "camera":
                launchIntent(new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                        "Camera");
                return;
            case "phone":
            case "dialer":
                launchIntent(new Intent(Intent.ACTION_DIAL), "Phone dialer");
                return;
            case "messages":
            case "sms":
                launchIntent(new Intent(Intent.ACTION_SENDTO,
                        Uri.parse("smsto:")), "Messages");
                return;
            case "browser":
            case "internet":
            case "google":
                launchIntent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com")), "Browser");
                return;
            case "help":
            case "what can you do":
                respond("I can open Chrome, WhatsApp, YouTube, Settings, "
                        + "Camera, Clock, Phone, Messages, and your browser. "
                        + "More commands will be added.");
                return;
            case "stop":
            case "cancel":
                respond("Okay. No further action taken.");
                return;
            default:
                respond("I don't have that command yet. Say help to hear "
                        + "the commands I support.");
        }
    }

    private void openApp(String packageName, String label) {
        Intent intent = getPackageManager()
                .getLaunchIntentForPackage(packageName);

        if (intent == null) {
            respond(label + " is not available or could not be opened.");
            return;
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
            respond("Opening " + label + ".");
        } catch (Exception e) {
            respond("Android could not open " + label + ".");
        }
    }

    private void launchIntent(Intent intent, String label) {
        if (intent.resolveActivity(getPackageManager()) == null) {
            respond("No available app can handle " + label + ".");
            return;
        }

        try {
            startActivity(intent);
            respond("Opening " + label + ".");
        } catch (Exception e) {
            respond("Android could not open " + label + ".");
        }
    }

    private void respond(String message) {
        status.setText("Command processed.");
        String previous = transcript.getText().toString();
        transcript.setText(previous + "\n\nQORVYNTHEXA: " + message);

        if (ttsReady && tts != null) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH,
                    null, "qorvynthexa-response");
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == MIC_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else if (requestCode == MIC_PERMISSION) {
            status.setText("Microphone permission is required.");
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
