package com.example.controlbldc;

import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ReciboDatos extends Thread{

    OutputStream salidas;
    InputStream entradas;
    TextView tvVelocidad,tvVelocidadMax,tvVelocidadMin,tvDutyCycle,tvDutyCycleMax,tvDutyCycleMin,tvIntensidad,tvIntensidadMax,tvIntensidadMin;

    public ReciboDatos(OutputStream salidas, InputStream entradas,TextView tvVelocidad,TextView tvVelocidadMax,TextView tvVelocidadMin,TextView tvDutyCycle,TextView tvDutyCycleMax,TextView tvDutyCycleMin,TextView tvIntensidad,TextView tvIntensidadMax,TextView tvIntensidadMin){
        this.entradas=entradas;
        this.salidas=salidas;
        this.tvVelocidad=tvVelocidad;
        this.tvVelocidadMax = tvVelocidadMax;
        this.tvVelocidadMin = tvVelocidadMin;
        this.tvDutyCycle = tvDutyCycle;
        this.tvDutyCycleMax = tvDutyCycleMax;
        this.tvDutyCycleMin = tvDutyCycleMin;
        this.tvIntensidad = tvIntensidad;
        this.tvIntensidadMax = tvIntensidadMax;
        this.tvIntensidadMin = tvIntensidadMin;
    }
    public ReciboDatos(){
        this.entradas=null;
        this.salidas=null;
        this.tvVelocidad=null;
    }
    public void run(){

        while(true) {
            char caracter = '?';
            char identificador = '?';
            String cadenaRecibida = "";


            try {

                entradas.skip(entradas.available());

                while (caracter != 'V' && caracter!= 'I' && caracter!= 'D' ) {//Lee caracteres hasta llegar a una V, I o D//
                    caracter = (char) entradas.read();
                }
                identificador=caracter; //guardamos el caracter con el que ha llegado//

                while (caracter != 'f') { //leemos hasta encontrar una f y lo guardamos en una cadena//
                    caracter = (char) entradas.read();
                    if (caracter != 'f') {
                        cadenaRecibida = cadenaRecibida + caracter;
                    }

                }

                switch (identificador){

                    case 'V':
                        tvVelocidad.setText("Vel. "+cadenaRecibida+" rpm");
                        break;

                    case 'D':
                        tvDutyCycle.setText("Dut.Cyc. "+cadenaRecibida);
                        break;

                    case 'I':
                        tvIntensidad.setText("Corr. "+cadenaRecibida+" A");
                        break;


                }



            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
