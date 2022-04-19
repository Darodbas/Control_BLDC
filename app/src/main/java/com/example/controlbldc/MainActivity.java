package com.example.controlbldc;

import static java.util.UUID.fromString;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    protected int REQUEST_ENABLE_BT = 1;
    protected int numDisp = 0;
    protected String dispositivosEmp = "";
    protected String macMicro = "";
    protected String nombreMicro = "DAVID-PC";

    protected Button btBoton, btPaired, btBuscar, btConectar;
    public TextView tvMensaje, tvDisp, tvMAC;

    protected UUID myUUID = fromString("9875E838-C3B2-CCC2-735D-0C9D92C2D64A");


    Intent enableBtIntent; // intent para activar bluetooth

    protected Set<BluetoothDevice> pairedDevices;
    protected BluetoothDevice microControlador;

    public BluetoothAdapter bluetoothAdapter = null;

    protected BluetoothServerSocket mmSocket;
    protected BluetoothDevice mmDevice;
    protected BluetoothSocket tmp;


    @SuppressLint("MissingPermission")
    protected void PairedDisp() { //crea pairedDevices y el String dispositivos y comprueba si alguno es el microcontrolador//
        pairedDevices = bluetoothAdapter.getBondedDevices();
        numDisp = 0;
        dispositivosEmp = "";

        if (pairedDevices.size() > 0) {
            // Hay dispositivos emparejados
            macMicro = "";
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                if (deviceName.equals(nombreMicro)) {
                    macMicro = deviceHardwareAddress;
                    nombreMicro = deviceName;
                    microControlador = device;
                }

                dispositivosEmp = dispositivosEmp + "\r\n" + deviceName;
                numDisp++;

            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case 1:
                if (resultCode == RESULT_OK) {
                    //bluetooth permitido
                    tvMensaje.setText("Bluetooth activado");
                } else if (resultCode == RESULT_CANCELED) {
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
        btPaired = findViewById(R.id.btPaired);
        btBuscar = findViewById(R.id.btBuscar);
        btConectar = findViewById(R.id.btConectar);
        tvMensaje = findViewById(R.id.tvMensaje);
        tvDisp = findViewById(R.id.tvDisp);
        tvMAC = findViewById(R.id.tvMAC);

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
                } else {
                    tvMensaje.setText("Bluetooth activo");
                }
            }
        });

        btPaired.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                PairedDisp();
                tvDisp.setText("Dispositivos Emparejados:\n" + dispositivosEmp + "\n\n" + numDisp + " Dispositivos emparejados");
            }

        });

        btBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PairedDisp();
                if (macMicro.length() > 0) {
                    tvMAC.setText("Mac " + nombreMicro + ": " + macMicro);
                } else {
                    tvMAC.setText("Microcontrolador no encontrado");
                }

            }
        });

        btConectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                Conectar(microControlador);

            }
        });

    }


    @SuppressLint("MissingPermission")
    public void Conectar(BluetoothDevice device) {


        mmDevice = device;

        try {
            mmSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("BLDC_control", myUUID);
        } catch (IOException e) {
            tvMensaje.setText("");
            tvDisp.setText("");
            tvMAC.setText("MAL1");
            Log.e("MIS MENSAJES", "error 1", e);
        }

        //RUN//
        BluetoothSocket socket = null;
        int salir=0;
        // Keep listening until exception occurs or a socket is returned.
        while (salir==0) {
            try {
                socket = mmSocket.accept();
            } catch (IOException e) {
                //Log.e(TAG, "Socket's accept() method failed", e);
                tvMensaje.setText("");
                tvDisp.setText("");
                tvMAC.setText("MAL2");
                break;
            }

            if (socket != null) {
                // A connection was accepted. Perform work associated with
                // the connection in a separate thread.
                //manageMyConnectedSocket(socket);
                try {
                    mmSocket.close();
                    tvMensaje.setText("");
                    tvDisp.setText("");
                    salir=1;
                    tvMAC.setText("Conectado?");
                } catch (IOException e) {
                    tvMensaje.setText("");
                    tvDisp.setText("");
                    tvMAC.setText("MAL3");
                }
                break;
            }

        }



        }

    }
