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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {



    protected int REQUEST_ENABLE_BT = 1;
    protected int resolucion = 10000;



    protected double dutyCycle;

    protected String cadenaRecibida = "";

    //Vectores para los dispositivos: nombres,direcciones y objetos BluetoothDevice//
    protected String[] nombresDisp;
    protected String[] direccionesDisp;
    protected BluetoothDevice[] dispositivos;

    protected Button  btConectar,btEnvio,btRecibe,btEnvioValor;
    public TextView tvRecibo;
    protected EditText etEnvio,etValorEnvio;
    protected SeekBar sbBarraDeslizante;
    protected Spinner spDispositivos;

    protected boolean conectado=false;
    protected boolean btActiv = false;
    protected boolean valorEdittext=false;

    protected UUID mUUID = fromString("00001101-0000-1000-8000-00805F9B34FB");

    Intent enableBtIntent; // intent para activar bluetooth

    protected Set<BluetoothDevice> pairedDevices;
    protected BluetoothDevice dispConect;

    protected BluetoothAdapter bluetoothAdapter;

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

            case 5:
                mensaje="No hay dispositivos emparejados";
                break;


        }
        mensajePant = Toast.makeText(context,mensaje,Toast.LENGTH_SHORT);
        mensajePant.setGravity(Gravity.BOTTOM,0,0);
        mensajePant.show();


    }

    @SuppressLint("MissingPermission")
    public void Emparejados(){
        pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            int i;

            //Se inicializan los vectores de nombres y direcciones de dispositivos//
            for(i=0;i<pairedDevices.size();i++){
                nombresDisp = new String[pairedDevices.size()];
                direccionesDisp = new String[pairedDevices.size()];
                dispositivos = new BluetoothDevice[pairedDevices.size()];

                nombresDisp[i]="";
                direccionesDisp[i]="";
                dispositivos[i]=null;


            }
            i=0;
            //se llenan los vecores con los nombres y direcciones//
            for (BluetoothDevice device : pairedDevices) {

                nombresDisp[i] = device.getName();
                direccionesDisp[i] = device.getAddress();
                dispositivos[i]=bluetoothAdapter.getRemoteDevice(device.getAddress());

                i++;

            }
            //asociamos los nombres al Spinner//
            ArrayAdapter mi_adaptador = new ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresDisp);
            spDispositivos.setAdapter(mi_adaptador);


        }else{
            MensajesPantalla(5);
        }
    }



    @SuppressLint("MissingPermission")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case 1:
                if (resultCode == RESULT_OK) {
                    //bluetooth permitido
                    btActiv=true;
                    Emparejados();



                } else if (resultCode == RESULT_CANCELED) {
                    //bluetooth no permitido
                    btActiv=false;
                    finish();

                }

        }
    }


    @SuppressLint("MissingPermission")
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
        spDispositivos = findViewById(R.id.spDispositivos);

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
        //Si bluetooth no esta activado pide activacion//
        if (!bluetoothAdapter.isEnabled()) {
            enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }else{
            btActiv=true;
        }
        if(btActiv) {
            Emparejados();

        }



        btConectar.setOnClickListener(new View.OnClickListener() {

            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {

                //Comprueba que Bt este activado y pide permiso en caso de no estarlo//
                if (!bluetoothAdapter.isEnabled()) {
                    enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }

                if(btActiv) {
                    if (!conectado) {
                        //Luego busca el micro en los emparejados//
                        //PairedDisp();
                        //intenta conectarse//
                        if (true) {
                            int contador = 0;
                            do {
                                try {
                                    btsocket = dispConect.createRfcommSocketToServiceRecord(mUUID);
                                    btsocket.connect();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                contador++;
                            } while (!btsocket.isConnected() && contador <= 3);
                            if (btsocket.isConnected()) { //conectado correctamente//
                                conectado = true;
                                btConectar.setBackgroundColor(Color.GREEN);
                                btConectar.setText("Desconectar");
                                btConectar.setTextColor(Color.BLACK);


                                try {
                                    salidas = btsocket.getOutputStream();
                                    entradas = btsocket.getInputStream();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            } else { //error al conectar//
                                try {
                                    btsocket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                MensajesPantalla(4);
                                conectado = false;


                            }
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

                            valorEdittext=true;
                            sbBarraDeslizante.setProgress((int)(dutyCycle*resolucion));

                            String textoEnvio;
                            int caracter;
                            textoEnvio = "D"+ etValorEnvio.getText()+"d";


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
                    valorEdittext=true;
                    dutyCycle = Double.parseDouble(String.valueOf(etValorEnvio.getText()));

                    if (dutyCycle >= 0 && dutyCycle <= 1) {

                        sbBarraDeslizante.setProgress((int) (dutyCycle * resolucion));
                    }
                    MensajesPantalla(0);
                }
            }
        });

        sbBarraDeslizante.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                if(valorEdittext==false){//si el valor lo cambia el edit text no hace nada
                    dutyCycle = (double) i/resolucion;
                    etValorEnvio.setText(Double.toString(dutyCycle));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                valorEdittext=false;

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                etValorEnvio.setText(Double.toString(dutyCycle));

            }
        });

        spDispositivos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                dispConect=dispositivos[i];

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });



    }
}