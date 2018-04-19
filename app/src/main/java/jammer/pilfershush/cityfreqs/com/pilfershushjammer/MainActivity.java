package jammer.pilfershush.cityfreqs.com.pilfershushjammer;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "PilferShush_Jammer";
    public static final String VERSION = "1.0.01";

    private PendingIntent permissionIntent;
    private static final int REQUEST_AUDIO_PERMISSION = 1;

    private static TextView debugText;
    private Button passiveJammerButton;
    private Button activeJammerButton;

    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusListener;
    private HeadsetIntentReceiver headsetReceiver;

    private AudioSettings audioSettings;
    private AudioChecker audioChecker;
    private PassiveJammer passiveJammer;
    private boolean PASSIVE;
    //private boolean ACTIVE;
    public static String START_PASSIVE_ACTION = "jammer.pilfershush.cityfreqs.com.pilfershushjammer.PSJammerService.action.startpassive";
    public static String STOP_PASSIVE_ACTION = "jammer.pilfershush.cityfreqs.com.pilfershushjammer.PSJammerService.action.stoppassive";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //
        debugText = (TextView) findViewById(R.id.debug_text);
        debugText.setTextColor(Color.parseColor("#00ff00"));
        debugText.setMovementMethod(new ScrollingMovementMethod());
        debugText.setOnClickListener(new TextView.OnClickListener() {
            @Override
            public void onClick(View v) {
                debugText.setGravity(Gravity.NO_GRAVITY);
                debugText.setSoundEffectsEnabled(false); // no further click sounds
            }
        });

        passiveJammerButton = (Button) findViewById(R.id.run_passive_button);
        passiveJammerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                togglePassiveJamming();
            }
        });
        activeJammerButton = (Button) findViewById(R.id.run_active_button);
        activeJammerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //TODO not yet implemented
                //toggleActiveJamming();
            }
        });

        headsetReceiver = new HeadsetIntentReceiver();

        // permissions ask:
        // check API version, above 23 permissions are asked at runtime
        // if API version < 23 (6.x) fallback is manifest.xml file permission declares
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.LOLLIPOP) {

            List<String> permissionsNeeded = new ArrayList<String>();
            final List<String> permissionsList = new ArrayList<String>();

            if (!addPermission(permissionsList, Manifest.permission.RECORD_AUDIO))
                permissionsNeeded.add(getResources().getString(R.string.perms_state_1));

            if (permissionsList.size() > 0) {
                if (permissionsNeeded.size() > 0) {
                    // Need Rationale
                    String message = getResources().getString(R.string.perms_state_2) + permissionsNeeded.get(0);
                    for (int i = 1; i < permissionsNeeded.size(); i++) {
                        message = message + ", " + permissionsNeeded.get(i);
                    }
                    showPermissionsDialog(message,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            permissionsList.toArray(new String[permissionsList.size()]),
                                            REQUEST_AUDIO_PERMISSION);
                                }
                            });
                    return;
                }
                ActivityCompat.requestPermissions(this,
                        permissionsList.toArray(new String[permissionsList.size()]),
                        REQUEST_AUDIO_PERMISSION);
                return;
            }
            else {
                // assume already runonce, has permissions
                initApplication();
            }

        }
        else {
            // pre API 23
            initApplication();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(headsetReceiver, filter);
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        // refocus app
        toggleHeadset(false); // default state at init
        audioFocusCheck();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // backgrounded, stop recording, possible audio_focus loss due to telephony...
        unregisterReceiver(headsetReceiver);
        interruptRequestAudio();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*

        MAIN APPLICATION FUNCTIONS

    */
    private void showPermissionsDialog(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton(getResources().getString(R.string.dialog_button_okay), okListener)
                .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), null)
                .create()
                .show();
    }

    private boolean addPermission(List<String> permissionsList, String permission) {
        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission))
                return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSION: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++) {
                    perms.put(permissions[i], grantResults[i]);
                }
                // Check for RECORD_AUDIO
                if (perms.get(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted
                    initApplication();
                }
                else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.perms_state_3), Toast.LENGTH_SHORT)
                            .show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private boolean initApplication() {
        entryLogger(getResources().getString(R.string.init_state_1) + VERSION, false);
        headsetReceiver = new HeadsetIntentReceiver();
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        toggleHeadset(false); // default state at init

        initAudioFocusListener();

        audioSettings = new AudioSettings();
        audioChecker = new AudioChecker(this, audioSettings);
        if (audioChecker.determineInternalAudioType() == false) {
            // have a setup error getting the audio
            return false;
        }

        passiveJammer = new PassiveJammer(this, audioSettings);

        return true;
    }

    private void togglePassiveJamming() {
        if (PASSIVE) {
            PASSIVE = false;
            if (passiveJammer != null) {
                passiveJammer.stopPassiveJammer();
                passiveJammerButton.setBackgroundColor(Color.LTGRAY);
                entryLogger(getResources().getString(R.string.main_scanner_4), false);
            }
        }
        else {
            PASSIVE = true;
            if (passiveJammer != null) {
                if (passiveJammer.startPassiveJammer()) {
                    passiveJammer.runPassiveJammer();
                    passiveJammerButton.setBackgroundColor(Color.RED);
                    entryLogger(getResources().getString(R.string.main_scanner_3), false);
                }
            }
        }
    }


    /*

        UTILITY FUNCTIONS

    */
    private void interruptRequestAudio() {
        // possible system app request audio focus, respond here
        entryLogger(getResources().getString(R.string.audiofocus_check_5), true);
        /*
        if (SCANNING) {
            toggleScanning();
        }
        */
    }

    private void audioFocusCheck() {
        // this may not work as SDKs requesting focus may not get it cos we already have it?
        // also: getting MIC access does not require getting AUDIO_FOCUS
        entryLogger(getResources().getString(R.string.audiofocus_check_1), false);
        int result = audioManager.requestAudioFocus(audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            entryLogger(getResources().getString(R.string.audiofocus_check_2), false);
        }
        else if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            entryLogger(getResources().getString(R.string.audiofocus_check_3), false);
        }
        else {
            entryLogger(getResources().getString(R.string.audiofocus_check_4), false);
        }
    }

    private void toggleHeadset(boolean hasHeadset) {
        // if no headset, mute the audio output
        if (hasHeadset) {
            // volume to 50%
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2,
                    AudioManager.FLAG_SHOW_UI);
        }
        else {
            // volume to 0
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    0,
                    AudioManager.FLAG_SHOW_UI);
        }
    }

    private void initAudioFocusListener() {
        audioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch(focusChange) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                        // -1
                        // loss for unknown duration
                        entryLogger(getResources().getString(R.string.audiofocus_1), false);
                        audioManager.abandonAudioFocus(audioFocusListener);
                        interruptRequestAudio();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        // -2
                        // temporary loss ? API docs says a "transient loss"!
                        entryLogger(getResources().getString(R.string.audiofocus_2), false);
                        interruptRequestAudio();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        // -3
                        // loss to other audio source, this can duck for the short duration if it wants
                        entryLogger(getResources().getString(R.string.audiofocus_3), false);
                        interruptRequestAudio();
                        break;
                    case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                        // 0
                        // failed focus change request
                        entryLogger(getResources().getString(R.string.audiofocus_4), false);
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        //case AudioManager.AUDIOFOCUS_REQUEST_GRANTED: <- duplicate int value...
                        // 1
                        // has gain, or request for gain, of unknown duration
                        entryLogger(getResources().getString(R.string.audiofocus_5), false);
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                        // 2
                        // temporary gain or request for gain, for short duration (ie. notification)
                        entryLogger(getResources().getString(R.string.audiofocus_6), false);
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                        // 3
                        // as above but with other background audio ducked for duration
                        entryLogger(getResources().getString(R.string.audiofocus_7), false);
                        break;
                    default:
                        //
                        entryLogger(getResources().getString(R.string.audiofocus_8), false);
                }
            }
        };
    }

    private class HeadsetIntentReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        entryLogger(getResources().getString(R.string.headset_state_1), false);
                        toggleHeadset(false);
                        break;
                    case 1:
                        entryLogger(getResources().getString(R.string.headset_state_2), false);
                        toggleHeadset(true);
                        break;
                    default:
                        entryLogger(getResources().getString(R.string.headset_state_3), false);
                }
            }
        }
    }


    /*


    */
    protected static void entryLogger(String entry, boolean caution) {
        int start = debugText.getText().length();
        debugText.append("\n" + entry);
        int end = debugText.getText().length();
        Spannable spannableText = (Spannable) debugText.getText();
        if (caution) {
            spannableText.setSpan(new ForegroundColorSpan(Color.YELLOW), start, end, 0);
        }
        else {
            spannableText.setSpan(new ForegroundColorSpan(Color.GREEN), start, end, 0);
        }
    }
}