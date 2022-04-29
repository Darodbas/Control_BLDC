package com.example.controlbldc;

import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ReciboDatos extends Thread{

    OutputStream salidas;
    InputStream entradas;
    TextView tvRecibo;

    public ReciboDatos(OutputStream salidas, InputStream entradas,TextView tvRecibo){
        this.entradas=entradas;
        this.salidas=salidas;
        this.tvRecibo=tvRecibo;
    }
    public ReciboDatos(){
        this.entradas=null;
        this.salidas=null;
        this.tvRecibo=null;
    }
    public void run(){

        while(true) {
            char caracter = ' ';
            String cadenaRecibida = "";


            try {

                entradas.skip(entradas.available());

                while (caracter != 'V' && caracter!= 'I' && caracter!= 'D' ) {

                    caracter = (char) entradas.read();
                }
                while (caracter != 'f') {
                    caracter = (char) entradas.read();
                    if (caracter != 'f') {
                        cadenaRecibida = cadenaRecibida + caracter;
                    }

                }
                tvRecibo.setText(cadenaRecibida);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
