package com.example.controlbldc;

import static java.util.UUID.fromString;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {



    protected int REQUEST_ENABLE_BT = 1;
    protected int numDisp = 0;
    protected int resolucion = 100000;

    protected double dutyCycle;

    protected String dispositivosEmp = "";
    protected String macMicro = "";
    protected String nombreMicro = "DAVID-PC";
    protected String cadenaRecibida = "";

    protected Button  btConectar,btEnvio,btRecibe,btEnvioValor;
    public TextView tvRecibo;
    protected EditText etEnvio,etValorEnvio;
    protected SeekBar sbBarraDeslizante;

    protected boolean conectado=false;


    protected UUID mUUID = fromString("00001101-0000-1000-8000-00805F9B34FB");


    Intent enableBtIntent; // intent para activar bluetooth

    protected Set<BluetoothDevice> pairedDevices;
    protected BluetoothDevice microControlador;

    public BluetoothAdapter bluetoothAdapter = null;

    protected BluetoothSocket btsocket = null;

    OutputStream salidas;
    InputStream entradas;



    public void MensajesPantalla(int codigo){

        String mensaje=null;

        Context context = getApplicationContext();
        Toast mensajePant;
        switch (codigo){

            case 0:
                mensaje="Dispositivo NO conectado";
                break;

            case 1:
                mensaje="Campo de envío vacio";
                break;

            case 3:
                mensaje="Bluetooth no soportado";
                break;

            case 4:
                mensaje="Error al conectar";
                break;


        }
        mensajePant = Toast.makeText(context,mensaje,Toast.LENGTH_SHORT);
        mensajePant.setGravity(Gravity.BOTTOM,0,0);
        mensajePant.show();


    }



    @SuppressLint("MissingPermission")
    public void PairedDisp() { //crea pairedDevices y el String dispositivos y comprueba si alguno es el microcontrolador//
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
                    microControlador = bluetoothAdapter.getRemoteDevice(macMicro);
                }

                dispositivosEmp = dispositivosEmp + "\r\n" + deviceName;
                numDisp++;

            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case 1:
                if (resultCode == RESULT_OK) {
                    //bluetooth permitido
                } else if (resultCode == RESULT_CANCELED) {
                    //bluetooth no permitido
                }

        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btConectar = findViewById(R.id.btConectar);
        etEnvio = findViewById(R.id.etTextoAEnviar);
        btEnvio = findViewById(R.id.btEnviar);
        tvRecibo = findViewById(R.id.tvRecibe);
        btRecibe = findViewById(R.id.btRecibir);
        sbBarraDeslizante = findViewById(R.id.sbBarraDeslizante);
        etValorEnvio = findViewById(R.id.etValorEnvio);
        btEnvioValor = findViewById(R.id.btEnvioValor);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Comprobaciuones iniciales//
        if (bluetoothAdapter == null) {
            // No soporta bluetooth
           MensajesPantalla(3);
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            //No soporta BLE
            MensajesPantalla(3);
        }



        btConectar.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
                boolean btactiv;
                //Comprueba que Bt este activado y pide permiso en caso de no estarlo//
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                    enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    btactiv=false;
                } else {

                    btactiv=true;
                }

                if(btactiv) {
                    if (!conectado) {
                        //Luego busca el micro en los emparejados//
                        PairedDisp();

                        //intenta conectarse//
                        if (macMicro.length() > 0) {

                            int contador = 0;

                            do {


                                try {

                                    btsocket = microControlador.createRfcommSocketToServiceRecord(mUUID);

                                    btsocket.connect();


                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                contador++;
                            } while (!btsocket.isConnected() && contador <= 10);

                            if (btsocket.isConnected()) {
                                conectado = true;

                                btConectar.setBackgroundColor(Color.GREEN);
                                btConectar.setText("Desconectar");
                                btConectar.setTextColor(Color.BLACK);
                            } else {
                                MensajesPantalla(4);
                                conectado = false;
                            }

                            try {
                                salidas = btsocket.getOutputStream();
                                entradas = btsocket.getInputStream();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                        } else {

                        }
                    }else{
                        try {
                            btsocket.close();
                            conectado=false;

                            btConectar.setBackgroundColor(Color.BLUE);
                            btConectar.setText("Conectar");
                            btConectar.setTextColor(Color.WHITE);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        btEnvio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (conectado) {
                    if (etEnvio.getText().length() > 0) {

                        String textoEnvio;
                        int caracter;
                        textoEnvio = String.valueOf(etEnvio.getText());

                        try {

                            for (caracter = 0; caracter < textoEnvio.length(); caracter++) {
                                salidas.write(textoEnvio.charAt(caracter));
                            }


                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }else{
                        MensajesPantalla(1);
                    }
                }else {
                    MensajesPantalla(0);
                }
            }
        });

        btRecibe.setOnClickListener(new View.OnClickListener() { //espera a recibir datos, solo finaliza si recibe 'f'//
            @Override
            public void onClick(View view) {



                if (conectado) {
                    char caracter=' ';
                    cadenaRecibida="";

                    try {

                        entradas.skip(entradas.available());



                       while(caracter!='f'){

                           caracter =(char) entradas.read();
                           if(caracter!='f'){
                               cadenaRecibida = cadenaRecibida + caracter;
                           }


                        }
                        tvRecibo.setText(cadenaRecibida);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }else{
                    MensajesPantalla(0);
                }
            }
        });

        btEnvioValor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (conectado) {
                    if (etValorEnvio.getText().length() > 0) {

                        dutyCycle = Double.parseDouble(String.valueOf(etValorEnvio.getText()));

                        if (dutyCycle >= 0 && dutyCycle <= 1) {

                            sbBarraDeslizante.setProgress((int)(dutyCycle*resolucion));

                            String textoEnvio;
                            int caracter;
                            textoEnvio = "D"+ String.valueOf(etValorEnvio.getText())+"d";


                            try {

                                for (caracter = 0; caracter < textoEnvio.length(); caracter++) {
                                    salidas.write(textoEnvio.charAt(caracter));
                                }


                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                }else{
                    MensajesPantalla(0);
                }
            }
        });

        sbBarraDeslizante.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                dutyCycle = (double) i/resolucion;
                etValorEnvio.setText(Double.toString(dutyCycle));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                etValorEnvio.setText(Double.toString(dutyCycle));

            }
        });




    }
}

