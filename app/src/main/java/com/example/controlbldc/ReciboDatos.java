package com.example.controlbldc;

import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class ReciboDatos extends Thread{

    OutputStream salidas;
    InputStream entradas;
    TextView tvVelocidad,tvVelocidadMax,tvVelocidadMin,tvDutyCycle,tvDutyCycleMax,tvDutyCycleMin,tvIntensidad,tvIntensidadMax,tvIntensidadMin;

    Double velMax,velMin,iMax,iMin,dMax,dMin,vel,I,D,tiempo;

    //datos grafica
    protected LineChart grafica;
    protected ArrayList<ILineDataSet> setsDatosGrafica = new ArrayList<>();
    protected LineDataSet SetVel,SetCorr,SetDC;
    protected LineData DatosGraf;

    ArrayList<Entry> dataCorr = new ArrayList<Entry>();
    ArrayList<Entry> dataDC = new ArrayList<Entry>();
    ArrayList<Entry> dataVel = new ArrayList<Entry>();

    public ReciboDatos(LineChart grafica, OutputStream salidas, InputStream entradas, TextView tvVelocidad, TextView tvVelocidadMax, TextView tvVelocidadMin, TextView tvDutyCycle, TextView tvDutyCycleMax, TextView tvDutyCycleMin, TextView tvIntensidad, TextView tvIntensidadMax, TextView tvIntensidadMin){
        this.entradas = entradas;
        this.salidas = salidas;
        this.tvVelocidad = tvVelocidad;
        this.tvVelocidadMax = tvVelocidadMax;
        this.tvVelocidadMin = tvVelocidadMin;
        this.tvDutyCycle = tvDutyCycle;
        this.tvDutyCycleMax = tvDutyCycleMax;
        this.tvDutyCycleMin = tvDutyCycleMin;
        this.tvIntensidad = tvIntensidad;
        this.tvIntensidadMax = tvIntensidadMax;
        this.tvIntensidadMin = tvIntensidadMin;
        this.grafica = grafica;

        velMax=iMax=dMax= -999999999.0;
        velMin=iMin=dMin=99999999.9;

        vel=0.0;
        I=0.0;
        D=0.0;
        tiempo=0.0;


    }
    public ReciboDatos(){
        this.entradas=null;
        this.salidas=null;
        this.tvVelocidad=null;
    }

    public ArrayList<Entry> ActualizaDatosGrafica(int codigo){
        ArrayList<Entry> dataVals = new ArrayList<Entry>();

        switch (codigo){

            case 0://corriente

                dataCorr.add(new Entry(0,1));
                dataVals=dataCorr;
                break;

            case 1://DC
                dataDC.add(new Entry(0,4));
                dataVals=dataDC;
                break;

            case 2:
                dataVel.add(new Entry(0,2));
                dataVals=dataVel;
                break;

        }

        return dataVals;

    }

    public  void ActualizaGrafica(){

        SetCorr = new LineDataSet(ActualizaDatosGrafica(0),"Corriente"); //Actualizamos el set corriente
        SetDC = new LineDataSet(ActualizaDatosGrafica(1),"Duty Cycle"); //Actualizamos el set Velocidad
        SetVel = new LineDataSet(ActualizaDatosGrafica(2),"Velocidad"); //Actualizamos el set Velocidad

        //añadimos al conjunto de todos los sets los nuevos actualizados
        setsDatosGrafica.add(SetCorr);
        setsDatosGrafica.add(SetDC);
        setsDatosGrafica.add(SetVel);


        DatosGraf = new LineData(setsDatosGrafica);//se asocian a los datos totales los nuevos sets
        grafica.setData(DatosGraf);
        grafica.invalidate();
    }

    public void run(){



        while(true) {
            ActualizaGrafica();
            char caracter = '?';
            char identificador = '?';
            String cadenaRecibida = "";


            try {

                entradas.skip(entradas.available());

                while (caracter != 'V' && caracter!= 'I' && caracter!= 'D' && caracter !='T' ) {//Lee caracteres hasta llegar a una V, I o D//
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

                    case 'T':

                        tiempo=Double.parseDouble(cadenaRecibida);
                        tiempo=tiempo/1000;//Pasar a segundos
                        break;

                }



            } catch (IOException e) {
                e.printStackTrace();
            }




        }

    }
}
