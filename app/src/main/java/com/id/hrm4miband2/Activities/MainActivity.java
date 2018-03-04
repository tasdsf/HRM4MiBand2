package com.id.hrm4miband2.Activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ubidots.ApiClient;
import com.ubidots.Variable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import com.id.hrm4miband2.Helpers.CustomBluetoothProfile;
import com.id.hrm4miband2.R;

public class MainActivity extends Activity {

    // Default normal Heart Rate Values
    private static final int ABSOLUTE_MAX_BPM = 200;
    private static final int ABSOLUTE_MIN_BPM = 30;

    /////////////////PERMISSIONS/////////////
    //Flag to signal if the Bluetooth Requests are possible
    private final static int REQUEST_ENABLE_BT = 1;
    //Flag to signal if the requests to send sms are possible
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0;
    //Flag to signal if the read/write requests are possible
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    //Flags to signal if read/write permissions are asked and available
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    //NAME OF THE FILE THAT STORES THE APP USER SETTINGS
    private final static String filesettsname = "settings.txt";
    // Get the absolute path to the Directory where the app can write/read
    private final static String path = Environment.getExternalStorageDirectory().getAbsolutePath();

    ////////////MiBand related Variables///////////
    // time to live of a app reading request in milliseconds
    private final Integer mibandTimeOut = 30000;
    // delay to start counting the time to live of a app reading request in milliseconds
    private final Integer mibandTimeOutDelay = 5000;
    // flag to signal to trace if requests can be received
    private Boolean isListeningHeartRate = false;
    private Boolean isListeningBateryLevel = false;
    //flag to signal if MiBand is commanded to vibrate
    private Boolean vibrate = false;
    //flags to signal if the given ubidots service key/id are valid
    private Boolean validHeartRateKEY = false;
    private Boolean validBatteryKEY = false;

    //////////Timer//////////
    //Timer auxiliary TimerTask Var
    private TimerTask timerTask;
    //Timer to do Heart Rate readings
    private Timer timer = new Timer();
    //Time delay to initiate the timer schedule in milliseconds
    private int delay=30000;

    /////////BLUETOOTH service related//////////
    // the device
    private BluetoothDevice bluetoothDevice;
    // the adapters device
    private BluetoothAdapter bluetoothAdapter;
    // the generic Attributes structure class
    //to enable communication with Bluetooth (MiBand2)
    private BluetoothGatt bluetoothGatt;

    /////////////LAYOUT/////////////
    //--BUTTONS
    // Restart Bluetooth Connection to MiBand2 button
    private Button btnStartConnecting;
    // Request MiBand Battery level info button
    private Button btnGetBatteryInfo;
    // request MiBand to read heart rate button
    private Button btnGetHeartRate;
    // request MiBand to start/stop vibrate button
    private Button btnStartVibrate;
    // Show/hide configurations and settings layout button
    private Button btnMiBand_show_cfg;
    //--EDITABLE TEXT
    // shows MiBands Physical MAC Address
    private EditText txtPhysicalAddress;
    // Shows the maximum heart rate value without alarm
    private EditText maxBpmAlarm;
    // Shows the minimum heart rate value without alarm
    private EditText minBpmAlarm;
    // shows how many minute are set on the reading heart rate timer
    private EditText readMin;
    // shows how many hours are set on the reading heart rate timer
    private EditText readHour;
    // defines the Ubidots key
    private EditText ubiKey;
    // defines the Ubidots heart rate variable ID
    private EditText ubiHeartID;
    // defines the Ubidots battery level variable ID
    private EditText ubiBatID;
    //defines the telephone number to send the sms alarm messages
    private EditText txtPhone;
    //--LAYOUT SETS
    //Layout with MiBands heart reading related buttons
    private View heartLy;
    //Layout MiBands Find/vibrate and battery reading level buttons
    private View FBatLy;
    //Layout with Ubidots related key and variable Id settings
    private View ubiLx;
    //Layout with the heart rate reading time interval  related buttons
    private View timerLx;
    //Layout with the heart rate values alarm related buttons
    private View bpmLx;
    //--SEEKBARs
    // Sets the minimum heart rate value without alarm
    private SeekBar setMinBpm;
    // Sets the maximum heart rate value without alarm
    private SeekBar setMaxBpm;
    // Sets how many minute are set on the reading heart rate timer
    private SeekBar setTimerMin;
    // Sets how many hours are set on the reading heart rate timer
    private SeekBar setTimerHour;
    //--ONLY TEXT
    //says the state Disconnected
    private TextView txtState;
    //shows the image connected / connecting
    private TextView txtStateImg;
    //shows the heart rate value
    private TextView txtBpm;
    //shows the battery level value
    private TextView txtBat;

    //stores last red battery level
    private int battlevel;
    //stores last red heart rate
    private int bpm;
    // stores how many heart rate readings out of boundaries
    private int probCounter = 0;

    // stores the Ubidots key
    private String KEY = "";
    // stores the Ubidots battery level variable ID
    private String BATTERY_ID = "";
    // stores the Ubidots heart rate variable ID
    private String HEART_RATE_ID = "";
    // stores the phone number to which the sms is to be sent
    private String phoneNo = "";
    //default value of the maximum heart rate alarm
    private Integer MaxBpmAlarm = 190;
    //default value of the minimum heart rate alarm
    private Integer MinBpmAlarm = 40;
    //default value of the minutes on the heart rate readings timer
    private Integer Min_TIMER = 20;
    //default value of the hours on the heart rate readings timer
    private Integer Hour_TIMER = 0;
    //Auxiliary variable to pass context to some functions
    private final Context context = this;

    ///////TTL Timer of MiBand Answers//////
    /**
     * TTL Timer of MiBand Answers
     *
     * @mibandTimeOut the time to wait for the answer
     * @mibandTimeOutDelay the time to wait until start waiting
     */
    private final CountDownTimer miBandTimeOut = new CountDownTimer(mibandTimeOut,
            mibandTimeOutDelay) {
        public void onTick(long millisUntilFinished) {
            //there's nothing to do
            Log.v("mibandtimerout", "here I am waiting for miband answer");
        }

        public void onFinish() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txtBpm.setText("Failed to read bpm. Please, try again.");
                    Log.v("mibandtimerout", "Failed to read bpm. Please, try again.");
                }
            });

        }
    };

    // the generic Attributes structure callback function
    // to enable communication with Bluetooth device (MiBand2)
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.v("test", "onConnectionStateChange");

            //Bluetooth is connected?
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //start discovering available services
                bluetoothGatt.discoverServices();
                ShowConnected(); //change to the layout 'connected state'
                Log.v("test", "ConnectionStateChange to Connected");
                //info the user
                //says the state connected
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtState.setText("Connected");
                    }
                });
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //disconect de generic Attributes service structure
                bluetoothGatt.disconnect();
                showConnect(); //change to the layout 'able to connect state'
                //info the user
                Log.v("test", "ConnectionStateChange to disconnected");
                //says the state Disconnected
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtState.setText("Disconnected");
                    }
                });
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            //log tracing info
            Log.v("test", "onServicesDiscovered:");
            Log.v("test", "is Listening to HeartRate: " + isListeningHeartRate);
            Log.v("test", "is Listening to BateryLevel: " + isListeningBateryLevel);
            //there is a service
            //listen to the heart rate reading
            listenHeartRate();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.v("test", "onCharacteristicRead");
            //store ccharacteristic
            final byte[] data = characteristic.getValue();
            //get the battery level value
            battlevel = (int) data[1];
            Log.v("test", "listen battery battlevel xubiz : " + battlevel + "%");
            // is the value valid
            if (battlevel > 0) {
                //info the user
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtBat.setText(Integer.toString(battlevel)+ "%");
                    }
                });
                //if the given ubidots service key/id are valid
                if (validBatteryKEY) {
                    //sends the value to Ubidots service
                    new ApiUbidots().execute(KEY, BATTERY_ID, battlevel + "");
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.v("test", "onCharacteristicWrite");

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            String message, phoneNo = txtPhone.getText().toString();

            Log.v("charChanged", "onCharacteristicChanged");
            //store characteristic
            final byte[] data = characteristic.getValue();
            //get the heart rate value
            bpm = (int) data[1];
            //cancel the TTL counter
            miBandTimeOut.cancel();
            //////////////is the value above normal//////////
            if (bpm >= MaxBpmAlarm) {
                message = "The last heart rate value of " + bpm + " it's exceeding the "
                        + MaxBpmAlarm + " bmp Maximum Value";
                //if it is not just a bad reading
                if (probCounter > 2) {
                    //send message through sms services
                    sendSMSMessage(message, phoneNo);
                    //keep reading the heart rate
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startScanHeartRate();
                        }
                    });
                    Log.v("charChanged", "value alarm: " + bpm + " counter:" + probCounter);

                }
            }
            //////////////is the value under normal/////////
            if ((bpm <= MinBpmAlarm) && (MinBpmAlarm >= 0)) {
                message = "The last heart rate value of " + bpm + " it's under the "
                        + MaxBpmAlarm + " bmp Minimum Value";
                //if it is not just a bad reading
                if (probCounter > 2) {
                    //send message through sms services
                    sendSMSMessage(message, phoneNo);
                    //keep reading the heart rate
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            startScanHeartRate();
                        }
                    });
                    Log.v("charChanged", "value alarm: " + bpm + " counter:" + probCounter);
                }
            }
            final String hrbpm = String.valueOf(bpm) + " bpm";
            Log.v("charChanged", "got heartrate bpm: " + hrbpm);
            //if it is a error
            if (bpm < 0) {
                //if it is a error ask user to check MiBand
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtBpm.setText("Adjust the MiBand for better reading");
                    }
                });
            } else {
                //if heart rate is normal
                if (bpm < MaxBpmAlarm && bpm > MinBpmAlarm) {
                    probCounter = 0;
                } else {
                    probCounter++;
                }
                //if the given ubidots service key/id are valid
                if (validHeartRateKEY) {
                    //sends the value to Ubidots service
                    new ApiUbidots().execute(KEY, HEART_RATE_ID, String.valueOf(bpm));
                    //write the heaart rate in the layout
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtBpm.setText(hrbpm);

                        }
                    });
                    //verifies th battery level status
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getBatteryStatus(txtBat);
                        }
                    });
                }
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt,
                                     BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.v("test", "onDescriptorRead");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.v("test", "onDescriptorWrite");
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.v("test", "onReliableWriteCompleted");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.v("test", "onReadRemoteRssi");
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.v("test", "onMtuChanged");
        }

    };

    /**
     * Checks if the app has permission to write to device storage
     * <p>
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity the main activity
     */
    private static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            Toast.makeText(activity, "This permission is needed to be able to save application settings.", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
            Log.v("media", "has permitions");
        }
    }

    /**
     * Checks if external storage is available for read and write
     */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            Log.v("media", "is writable");
            return true;
        }
        return false;
    }

    /**
     * Checks if external storage is available to at least read
     */
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            Log.v("media", "is readable");
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(this);
        isExternalStorageWritable();
        isExternalStorageReadable();


        initializeBluetoothDevice();
        initializeUIComponents();

        // writeSettings(this, filesettsname);
        initializeEvents();
        //writeSettings(this, filesettsname);
        //
        readSettings(this, filesettsname);
        getBoundedDevice();
        startConnecting();
        ShowConnected();
        //initiate a task to atuate the heart rate readings
        timerTask = new TimerTask() {
            public void run() {
                //activate reading
                startScanHeartRate();
            }
        };
        //if there is a time interval
        if ((Min_TIMER > 0) || (Hour_TIMER > 0))
            //set the timer schedule
            timer.scheduleAtFixedRate(timerTask, delay, Min_TIMER * 60000 + Hour_TIMER * 3600000);
    }

    protected void onResume() {
        super.onResume();
        Log.d("onResume", "onResume()");
        getBoundedDevice();
        startConnecting();
        ShowConnected();
    }

    /**
     * gets the bounded Bluetooth devices and chooses MiBand
     */
    private void getBoundedDevice() {
        //stores MiBand2 MAC Address
        String address = "";
        //stores a list of Bluetooth bounded devices
        Set<BluetoothDevice> boundedDevice;
        //waits for the list of Bluetooth bounded devices
        do {
            //gets a list of Bluetooth bounded devices
            boundedDevice = bluetoothAdapter.getBondedDevices();
            // searches and gets MiBand2 MAC Address
            for (BluetoothDevice bd : boundedDevice) {
                if (bd.getName().contains("MI Band 2")) {
                    address = bd.getAddress();
                    txtPhysicalAddress.setText(address);
                } else {
                    Toast.makeText(this, "Waiting for MiBand2 Connection," +
                            " be sure it's paired", Toast.LENGTH_SHORT).show();
                    Log.v("test", "I'm here waiting for connection");
                }
            }
        } while (address.isEmpty());
    }

    /**
     * initialize the Bluetooth Device
     */
    private void initializeBluetoothDevice() {
        // Ask for location permission if not already allowed
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "We need Bluetooth to be able to connect to MiBand2.",
                    Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this, "Device does not support Bluetooth!",
                    Toast.LENGTH_SHORT).show();
            onPause();
        }
        //enables Bluetooth adapter
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Toast.makeText(this, "Getting the  Bluetooth Device...",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Initializing UI components
     */
    private void initializeUIComponents() {
        // Restart Bluetooth Connection to MiBand2 button
        btnStartConnecting = (Button) findViewById(R.id.btnStartConnecting);
        //Layout with MiBands heart reading related buttons
        heartLy = findViewById(R.id.heartLy);
        //Layout MiBands Find/vibrate and battery reading level buttons
        FBatLy = findViewById(R.id.FBatLy);
        //Layout with Ubidots related key and variable Id settings
        ubiLx = findViewById(R.id.ubiLx);
        //Layout with the heart rate reading time interval  related buttons
        timerLx = findViewById(R.id.timerLx);
        //Layout with the heart rate values alarm related buttons
        bpmLx = findViewById(R.id.bpmLx);
        // Request MiBand Battery level info button
        btnGetBatteryInfo = (Button) findViewById(R.id.btnGetBatteryInfo);
        // request MiBand to start/stop vibrate button
        btnStartVibrate = (Button) findViewById(R.id.btnStartVibrate);
        // request MiBand to read heart rate button
        btnGetHeartRate = (Button) findViewById(R.id.btnGetHeartRate);
        // Show/hide configurations and settings layout button
        btnMiBand_show_cfg = (Button) findViewById(R.id.miBand_show_addr);
        // shows MiBands Physical MAC Address
        txtPhysicalAddress = (EditText) findViewById(R.id.txtPhysicalAddress);
        //says the state connected / Disconnected
        txtState = (TextView) findViewById(R.id.txtState);
        //shows the image connected / connecting
        txtStateImg = (TextView) findViewById(R.id.txtState2);
        //shows the heart rate value
        txtBpm = (TextView) findViewById(R.id.txtBpm);
        //shows the battery level value
        txtBat = (TextView) findViewById(R.id.textBat);
        // Shows the maximum heart rate value without alarm
        maxBpmAlarm = (EditText) findViewById(R.id.maxBpmAlarm);
        // Shows the minimum heart rate value without alarm
        minBpmAlarm = (EditText) findViewById(R.id.minBpmAlarm);
        // shows how many minute are set on the reading heart rate timer
        readMin = (EditText) findViewById(R.id.readMin);
        // shows how many hours are set on the reading heart rate timer
        readHour = (EditText) findViewById(R.id.readHour);
        // defines the Ubidots key
        ubiKey = (EditText) findViewById(R.id.ubiID);
        // defines the Ubidots heart rate variable ID
        ubiHeartID = (EditText) findViewById(R.id.ubiHeartKey);
        // defines the Ubidots battery level variable ID
        ubiBatID = (EditText) findViewById(R.id.ubiBatKey);
        // Sets the maximum heart rate value without alarm
        setMaxBpm = (SeekBar) findViewById(R.id.setMaxbpmAlarm);
        // Sets the minimum heart rate value without alarm
        setMinBpm = (SeekBar) findViewById(R.id.setMinbpmAlarm);
        // Sets how many minute are set on the reading heart rate timer
        setTimerMin = (SeekBar) findViewById(R.id.setTimeerMin);
        // Sets how many hours are set on the reading heart rate timer
        setTimerHour = (SeekBar) findViewById(R.id.setTimerHour);
        //defines the telephone number to send the sms alarm messages
        txtPhone = (EditText) findViewById(R.id.sosTelef);
    }

    /**
     * Initializing UI components events
     */
    private void initializeEvents() {

        // Restart Bluetooth Connection to MiBand2 button
        btnStartConnecting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //reconnects to MiBand2
                start();
            }
        });
        // Request MiBand Battery level info button
        btnGetBatteryInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Request MiBand Battery level info
                getBatteryStatus(txtBat);
            }
        });
        // request MiBand to start/stop vibrate button
        btnStartVibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // request MiBand to start/stop vibrate
                startVibrate();
            }
        });
        // request MiBand to read heart rate button
        btnGetHeartRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // request MiBand to read heart rate
                startScanHeartRate();
            }
        });
        // Show/hide configurations and settings layout
        btnMiBand_show_cfg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show/hide configurations and settings layout
                toggleCfg();
            }
        });
        // defines the Ubidots key
        ubiKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // set the Ubidots key
                KEY = ubiKey.getText().toString();
            }
        });
        // defines the Ubidots heart rate variable ID
        ubiHeartID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // sets the Ubidots heart rate variable ID
                HEART_RATE_ID = ubiHeartID.getText().toString();
                //stores the key/id and value
                String[] keyvarArray = new String[]{KEY, HEART_RATE_ID};
                try {
                    //stores if the given ubidots service key/id are valid
                    validHeartRateKEY = new ApiUbidots_VerifyVarId().execute(keyvarArray).get();
                    //is the given ubidots service key/id are valid
                    if (validHeartRateKEY) {
                        Log.v("ubiVar", " Variable id valid ");
                        //the given ubidots service key/id are valid
                        //letters are showed in blue color
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ubiHeartID.setHighlightColor(Color.WHITE);
                                ubiHeartID.setTextColor(Color.BLUE);
                            }
                        });
                    } else {
                        Log.v("ubiVar", " Variable id <<INVALID>>: ");
                        //the given ubidots service key/id are NOT valid
                        //letters are showed in red color
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ubiHeartID.setHighlightColor(Color.GRAY);
                                ubiHeartID.setTextColor(Color.RED);
                                cfgOn();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Log.v("ubiHeart_InterruptedExc", "the exception is " + e.toString());
                } catch (ExecutionException e) {
                    Log.v("ubiheart_ExecutionExc", "the exception is " + e.toString());
                }
            }
        });

        // defines the Ubidots battery level variable ID
        ubiBatID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //stores the key/id and value
                BATTERY_ID = ubiBatID.getText().toString();
                //stores the key/id and value
                String[] keyvarArray = new String[]{KEY, BATTERY_ID};
                try {
                    //stores if the given ubidots service key/id are valid
                    validBatteryKEY = new ApiUbidots_VerifyVarId().execute(keyvarArray).get();
                    //is the given ubidots service key/id are valid
                    if (validBatteryKEY) {
                        Log.v("ubiVar", " Variable id valid ");
                        //the given ubidots service key/id are valid
                        //letters are showed in blue color
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ubiBatID.setHighlightColor(Color.WHITE);
                                ubiBatID.setTextColor(Color.BLUE);
                            }
                        });
                    } else {
                        Log.v("ubiVar", " Variable id <<INVALID>>: ");
                        //the given ubidots service key/id are NOT valid
                        //letters are showed in red color
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ubiBatID.setHighlightColor(Color.GRAY);
                                ubiBatID.setTextColor(Color.RED);
                                cfgOn();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Log.v("ubibat_InterruptedExc", "the exception is " + e.toString());
                } catch (ExecutionException e) {
                    Log.v("ubibat_ExecutionExcept", "the exception is " + e.toString());
                }
            }
        });

        // Sets the minimum heart rate value without firing the alarm
        setMinBpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //the current value
            int value = setMinBpm.getProgress();

            @SuppressLint("SetTextI18n")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //actualize the current value plus offset
                value = progress + ABSOLUTE_MIN_BPM;
                // Shows the current minimum heart rate value without alarm
                minBpmAlarm.setText(Integer.toString(value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //the top value is the current value of  the maximum heart rate alarm value
                setMinBpm.setMax(Integer.valueOf(maxBpmAlarm.getText().toString()) - ABSOLUTE_MIN_BPM);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Shows the final minimum heart rate value without firing the alarm
                minBpmAlarm.setText(Integer.toString(value));
                // stores value of the minimum heart rate alarm
                MinBpmAlarm = value;
            }
        });

        // Sets the maximum heart rate value without alarm
        setMaxBpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //the current value
            int value = MaxBpmAlarm, min;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //actualize the current value plus offset
                value = progress + min;
                // Shows the maximum heart rate value without alarm
                maxBpmAlarm.setText(Integer.toString(value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //the lowest value is the current value of  the minimum heart rate alarm value
                min = Integer.valueOf(minBpmAlarm.getText().toString());
                //the top value is the offset
                // (diference between the value of very top acceptable value and
                // the minimum heart rate alarm value)
                setMaxBpm.setMax(ABSOLUTE_MAX_BPM - min);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Shows the final maximum heart rate value without firing the alarm
                maxBpmAlarm.setText(Integer.toString(value));
                // stores value of the maximum heart rate alarm
                MaxBpmAlarm = value;
            }
        });

        // Sets how many minute are set on the reading heart rate timer
        setTimerMin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //the current value
            int value = Min_TIMER;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //actualize the current value
                value = progress;
                // Shows the current value
                readMin.setText(Integer.toString(value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Shows the final value
                readMin.setText(Integer.toString(value));
                // stores the final value
                Min_TIMER = value;
                //restart the timer with the new setting//
                timer.cancel(); //cancel actual timer
                //if there is a time interval
                if ((Min_TIMER > 0) || (Hour_TIMER > 0)) {
                    //initiate a task to atuate the heart rate readings
                    timerTask = new TimerTask() {
                        public void run() {
                            //activate reading
                            startScanHeartRate();//requests reading heart rate value
                        }
                    };
                    timer = new Timer(); //new Timer instance
                    //finally schedulee the task with the new time settings
                    timer.scheduleAtFixedRate(timerTask, delay, Min_TIMER * 60000 + Hour_TIMER * 3600000);
                }
            }
        });

        // Sets how many hours are set on the reading heart rate timer
        setTimerHour.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //the current value
            int value = Hour_TIMER;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //actualize the current value
                value = progress;
                // Shows the current value
                readHour.setText(Integer.toString(value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Shows the final value
                readHour.setText(Integer.toString(value));
                // stores the final value
                Hour_TIMER = value;
                //restart the timer with the new setting//
                timer.cancel(); //cancel actual timer
                //if there is a time interval
                if ((Min_TIMER > 0) || (Hour_TIMER > 0)) {
                    //initiate a task to atuate the heart rate readings
                    timerTask = new TimerTask() {
                        public void run() {
                            //activate reading
                            startScanHeartRate();//requests reading heart rate value
                        }
                    };
                    timer = new Timer(); //new Timer instance
                    //finally schedulee the task with the new time settings
                    timer.scheduleAtFixedRate(timerTask, delay, Min_TIMER * 60000 + Hour_TIMER * 3600000);
                }
            }
        });

        //defines the telephone number to send the sms alarm messages
        txtPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                phoneNo = txtPhone.getText().toString();
                Log.v("txtphone", "phone number modified.");
            }
        });
    }

    /**
     * reconnects to MiBand2
     */
    private void start() {
        //hide thwe reconect button (already reconnecting)
        btnStartConnecting.setVisibility(View.GONE);
        //initialize the Bluetooth Device if must to
        initializeBluetoothDevice();
        //gets the bounded Bluetooth devices and chooses MiBand MAC
        getBoundedDevice();
        //connects to the stored MAC address
        startConnecting();
        //changes the layout items showing the connected state
        ShowConnected();
    }

    /**
     * toggles the config layout on/off
     */
    private void toggleCfg() {
        //If the txtPhysicalAddress() is visible
        if (txtPhysicalAddress.getVisibility() == View.VISIBLE) {
            //he txtPhysicalAddress() is visible, the config layout is on
            cfgOff(true); //then turn it off
        } else {
            //he txtPhysicalAddress() is not visible, the config layout is off
            cfgOn(); //then turn it on
        }
    }

    /**
     * turns the config layout off
     *
     * @param save boolean
     *             if true, saves the settings
     *             to the settings file
     */
    private void cfgOff(boolean save) {
        // shows MiBands Physical MAC Address
        txtPhysicalAddress.setVisibility(View.INVISIBLE);
        // Restart Bluetooth Connection to MiBand2 button
        btnStartConnecting.setVisibility(View.GONE);
        //Layout with the heart rate values alarm related buttons
        bpmLx.setVisibility(View.GONE);
        //Layout with the heart rate reading time interval  related buttons
        timerLx.setVisibility(View.GONE);
        //Layout with Ubidots related key and variable Id settings
        ubiLx.setVisibility(View.GONE);
        //Layout with MiBands heart reading related buttons
        heartLy.setVisibility(View.VISIBLE);
        //Layout MiBands Find/vibrate and battery reading level buttons
        FBatLy.setVisibility(View.VISIBLE);
        //if true, saves the settings to the settings file
        if (save) writeSettings(context, filesettsname);
    }

    /**
     * turns the config layout on
     */
    private void cfgOn() {
        // shows MiBands Physical MAC Address
        txtPhysicalAddress.setVisibility(View.VISIBLE);
        // Restart Bluetooth Connection to MiBand2 button
        btnStartConnecting.setVisibility(View.GONE);
        //Layout with the heart rate values alarm related buttons
        bpmLx.setVisibility(View.VISIBLE);
        //Layout with the heart rate reading time interval  related buttons
        timerLx.setVisibility(View.VISIBLE);
        //Layout with Ubidots related key and variable Id settings
        ubiLx.setVisibility(View.VISIBLE);
        //Layout with MiBands heart reading related buttons
        heartLy.setVisibility(View.GONE);
        //if true, saves the settings to the settings file
        FBatLy.setVisibility(View.GONE);
    }

    /**
     * changes the layout items showing the connecting state
     */
    private void showConnect() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //hide thwe reconect button (already reconnecting)
                btnStartConnecting.setVisibility(View.VISIBLE);
                // shows MiBands Physical MAC Address
                txtPhysicalAddress.setVisibility(View.INVISIBLE);
                //Layout with the heart rate values alarm related buttons
                bpmLx.setVisibility(View.GONE);
                //Layout with the heart rate reading time interval  related buttons
                timerLx.setVisibility(View.GONE);
                //Layout with Ubidots related key and variable Id settings
                ubiLx.setVisibility(View.GONE);
                //Layout with MiBands heart reading related buttons
                heartLy.setVisibility(View.GONE);
                //Layout MiBands Find/vibrate and battery reading level buttons
                FBatLy.setVisibility(View.GONE);
                //says the state Disconnected
                txtState.setText("Disconnected");
                //shows the image Disconnected
                txtStateImg.setBackground(getResources().getDrawable(R.drawable.bluetooth_off));
            }
        });
    }

    /**
     * changes the layout items showing the connected state
     */
    private void ShowConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cfgOff(false);
                //says the state connected
                txtState.setText("Connected");
                //shows the image connected
                txtStateImg.setBackground(getResources().getDrawable(R.drawable.bluetooth));

            }
        });
    }

    /**
     * connects to the stored MAC address
     */
    private void startConnecting() {
        String address = txtPhysicalAddress.getText().toString();

        bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
        Log.v("startConnecting", "Connecting to " + address);
        Log.v("startConnecting", "Device name " + bluetoothDevice.getName());
        bluetoothGatt = bluetoothDevice.connectGatt(this, true, bluetoothGattCallback);
    }

    /**
     * requests reading heart rate value
     */
    private void startScanHeartRate() {
        // is there a GATT (the generic Attributes service structure)
        if (bluetoothGatt != null) {
            Log.v("startScanHeartRate", "gatt is " + bluetoothGatt.toString());
            // is the service there
            if (bluetoothGatt.getService(CustomBluetoothProfile.Basic.service) == null) {
                Log.v("startScanHeartRate", "...waiting for miBand2 bpm answer...");
                //the service exists flag signal tracer
                isListeningHeartRate = true;
                return;
            }
            //info user wait for the reading ends
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txtBpm.setText("Started Reading HeartRate");
                }
            });
            //get the characteristic (heart rate reading) from the service
            BluetoothGattCharacteristic bchar = bluetoothGatt.getService(CustomBluetoothProfile.HeartRate.service)
                    .getCharacteristic(CustomBluetoothProfile.HeartRate.controlCharacteristic);
            //write the value to be writen on the characteristic (heart rate reading)
            bchar.setValue(new byte[]{21, 2, 1});
            //write the value in the characteristic (heart rate reading)
            bluetoothGatt.writeCharacteristic(bchar);
            Log.v("startScanHeartRate", "Started Reading HeartRate");
            //the service exists flag signal tracer
            isListeningHeartRate = true;
            //start the reading timer
            miBandTimeOut.start();
        } else {
            Log.v("startScanHeartRate", "gatt is null, no bpm reading");
            Toast.makeText(this, "Bluetooth gatt is nuked, no bpm reading", Toast.LENGTH_SHORT).show();

        }
    }

    /**
     * listen to heart rate readings
     */
    private void listenHeartRate() {
        //get the characteristic value (heart rate reading) from the service
        BluetoothGattCharacteristic bchar = bluetoothGatt.getService(CustomBluetoothProfile.HeartRate.service)
                .getCharacteristic(CustomBluetoothProfile.HeartRate.measurementCharacteristic);
        //enable characteristic notification (heart rate reading)
        bluetoothGatt.setCharacteristicNotification(bchar, true);
        //get the Bluetooth profile descriptor (heart rate reading)
        BluetoothGattDescriptor descriptor = bchar.getDescriptor(CustomBluetoothProfile.HeartRate.descriptor);
        //enable descriptor notification on the characteristic (heart rate reading)
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        //write the descriptor with notifications enable
        bluetoothGatt.writeDescriptor(descriptor);
        //the service exists flag signal tracer
        isListeningHeartRate = true;
        Log.v("listenHeartRate", "I'm here listening");
    }

    /**
     * requests reading battery level value
     */
    private void getBatteryStatus(TextView txtBat) {
        //to store temporarily a GATT service (GATT the generic Attributes service structure)
        BluetoothGattService serviceTemp;
        //to store temporarily a GATT service  characteristic (GATT the generic Attributes service structure)
        BluetoothGattCharacteristic bchar;

        // is there a GATT (the generic Attributes service structure)
        if (bluetoothGatt != null) {
            Log.v("getBatteryStatus", "gatt is " + bluetoothGatt.toString());
            //store temporarily the GATT service
            serviceTemp = bluetoothGatt.getService(CustomBluetoothProfile.Basic.service);
            // is the service there
            if (serviceTemp == null) {
                Log.v("getBatteryStatus", "...waiting for miBand2 battery level answer...");
                Toast.makeText(this, "...waiting for miBand2 battery level answer...", Toast.LENGTH_SHORT).show();
                //the service exists flag signal tracer
                isListeningBateryLevel = true;
                return;
            }
            Log.v("getBatteryStatus", "gatt service is " + serviceTemp.toString());
            //get the characteristic value (battery level reading) from the service
            bchar = serviceTemp.getCharacteristic(CustomBluetoothProfile.Basic.batteryCharacteristic);
            //get and store the value red on the characteristic (battery level reading)
            byte[] z = bchar.getValue();
            //is the value on the characteristic been red (battery level reading)
            if (!bluetoothGatt.readCharacteristic(bchar)) {
                Toast.makeText(this, "Failed get battery info", Toast.LENGTH_SHORT).show();
            } else {
                //got a valid battery level value
                if (z != null) {
                    //get the battery level value
                    battlevel = (int) z[1];
                    //if the given ubidots service key/id are valid
                    if (validBatteryKEY) {
                        //sends the value to Ubidots service
                        new ApiUbidots().execute(KEY, BATTERY_ID, battlevel + "");
                        // compose a string to show  in layout
                        String bat = String.valueOf(battlevel) + "%";
                        //show battery level value in layout
                        txtBat.setText(bat);
                    }
                }
            }
        } else {
            Log.v("test", "gatt is null, no battery level reading");
            Toast.makeText(this, "Bluetooth gatt is nuked, no battery level reading", Toast.LENGTH_SHORT).show();

        }
    }

    /**
     * requests MiBand to start/stop vibrating
     */
    private void startVibrate() {
        // is there a GATT (the generic Attributes service structure)
        if (bluetoothGatt != null) {
            //get the characteristic value (vibrate) from the service
            BluetoothGattCharacteristic bchar = bluetoothGatt
                    .getService(CustomBluetoothProfile.AlertNotification.service)
                    .getCharacteristic(CustomBluetoothProfile.AlertNotification.alertCharacteristic);
            // is not vibrating
            if (!vibrate) {
                //get and store the value red on the characteristic (vibrate)
                bchar.setValue(new byte[]{2});
                //is the value on the characteristic been written (vibrate)
                if (!bluetoothGatt.writeCharacteristic(bchar)) {
                    Toast.makeText(this, "Failed start vibrate", Toast.LENGTH_SHORT).show();
                } else {
                    //is vibrating so change the flag
                    vibrate = true;
                    //change information for the user know the new state
                    btnStartVibrate.setText("Found/Stop Vibrate");
                }
            } else {
                //is vibrating
                //get and store the value red on the characteristic (vibrate)
                bchar.setValue(new byte[]{0});
                //is the value on the characteristic been written (vibrate)
                if (!bluetoothGatt.writeCharacteristic(bchar)) {
                    Toast.makeText(this, "Failed stop vibrate", Toast.LENGTH_SHORT).show();
                } else {
                    //is vibrating so change the flag
                    vibrate = false;
                    //change information for the user know the new state
                    btnStartVibrate.setText("Find/Start Vibrate");
                }
            }
        }
    }

    /**
     * reads Ubidots configurations Keys and IDs,
     * Timer and Heart Rate reading Variables settings
     *
     * @param context  Activity context
     * @param filename name of the file that stores the Vars and Confs.
     */
    private void readSettings(Context context, String filename) {
        int index; //Auxiliary var to store temporary values
        try {
            //open File
            FileInputStream fs = context.openFileInput(filename);
            //get stream reader for the File
            InputStreamReader isr = new InputStreamReader(fs, "UTF-8");
            //Get a Buffered reader for the Stream Reader
            BufferedReader br = new BufferedReader(isr);
            //read a line
            String line = br.readLine();
            //line is not null
            if (line != null) {
                //stores only the value, cutting the user info part
                KEY = line.substring(line.indexOf("=") + 2);
                //Show the value in the layout
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ubiKey.setText(KEY);
                    }
                });
            }
            //read a line
            line = br.readLine();
            //line is not null
            if (line != null) {
                //stores only the value, cutting the user info part
                BATTERY_ID = line.substring(line.indexOf("=") + 2);
                //Show the value in the layout
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ubiBatID.setText(BATTERY_ID);
                    }
                });
                //stores the key/id and value
                String[] keyvarArray = new String[]{KEY, BATTERY_ID};
                try {
                    //stores if the given ubidots service key/id are valid
                    validBatteryKEY = new ApiUbidots_VerifyVarId().execute(keyvarArray).get();
                    //is the given ubidots service key/id are valid
                    if (validBatteryKEY) {
                        Log.v("ubiVar", " Variable id valid ");
                        //the given ubidots service key/id are valid
                        //letters are showed in blue color
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ubiBatID.setHighlightColor(Color.WHITE);
                                ubiBatID.setTextColor(Color.BLUE);
                            }
                        });
                    } else {
                        Log.v("ubiVar", " Variable id <<INVALID>>: ");
                        //the given ubidots service key/id are NOT valid
                        //letters are showed in red color
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ubiBatID.setHighlightColor(Color.GRAY);
                                ubiBatID.setTextColor(Color.RED);
                                cfgOn();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Log.v("ubibat_InterruptedExc", "the exception is " + e.toString());
                } catch (ExecutionException e) {
                    Log.v("ubibat_ExecutionExcept", "the exception is " + e.toString());
                }
            }
            //read a line
            line = br.readLine();
            //line is not null
            if (line != null) {
                //stores only the value, cutting the user info part
                HEART_RATE_ID = line.substring(line.indexOf("=") + 2);
                //Show the value in the layout
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ubiHeartID.setText(HEART_RATE_ID);
                    }
                });
                //stores the key/id and value
                String[] keyvarArray = new String[]{KEY, HEART_RATE_ID};
                try {
                    //stores if the given ubidots service key/id are valid
                    validHeartRateKEY = new ApiUbidots_VerifyVarId().execute(keyvarArray).get();
                    //is the given ubidots service key/id are valid
                    if (validHeartRateKEY) {
                        Log.v("ubiVar", " Variable id valid ");
                        //the given ubidots service key/id are valid
                        //letters are showed in blue color
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ubiHeartID.setHighlightColor(Color.WHITE);
                                ubiHeartID.setTextColor(Color.BLUE);
                            }
                        });
                    } else {
                        Log.v("ubiVar", " Variable id <<INVALID>>: ");
                        //the given ubidots service key/id are NOT valid
                        //letters are showed in red color
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ubiHeartID.setHighlightColor(Color.GRAY);
                                ubiHeartID.setTextColor(Color.RED);
                                cfgOn();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Log.v("ubiHeart_InterruptedExc", "the exception is " + e.toString());
                } catch (ExecutionException e) {
                    Log.v("ubiheart_ExecutionExc", "the exception is " + e.toString());
                }
            }
            //read a line
            line = br.readLine();
            //line is not null
            if (line != null) {
                //stores only the value, cutting the user info part
                index = line.indexOf("=");
                MaxBpmAlarm = Integer.parseInt(line.substring(index + 2));
                //Show the value in the layout
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        maxBpmAlarm.setText(Integer.toString(MaxBpmAlarm));
                        setMaxBpm.setProgress(MaxBpmAlarm);
                    }
                });
            }
            //read a line
            line = br.readLine();
            //line is not null
            if (line != null) {
                //stores only the value, cutting the user info part
                index = line.indexOf("=");
                MinBpmAlarm = Integer.parseInt(line.substring(index + 2));
                //Show the value in the layout
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        minBpmAlarm.setText(Integer.toString(MinBpmAlarm));
                        setMinBpm.setProgress(MinBpmAlarm);
                    }
                });
            }
            //read a line
            line = br.readLine();
            //line is not null
            if (line != null) {
                //stores only the value, cutting the user info part
                index = line.indexOf("=");
                Min_TIMER = Integer.parseInt(line.substring(index + 2));
                //Show the value in the layout
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        readMin.setText(Integer.toString(Min_TIMER));
                        setTimerMin.setProgress(Min_TIMER);
                    }
                });
            }
            //read a line
            line = br.readLine();
            //line is not null
            if (line != null) {
                //stores only the value, cutting the user info part
                index = line.indexOf("=");
                Hour_TIMER = Integer.parseInt(line.substring(index + 2));
                //Show the value in the layout
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        readHour.setText(Integer.toString(Hour_TIMER));
                        setTimerHour.setProgress(Hour_TIMER);
                    }
                });
            }
            //read a line
            line = br.readLine();
            //line is not null
            if (line != null) {
                //stores only the value, cutting the user info part
                index = line.indexOf("=");
                phoneNo = line.substring(index + 2);
                //Show the value in the layout
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtPhone.setText(phoneNo);
                    }
                });
            }
            //close FileStreamReader
            fs.close();
            //informs the user that the file has been red
            Toast.makeText(getBaseContext(),
                    "Done reading SD 'mysdfile.txt'",
                    Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            Log.v("write file not found", e.getMessage());
        } catch (UnsupportedEncodingException e) {
            Log.v("UnsupportedEncoding", e.getMessage());
        } catch (IOException e) {
            Log.v("IO exception", e.getMessage());
        }
    }

    /**
     * writes Ubidots configurations Keys and IDs,
     * Timer and Heart Rate reading Variables settings
     *
     * @param context  activity context
     * @param fileName the name of the file to be written
     */
    private void writeSettings(Context context, String fileName) {
        try {
            //create File class instant
            File file = new File(path, fileName);
            // is the File has not benn created yet
            if (!file.exists()) {
                //create a File
                file.createNewFile();
            } else {
                Log.v("writing file", "Done file exists in " + path);
            }
            // File output streamer to write in the file
            FileOutputStream fs = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            //string to be written containing Ubidots configurations Keys and IDs,
            //Timer and Heart Rate reading Variables settings
            String s = "KEY = " + KEY + "\n"
                    + "BATTERY_ID = " + BATTERY_ID + "\n"
                    + "HEART_RATE_ID = " + HEART_RATE_ID + "\n"
                    + "MaxBpmAlarm = " + MaxBpmAlarm + "\n"
                    + "MinBpmAlarm = " + MinBpmAlarm + "\n"
                    + "Min_TIMER = " + Min_TIMER + "\n"
                    + "Hour_TIMER = " + Hour_TIMER + "\n"
                    + "SOS_PHONE = " + phoneNo;
            //write the string in the file
            fs.write(s.getBytes());
            //close FileStream writer
            fs.close();
            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(this,
                    new String[]{file.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });

            Toast.makeText(getBaseContext(),
                    "Done writing SD " + fileName,
                    Toast.LENGTH_SHORT).show();
            Log.v("writing file", "Done writing to path " + path);
        } catch (FileNotFoundException e) {
            Log.v("write file not found", e.getMessage());
        } catch (UnsupportedEncodingException e) {
            Log.v("UnsupportedEncoding", e.getMessage());
        } catch (IOException e) {
            Log.v("IO exception", e.getMessage());
        }

    }

    /**
     * checks permission to send a sms message
     * written on a String variable named 'message'
     * to a number stored on a String var named 'phoneNo'
     *
     * @param smsMessage  String containing a valid sms message to send
     * @param phoneNumber String Containing a valid telephone number
     */
    private void sendSMSMessage(String smsMessage, String phoneNumber) {
        Log.v("sendmsg", "we're in!");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.v("sendmsg", "true-> (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
                //      Toast.makeText(getApplicationContext(), "This permission is needed to be able to send SOS SMS.", Toast.LENGTH_LONG).show();
                Log.v("sendmsg", "true-> (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS))");
            } else {
                //     Toast.makeText(getApplicationContext(), "This permission is needed to be able to send SOS SMS.", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
                Log.v("sendmsg", "ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);");
            }
        } else {
            Log.v("sendmsg", "true-> (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED)");
            //   Toast.makeText(getApplicationContext(), "Please Enter a Valid Phone Number", Toast.LENGTH_SHORT).show();
            Log.v("sendMessage", "sms sent");
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, smsMessage, null, null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        //on request permission code
        switch (requestCode) {
            //get the permission to send sms messages
            case MY_PERMISSIONS_REQUEST_SEND_SMS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v("sendmsg", "true->(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED");
                    // The sms manager
                    SmsManager smsManager = SmsManager.getDefault();
                    // Variable to store the message to be sent
                    String message = "Autorization test";
                    //phone number to which the message will be send
                    String phoneNo = txtPhone.getText().toString();
                    //is there a pone number
                    if (phoneNo.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "Please Enter a Valid Phone Number", Toast.LENGTH_SHORT).show();
                        Log.v("onReqPermResult", "sms failed , phone number is empty");
                    } else {
                        //there is a phone number
                        //the message will be send to the phone number
                        smsManager.sendTextMessage(phoneNo, null, message, null, null);
                        Toast.makeText(getApplicationContext(), "SMS sent.", Toast.LENGTH_LONG).show();
                        Log.v("onReqPermResult", "sms sent");
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "SMS faild, please try again.", Toast.LENGTH_LONG).show();
                    Log.v("onReqPermResult", "dont have permission to send sms ");
                    //            return;
                }
            }
        }
    }

    /**
     * Api from ubidots to
     * store values in Ubidots remote Variables
     * <p>
     * delivers values (params[2])
     * to variable with ID (parmas[1])
     * in a account with the valid KEY (params[0])
     *
     * @params is a String[] array
     * <p>
     * <p>
     * use:  ApiUbidots(String[])
     */
    public class ApiUbidots extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            //getting the Ubidots Api Client off account with the valid KEY (params[0])
            ApiClient apiClient = new ApiClient(params[0]);
            //getting the Ubidots Variable with ID (params[1])
            Variable variable = apiClient.getVariable(params[1]);
            Log.v("test", "sending Ubidots the value: " + params[2]
                    + " to " + params[1] + " Variable");
            //if the Variable ID and the account Key is Valid
            if (variable != null) {
                //sends the values (params[2]) in the Ubidots Variable
                variable.saveValue(Integer.valueOf(params[2]));
            } else {
                Log.v("UBI_SEND_FAIL", "FAIL to send Ubidots  rate value: " + params[2]
                        + " to " + params[1] + " Variable");
            }
            return null;
        }
    }

    /**
     * Api from ubidots
     * to verify if the Ubidots Account key and
     * Variable ID are valid
     * <p>
     * in variable with ID (parmas[1])
     * in a account with the valid KEY (params[0])
     *
     * @params is a String[] array
     * @returns bollean true if both are valid
     * <p>
     * use:  ApiUbidots_VerifyVarId(String[])
     */
    public class ApiUbidots_VerifyVarId extends AsyncTask<String, Void, Boolean> {
        boolean keyIsValid = false;

        @Override
        protected Boolean doInBackground(String... params) {
            //getting the Ubidots Api Client off account with the valid KEY (params[0])
            ApiClient apiClient = new ApiClient(params[0]);
            Log.v("ApiubiVerifyVar", "key is valid");
            //getting the Ubidots Variable with ID (params[1])
            Variable variable = apiClient.getVariable(params[1]);
            //if the Variable ID and the account Key is Valid
            if (variable != null) {
                Log.v("ApiubiVerifyVar", "_return_true");
                return true;
            }
            Log.v("ApiubiVerifyVar", "_return false");
            return false;
        }

        @Override
        protected void onPostExecute(Boolean isValidVarID) {
            super.onPostExecute(isValidVarID);
        }
    }
}
