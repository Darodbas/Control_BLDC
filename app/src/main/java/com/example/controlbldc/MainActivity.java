package com.example.controlbldc;

import static java.util.UUID.fromString;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
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

    protected ReciboDatos recib;

    protected int REQUEST_ENABLE_BT = 1;
    protected int resolucion = 10000;

    protected double dutyCycle;

    //Vectores para los dispositivos: nombres,direcciones y objetos BluetoothDevice//
    protected String[] nombresDisp;
    protected String[] direccionesDisp;
    protected BluetoothDevice[] dispositivos;

    protected Button  btConectar,btEnvioValor;
    public TextView tvVelocidad,tvVelocidadMax,tvVelocidadMin,tvDutyCycle,tvDutyCycleMax,tvDutyCycleMin,tvIntensidad,tvIntensidadMax,tvIntensidadMin;
    protected EditText etValorEnvio;
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

    public BluetoothSocket btsocket = null;

    OutputStream salidas;
    InputStream entradas;

    SharedPreferences preferencias;

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
            //se llenan los vectores con los nombres y direcciones//
            for (BluetoothDevice device : pairedDevices) {

                nombresDisp[i] = device.getName();
                direccionesDisp[i] = device.getAddress();
                dispositivos[i]=bluetoothAdapter.getRemoteDevice(device.getAddress());

                i++;

            }
            //asociamos los nombres al Spinner//
            ArrayAdapter mi_adaptador = new ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresDisp);
            spDispositivos.setAdapter(mi_adaptador);
            //apuntamos al que hubieramos dejado anteriormente//
            spDispositivos.setSelection(preferencias.getInt("DISPOSITIVO",0));


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
        getSupportActionBar().hide();

        btConectar = findViewById(R.id.btConectar);

        tvVelocidad = findViewById(R.id.tvVelocidad);
        tvVelocidadMax = findViewById(R.id.tvVelocidadMax);
        tvVelocidadMin = findViewById(R.id.tvVelocidadMin);

        tvDutyCycle = findViewById(R.id.tvDutyCycle);
        tvDutyCycleMax = findViewById(R.id.tvDutyCycleMax);
        tvDutyCycleMin = findViewById(R.id.tvDutyCycleMin);

        tvIntensidad = findViewById(R.id.tvCorriente);
        tvIntensidadMax = findViewById(R.id.tvCorrienterMax);
        tvIntensidadMin = findViewById(R.id.tvDutyCycleMin);

        sbBarraDeslizante = findViewById(R.id.sbBarraDeslizante);
        etValorEnvio = findViewById(R.id.etValorEnvio);
        btEnvioValor = findViewById(R.id.btEnvioValor);
        spDispositivos = findViewById(R.id.spDispositivos);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        preferencias = getSharedPreferences("PREFERENCIAS",MODE_PRIVATE);

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
                    btActiv=false;
                    enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }else{
                    btActiv=true;
                }

                if(btActiv) {
                    if (!conectado) {
                        int contador = 0;
                    do{
                        try {
                            btsocket = dispConect.createRfcommSocketToServiceRecord(mUUID);
                            btsocket.connect();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        contador++;
                        }while(contador<3 && !btsocket.isConnected());
                        if (btsocket.isConnected()) { //conectado correctamente//
                            conectado = true;
                            btConectar.setBackgroundColor(Color.GREEN);
                            btConectar.setText("Desconectar");
                            btConectar.setTextColor(Color.BLACK);

                            try {
                                salidas = btsocket.getOutputStream();
                                entradas = btsocket.getInputStream();

                                recib = new ReciboDatos(salidas,entradas,tvVelocidad,tvVelocidadMax,tvVelocidadMin,tvDutyCycle,tvDutyCycleMax,tvDutyCycleMin,tvIntensidad,tvIntensidadMax,tvIntensidadMin);
                                recib.start();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                        } else {

                            try {

                                conectado = false;
                                btsocket.close();
                                MensajesPantalla(4);

                            } catch (IOException e) {
                                e.printStackTrace();
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
                SharedPreferences.Editor editor = preferencias.edit();
                editor.putInt("DISPOSITIVO",i);
                editor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });



    }
}