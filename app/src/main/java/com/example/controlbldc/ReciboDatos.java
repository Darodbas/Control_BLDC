package com.example.controlbldc;

import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ReciboDatos extends Thread{

    OutputStream salidas;
    InputStream entradas;
    TextView tvVelocidad,tvVelocidadMax,tvVelocidadMin,tvDutyCycle,tvDutyCycleMax,tvDutyCycleMin,tvIntensidad,tvIntensidadMax,tvIntensidadMin;

    Double velMax,velMin,iMax,iMin,dMax,dMin,vel,I,D;

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

        velMax=iMax=dMax= -999999999.0;
        velMin=iMin=dMin=99999999.9;

        vel=0.0;
        I=0.0;
        D=0.0;


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

                        vel=Double.parseDouble(cadenaRecibida);

                        if(vel>velMax){
                            velMax=vel;
                        }
                        if(vel<velMin){
                            velMin=vel;
                        }

                        tvVelocidadMax.setText("Max: "+Double.toString(velMax));
                        tvVelocidadMin.setText("Min: "+Double.toString(velMin));

                        break;

                    case 'D':

                        tvDutyCycle.setText("Dut.Cyc. "+cadenaRecibida);

                        D=Double.parseDouble(cadenaRecibida);

                        if(D>dMax){
                            dMax=D;
                        }
                        if(D<dMin){
                            dMin=D;
                        }

                        tvDutyCycleMax.setText("Max: "+Double.toString(dMax));
                        tvDutyCycleMin.setText("Min: "+Double.toString(dMin));

                        break;

                    case 'I':

                        tvIntensidad.setText("Corr. "+cadenaRecibida+" A");

                        I=Double.parseDouble(cadenaRecibida);

                        if(I>iMax){
                            iMax=I;
                        }
                        if(I<iMin){
                            iMin=I;
                        }

                        tvIntensidadMax.setText("Max: "+Double.toString(iMax));
                        tvIntensidadMin.setText("Min: "+Double.toString(iMin));

                        break;


                }



            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
