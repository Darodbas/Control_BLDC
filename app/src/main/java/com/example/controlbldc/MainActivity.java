package com.example.controlbldc;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {


    protected int REQUEST_ENABLE_BT=1;
    protected  int numDisp=0;
    protected String dispositivos="";

    protected Button btBoton,btBuscar;
    protected TextView tvMensaje,tvDisp;
    Intent enableBtIntent; // intent para activar bluetooth

    protected Set<BluetoothDevice> pairedDevices;

    private BluetoothAdapter bluetoothAdapter = null;



    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case 1:
                if (resultCode == RESULT_OK) {
                    //bluetooth permitido
                    tvMensaje.setText("Bluetooth activado");
                } else if(resultCode == RESULT_CANCELED) {
                    //bluetooth no permitido
                    tvMensaje.setText("Bluetooth sigue inactivo");
                    }

                }
        }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btBoton = findViewById(R.id.btBoton);
        tvMensaje = findViewById(R.id.tvMensaje);
        btBuscar = findViewById(R.id.btBuscar);
        tvDisp = findViewById(R.id.tvDisp);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();



        if (bluetoothAdapter == null) {
            // No soporta bluetooth
            tvMensaje.setText("Bluetooth no soprtado");
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            //No soporta BLE
            Toast.makeText(this, "R.string.ble_not_supported", Toast.LENGTH_SHORT).show();
        }

        btBoton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                    enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }else{
                    tvMensaje.setText("Bluetooth activo");
                }
            }
        });

        btBuscar.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {

               pairedDevices = bluetoothAdapter.getBondedDevices();
               numDisp=0;
                dispositivos="";

                if (pairedDevices.size() > 0) {
                    // There are paired devices. Get the name and address of each paired device.
                    for (BluetoothDevice device : pairedDevices) {
                         String deviceName = device.getName();
                        dispositivos = dispositivos +",   "+ deviceName;
                        String deviceHardwareAddress = device.getAddress(); // MAC address
                        numDisp++;

                        tvDisp.setText("Dispositivos Emparejados:   "+dispositivos);
                    }
                }
            }
        });

    }
}