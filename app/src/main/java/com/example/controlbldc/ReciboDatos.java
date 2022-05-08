package com.example.controlbldc;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;

public class ReciboDatos extends  Thread{

    OutputStream salidas;
    InputStream entradas;
    TextView tvVelocidad,tvVelocidadMax,tvVelocidadMin,tvDutyCycle,tvDutyCycleMax,tvDutyCycleMin,tvIntensidad,tvIntensidadMax,tvIntensidadMin;

    Double velMax,velMin,iMax,iMin,dMax,dMin,vel,I,D,tiempo;

    boolean pausa=false;

    //datos grafica
    protected LineChart graficaVel,graficaCorr,graficaDC;
    protected ArrayList<ILineDataSet> setsDatosGraficaVel = new ArrayList<>();
    protected ArrayList<ILineDataSet> setsDatosGraficaCorr = new ArrayList<>();
    protected ArrayList<ILineDataSet> setsDatosGraficaDC = new ArrayList<>();

    protected LineDataSet SetVel,SetCorr,SetDC;
    protected LineData DatosGrafVel, DatosGrafCorr,DatosGrafDC;

    ArrayList<Entry> dataCorr = new ArrayList<Entry>();
    ArrayList<Entry> dataDC = new ArrayList<Entry>();
    ArrayList<Entry> dataVel = new ArrayList<Entry>();

    public ReciboDatos(LineChart graficaVel,LineChart graficaDC,LineChart graficaCorr, OutputStream salidas, InputStream entradas, TextView tvVelocidad, TextView tvVelocidadMax, TextView tvVelocidadMin, TextView tvDutyCycle, TextView tvDutyCycleMax, TextView tvDutyCycleMin, TextView tvIntensidad, TextView tvIntensidadMax, TextView tvIntensidadMin){
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
        this.graficaVel = graficaVel;
        this.graficaCorr = graficaCorr;
        this.graficaDC = graficaDC;

        velMax=iMax=dMax= -999999999.0;
        velMin=iMin=dMin=99999999.9;

        vel=0.0;
        I=0.0;
        D=0.0;
        tiempo=0.0;
        pausa=false;


    }
    public ReciboDatos(){
        this.entradas=null;
        this.salidas=null;
        this.tvVelocidad=null;
    }

    public void FormatoEjes(){

        XAxis EjeXVel = graficaVel.getXAxis();
        YAxis EjeYVel = graficaVel.getAxisLeft();
        XAxis EjeXCorr = graficaCorr.getXAxis();
        YAxis EjeYCorr = graficaCorr.getAxisLeft();
        XAxis EjeXDC = graficaDC.getXAxis();
        YAxis EjeYDC = graficaDC.getAxisLeft();

        EjeXVel.setDrawLabels(false);



    }



    public ArrayList<Entry> ActualizaDatosGrafica(int codigo,double tiempo,double corr,double duty,double vel){
        ArrayList<Entry> dataVals = new ArrayList<Entry>();

        switch (codigo){

            case 0://corriente

                dataCorr.add(new Entry((float) tiempo, (float) corr));
                dataVals=dataCorr;
                break;

            case 1://DC
                dataDC.add(new Entry((float) tiempo, (float) duty ));

                dataVals=dataDC;
                break;

            case 2:
                dataVel.add(new Entry((float) tiempo, (float) vel));

                dataVals=dataVel;
                break;

        }

        return dataVals;

    }

    public  void ActualizaGrafica(){

        SetCorr = new LineDataSet(ActualizaDatosGrafica(0,tiempo,I,D,vel),"Corriente"); //Actualizamos el set corriente
        SetDC = new LineDataSet(ActualizaDatosGrafica(1,tiempo,I,D,vel),"Duty Cycle"); //Actualizamos el set Velocidad
        SetVel = new LineDataSet(ActualizaDatosGrafica(2,tiempo,I,D,vel),"Velocidad"); //Actualizamos el set Velocidad

        SetCorr.setColors(Color.RED);
        SetDC.setColors(Color.BLUE);
        SetVel.setColors(Color.GREEN);

        SetCorr.setCircleColors(Color.RED);
        SetDC.setCircleColors(Color.BLUE);
        SetVel.setCircleColors(Color.GREEN);

        setsDatosGraficaVel = new ArrayList<>();
        setsDatosGraficaCorr =  new ArrayList<>();
        setsDatosGraficaDC = new ArrayList<>();

        //añadimos al conjunto de todos los sets los nuevos actualizados
        setsDatosGraficaCorr.add(SetCorr);
        setsDatosGraficaDC.add(SetDC);
        setsDatosGraficaVel.add(SetVel);

        //se asocian a los datos totales los nuevos sets
        DatosGrafVel = new LineData(setsDatosGraficaVel);
        DatosGrafCorr = new LineData(setsDatosGraficaCorr);
        DatosGrafDC = new LineData(setsDatosGraficaDC);

        graficaVel.setData(DatosGrafVel);
        //graficaVel.animateX(1000);
        graficaVel.invalidate();

        graficaCorr.setData(DatosGrafCorr);
        //graficaCorr.animateX(1000);
        graficaCorr.invalidate();

        graficaDC.setData(DatosGrafDC);
        //graficaDC.animateX(1000);
        graficaDC.invalidate();


    }

    public void MuestraOcultaGraficas(int codigo){


        switch (codigo){

            case 0://muestra
                graficaDC.setVisibility(View.VISIBLE);
                graficaCorr.setVisibility(View.VISIBLE);
                graficaVel.setVisibility(View.VISIBLE);
                graficaVel.animateX(1000);
                graficaCorr.animateX(1000);
                graficaDC.animateX(1000);
                break;

            case 1://oculta
                graficaDC.setVisibility(View.INVISIBLE);
                graficaCorr.setVisibility(View.INVISIBLE);
                graficaVel.setVisibility(View.INVISIBLE);
                break;


        }

    }

    public void Pausa(){
        pausa=true;
    }
    public void Continua(){
        pausa=false;
    }

    public void SetSalidasEntradas(OutputStream salidas, InputStream entradas){
        this.salidas = salidas;
        this.entradas =entradas;
    }

    public void run(){

        while(true) {

            while (!pausa) {
                ActualizaGrafica();
                char caracter = '?';
                char identificador = '?';
                String cadenaRecibida = "";


                try {

                    entradas.skip(entradas.available());

                    while (caracter != 'V' && caracter != 'I' && caracter != 'D' && caracter != 'T') {//Lee caracteres hasta llegar a una V, I o D//
                        caracter = (char) entradas.read();
                    }
                    identificador = caracter; //guardamos el caracter con el que ha llegado//

                    while (caracter != 'f') { //leemos hasta encontrar una f y lo guardamos en una cadena//
                        caracter = (char) entradas.read();
                        if (caracter != 'f') {
                            cadenaRecibida = cadenaRecibida + caracter;
                        }

                    }

                    switch (identificador) {

                        case 'V':

                            tvVelocidad.setText("Vel. " + cadenaRecibida + " rpm");

                            vel = Double.parseDouble(cadenaRecibida);

                            if (vel > velMax) {
                                velMax = vel;
                            }
                            if (vel < velMin) {
                                velMin = vel;
                            }

                            tvVelocidadMax.setText("Max: " + Double.toString(velMax));
                            tvVelocidadMin.setText("Min: " + Double.toString(velMin));

                            break;

                        case 'D':

                            tvDutyCycle.setText("Dut.Cyc. " + cadenaRecibida);

                            D = Double.parseDouble(cadenaRecibida);

                            if (D > dMax) {
                                dMax = D;
                            }
                            if (D < dMin) {
                                dMin = D;
                            }

                            tvDutyCycleMax.setText("Max: " + Double.toString(dMax));
                            tvDutyCycleMin.setText("Min: " + Double.toString(dMin));

                            break;

                        case 'I':

                            tvIntensidad.setText("Corr. " + cadenaRecibida + " A");

                            I = Double.parseDouble(cadenaRecibida);

                            if (I > iMax) {
                                iMax = I;
                            }
                            if (I < iMin) {
                                iMin = I;
                            }

                            tvIntensidadMax.setText("Max: " + Double.toString(iMax));
                            tvIntensidadMin.setText("Min: " + Double.toString(iMin));

                            break;

                        case 'T':

                            tiempo = Double.parseDouble(cadenaRecibida);
                            tiempo = tiempo / 1000000;//Pasar a segundos
                            break;

                    }


                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }

    }
}
