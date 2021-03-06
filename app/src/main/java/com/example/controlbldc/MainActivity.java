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
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    protected boolean recibExiste=false;
    protected boolean puntos=false;
    protected boolean ejeFijo=false;

    protected ReciboDatos recib;

    protected int REQUEST_ENABLE_BT = 1;
    protected float resolucion = 1;
    protected float resolucionConsigna =1;

    protected double VelConsig;

    //Vectores para los dispositivos: nombres,direcciones y objetos BluetoothDevice//
    protected String[] nombresDisp;
    protected String[] direccionesDisp;
    protected BluetoothDevice[] dispositivos;

    protected Button  btEnvio;
    protected CheckBox chPuntos,chEjefijo;
    public TextView tvVelocidad,tvVelocidadMax,tvVelocidadMin,tvDutyCycle,tvDutyCycleMax,tvDutyCycleMin,tvIntensidad,tvIntensidadMax,tvIntensidadMin;
    protected EditText etValorEnvio;
    protected SeekBar sbBarraDeslizante,sbConsigna;
    protected Spinner spDispositivos;
    protected ImageView ivConectar,ivEncender,ivRestart;
    protected Switch swIman;

    protected boolean conectado=false;
    protected boolean btActiv = false;
    protected boolean encendido = false;

    protected UUID mUUID = fromString("00001101-0000-1000-8000-00805F9B34FB");

    Intent enableBtIntent; // intent para activar bluetooth

    protected Set<BluetoothDevice> pairedDevices;
    protected BluetoothDevice dispConect;

    protected BluetoothAdapter bluetoothAdapter;

    public BluetoothSocket btsocket = null;

    OutputStream salidas;
    InputStream entradas;

    SharedPreferences preferencias;


    //objetos para la grafica
     protected LineChart graficaVel, graficaCorr, graficaDC;



    public void MensajesPantalla(int codigo){

        String mensaje=null;

        Context context = getApplicationContext();
        Toast mensajePant;
        switch (codigo){

            case 0:
                mensaje="Dispositivo NO conectado";
                break;

            case 1:
                mensaje="Campo de env??o vacio";
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

    public void LimpiarTabla(){

        tvVelocidad.setText("Velocidad");
        tvDutyCycle.setText("Duty Cycle");
        tvIntensidad.setText("Corriente");

        tvVelocidadMax.setText("Max:");
        tvVelocidadMin.setText("Min:");

        tvDutyCycleMax.setText("Max:");
        tvDutyCycleMin.setText("Min:");

        tvIntensidadMax.setText("Max:");
        tvIntensidadMin.setText("Min:");

        if(recibExiste&&conectado){
            recib.reiniciarGraficas();

        }

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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().setStatusBarColor(Color.rgb(102-10,129-10,158-10));

        ivConectar = findViewById(R.id.ivConectar);
        ivEncender = findViewById(R.id.ivEncender);
        ivRestart = findViewById(R.id.ivRestart);

        chPuntos = findViewById(R.id.chPuntos);
        chEjefijo = findViewById(R.id.chEjesFijos);

        swIman = findViewById(R.id.swIman);

        tvVelocidad = findViewById(R.id.tvVelocidad);
        tvVelocidadMax = findViewById(R.id.tvVelocidadMax);
        tvVelocidadMin = findViewById(R.id.tvVelocidadMin);

        tvDutyCycle = findViewById(R.id.tvDutyCycle);
        tvDutyCycleMax = findViewById(R.id.tvDutyCycleMax);
        tvDutyCycleMin = findViewById(R.id.tvDutyCycleMin);

        tvIntensidad = findViewById(R.id.tvCorriente);
        tvIntensidadMax = findViewById(R.id.tvCorrienterMax);
        tvIntensidadMin = findViewById(R.id.tvCorrienterMin);

        sbBarraDeslizante = findViewById(R.id.sbBarraDeslizante);
        sbConsigna = findViewById(R.id.sbConsigna);

        etValorEnvio = findViewById(R.id.etValorEnvio);
        btEnvio = findViewById(R.id.btEnvioValor);
        spDispositivos = findViewById(R.id.spDispositivos);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        graficaVel = findViewById(R.id.graficaCorr);
        graficaCorr = findViewById(R.id.graficaVel);
        graficaDC = findViewById(R.id.graficaDC);


        preferencias = getSharedPreferences("PREFERENCIAS",MODE_PRIVATE);

        graficaVel.setVisibility(View.INVISIBLE);
        graficaDC.setVisibility(View.INVISIBLE);
        graficaCorr.setVisibility(View.INVISIBLE);

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


//BOTONES


        ivConectar.setOnClickListener(new View.OnClickListener() {

            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {

            //ANIMACION
                ivConectar.setScaleY(1.25F);
                ivConectar.setScaleX(1.25F);
                CountDownTimer CDTencender = new CountDownTimer(100,100) {
                    @Override
                    public void onTick(long l) {

                    }

                    @Override
                    public void onFinish() {
                        ivConectar.setScaleY(1F);
                        ivConectar.setScaleX(1F);
                    }
                }.start();



                //Comprueba que Bt este activado y pide permiso en caso de no estarlo//
                if (!bluetoothAdapter.isEnabled()) {
                    btActiv=false;
                    enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }else{
                    btActiv=true;
                }

                //Empieza la conexion
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
                        }while(contador<1 && !btsocket.isConnected());

                        if (btsocket.isConnected()) { //conectado correctamente//
                            conectado = true;
                            ivConectar.setImageResource(R.drawable.conectado);

                            try {
                                salidas = btsocket.getOutputStream();
                                entradas = btsocket.getInputStream();

                                //crea el objeto para recibir datos y manejar graficas//
                                if(!recibExiste){
                                    recib = new ReciboDatos(graficaVel,graficaDC,graficaCorr,salidas,entradas,tvVelocidad,tvVelocidadMax,tvVelocidadMin,tvDutyCycle,tvDutyCycleMax,tvDutyCycleMin,tvIntensidad,tvIntensidadMax,tvIntensidadMin,ejeFijo,puntos);
                                    recib.start();
                                    recibExiste=true;
                                }else{
                                    recib.SetSalidasEntradas(salidas,entradas);
                                    recib.Continua();
                                }
                                recib.MuestraOcultaGraficas(0);
                                //recib.EjeFijo(ejeFijo);
                                //recib.Puntos(puntos);
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

                            if(encendido){//si est?? encendido se apaga al desconectar
                                ivEncender.performClick();
                            }


                            btsocket.close();
                            recib.MuestraOcultaGraficas(1);
                            recib.Pausa();
                            conectado=false;
                            ivConectar.setImageResource(R.drawable.desconectado);
                            LimpiarTabla();


                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    }
                }
        });

        ivEncender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ivEncender.setScaleY(1.25F);
                ivEncender.setScaleX(1.25F);
                CountDownTimer CDTencender = new CountDownTimer(100,100) {
                    @Override
                    public void onTick(long l) {

                    }

                    @Override
                    public void onFinish() {
                        ivEncender.setScaleY(1F);
                        ivEncender.setScaleX(1F);
                    }
                }.start();

                if(conectado) {//Solo si esta ya conectado

                    if (!encendido) {//si no esta encendido manda un 1 y cambia la imagen a ROJO

                        try {
                            salidas.write('1');
                            ivEncender.setImageResource(R.drawable.off);
                            encendido=true;

                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                    }else {// si ya esta encendido manda un 0 y cambia la imagen a NEGRO
                        try {
                            salidas.write('0');
                            ivEncender.setImageResource(R.drawable.on);
                            encendido=false;


                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                    } else {
                        MensajesPantalla(0);
                    }
                }

        });

        ivRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ivRestart.setScaleY(1.25F);
                ivRestart.setScaleX(1.25F);
                CountDownTimer CDTencender = new CountDownTimer(100,100) {
                    @Override
                    public void onTick(long l) {

                    }

                    @Override
                    public void onFinish() {
                        ivRestart.setScaleY(1F);
                        ivRestart.setScaleX(1F);
                    }
                }.start();





                LimpiarTabla();

            }
        });

        btEnvio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (conectado) {

                    if (etValorEnvio.getText().length() > 0) {

                        //Lee el valor en el EditText
                        VelConsig = Double.parseDouble(String.valueOf(etValorEnvio.getText()));

                        //se comprueba que no excede los l??mites
                        if (VelConsig >= -3000 && VelConsig <= 3000) {

                            //Se actualiza la barra deslizante
                            sbBarraDeslizante.setProgress((int)(Math.round(VelConsig*resolucion)));
                            sbConsigna.setProgress((int)(Math.round(VelConsig*resolucionConsigna)));

                            String textoEnvio;
                            int caracter;
                            //Se crea la cadena a enviar
                            textoEnvio = "V"+ etValorEnvio.getText()+"f";


                            try {

                                //se envia la cadena
                                for (caracter = 0; caracter < textoEnvio.length(); caracter++) {
                                    salidas.write(textoEnvio.charAt(caracter));
                                }


                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                        }
                    }
                }else{

                    VelConsig = Double.parseDouble(String.valueOf(etValorEnvio.getText()));

                    if (VelConsig >= -3000 && VelConsig <= 3000) {

                        sbBarraDeslizante.setProgress((int) (Math.round(VelConsig * resolucion)));
                        sbConsigna.setProgress((int)(Math.round(VelConsig*resolucionConsigna)));
                    }
                    MensajesPantalla(0);
                }
            }
        });

        sbBarraDeslizante.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {


                    VelConsig = (double) i/resolucion;
                    VelConsig = (int) Math.round(VelConsig);
                    etValorEnvio.setText(Integer.toString((int)VelConsig));

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {


            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                etValorEnvio.setText(Integer.toString((int)VelConsig));

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

        chEjefijo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(chEjefijo.isChecked()){
                    ejeFijo=true;
                }else{
                    ejeFijo=false;
                }

                if(recibExiste&&conectado){
                    recib.EjeFijo(ejeFijo);
                }


            }
        });

        chPuntos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(chPuntos.isChecked()){
                    puntos=true;
                }else{
                    puntos=false;
                }

                if(recibExiste&&conectado){
                    recib.MuestraPuntos(puntos);
                }


            }
        });

        swIman.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    sbBarraDeslizante.setMax(30);
                    sbBarraDeslizante.setMin(-30);
                    resolucion=0.01f;
                    sbBarraDeslizante.setProgress(0);


                }else{
                    sbBarraDeslizante.setMax(3000);
                    sbBarraDeslizante.setMin(-3000);
                    resolucion=1f;
                    sbBarraDeslizante.setProgress(0);


                }
            }
        });



    }
}