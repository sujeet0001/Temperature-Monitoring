package com.ble.activities;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ble.R;
import com.ble.dialogs.BuzzAlert;
import com.ble.dialogs.SetThreshold;
import com.ble.pojos.BTDev;

import java.util.UUID;

public class ConnectedDevice extends AppCompatActivity {

    BluetoothDevice bluetoothDevice = null;
    BTDev btDev;
    View toolbar;
    Button startListening, stoplistening, setthreshold;
    LinearLayout lay;
    TextView title, name, addr, tvtemp, minthres, maxthres;
    float min = 0, max = 0;
    MediaPlayer mp;
    BTService btService;
    BuzzAlert buzzAlert;
    private final UUID DEV_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    boolean rchdThrshld, isListening, bzmx = false, bzmn = false;
    SetThreshold setThreshold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connected_device);

        toolbar = findViewById(R.id.cd_toolbar);
        title = toolbar.findViewById(R.id.tb_title);
        title.setText(R.string.cnndev);

        setthreshold = findViewById(R.id.cd_setthreshold);
        stoplistening = findViewById(R.id.cd_stoplistening);
        stoplistening.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isListening){
                    stopReceiving();
                }
            }
        });



        name = findViewById(R.id.cd_devname);
        addr = findViewById(R.id.cd_devadd);
        startListening = findViewById(R.id.cd_listen);
        tvtemp = findViewById(R.id.cd_temp);
        lay = findViewById(R.id.cd_lay);

        minthres = findViewById(R.id.cd_minthres);
        maxthres = findViewById(R.id.cd_maxthres);

        setThreshold = new SetThreshold(ConnectedDevice.this);

        btDev = getIntent().getParcelableExtra("Obj");
        bluetoothDevice = btDev.getBluetoothDevice();

        name.setText(bluetoothDevice.getName());
        addr.setText(bluetoothDevice.getAddress());

        bluetoothDevice.createBond();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(connectDev, filter);

        btService = new BTService(ConnectedDevice.this);

        mp = MediaPlayer.create(this, R.raw.bell);
        mp.setLooping(true);

        LocalBroadcastManager.getInstance(this).registerReceiver(tempReceive, new IntentFilter("Msg"));

        setthreshold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setThreshold.show();
            }
        });

        startListening.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (min == 0 && max == 0) {
                    Toast.makeText(getApplicationContext(), "Thresholds not set", Toast.LENGTH_LONG).show();
                } else {
                    startReceiving(bluetoothDevice, DEV_UUID);
                }
            }
        });
    }

    public void setValues(float min, float max) {
        this.min = min;
        this.max = max;
        if (lay.getVisibility() != View.VISIBLE) {
            lay.setVisibility(View.VISIBLE);
        }
        String mn, mx;
        mn = String.valueOf(min) + " " + getResources().getString(R.string.degree);
        minthres.setText(mn);
        mx = String.valueOf(max) + " " + getResources().getString(R.string.degree);
        maxthres.setText(mx);
        Toast.makeText(getApplicationContext(), "Thresholds set successfully", Toast.LENGTH_LONG).show();
        rchdThrshld = false;
        bzmx = false;
        bzmn = false;

    }

    public void stopBuzzing() {
        mp.pause();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(connectDev);
        mp.stop();
        super.onDestroy();
    }

    void startReceiving(BluetoothDevice device, UUID uuid) {
        btService.startClient(device, uuid);
        isListening = true;
    }

    void stopReceiving(){
        isListening = false;
        btService.stop();
    }

    BroadcastReceiver tempReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            float temp = intent.getFloatExtra("Msg", 0);
            tvtemp.setText(String.valueOf(temp));
            if (!bzmx || !bzmn) {
                buzz(temp);
            }
        }
    };

    private BroadcastReceiver connectDev = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            assert action != null;
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_BONDED:
                        Toast.makeText(getApplicationContext(), "Paired", Toast.LENGTH_LONG).show();
                        bluetoothDevice = device;
                        break;
                    case BluetoothDevice.BOND_BONDING:
                        Toast.makeText(getApplicationContext(), "Pairing", Toast.LENGTH_LONG).show();
                        break;
                    case BluetoothDevice.BOND_NONE:
                        Toast.makeText(getApplicationContext(), "Couldn't pair", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }
    };

    void buzz(float num) {
        if (!rchdThrshld) {
            if (num > min) {
                rchdThrshld = true;
            }
        } else {
            if (num < min && !bzmn) {
                bzmn = true;
                mp.start();
                buzzAlert = new BuzzAlert(ConnectedDevice.this);
                buzzAlert.msg = getResources().getString(R.string.minthrrchd);
                buzzAlert.show();
            } else if (num > max && !bzmx) {
                bzmx = true;
                mp.start();
                buzzAlert = new BuzzAlert(ConnectedDevice.this);
                buzzAlert.msg = getResources().getString(R.string.maxthrrchd);
                buzzAlert.show();
            } else if (num > min && num < max) {
                if(bzmn){
                    bzmn = false;
                }
                if(bzmx){
                    bzmx = false;
                }
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        mp.stop();
    }
}