#include "BluetoothSerial.h" //Header File for Serial Bluetooth, will be added by default into Arduino

#define ENC_DATA 19 //blanco, pin 2 encoder, out
#define ENC_CS 5 //negro, pin 6 encoder, out
#define ENC_CLK 18 //verde, pin 4 encoder, in

BluetoothSerial ESP_BT; //Objeto para Bluetooth

//DUTY CYCLES
float dc=0.5; //Duty Cycle fundamental rama positiva
float dA,dB,dC; //Duty Cycle Fases A, B y C
short dcA, dcB, dcC, dcc, dcAc, dcBc, dcCc; //Valor entero de Duty Cycle  

//CORRIENTES
uint16_t Ia,Ib; //Corriente valor entero directo del sensor
float Irealim=0; //corriente realimentación
float Ifilt=0; //Corriente filtrada, solo usada para comprobar el límite
float ia,ib,ic; //Corrientes en las fases A, B y C

//POSICIONES Y SECTORES
uint16_t posicion=0; //Posicion medida directa
uint16_t posicionSect; //Posicion teniendo en cuenta 3 pares de polos
int sector; //Sector del motor [1-6]
int sectorI; //Sector para cálculo de Irealim
int sentido=1;


//CONTROL DE Bucle
float dt = 200e-6; // paso de tiempo (200 microsegundos)
float Ts = 200e-6; // Paso de tiempo 200e-6
unsigned int dtus;
int t1,t2,t3,t4;//controla que no se supere el tiempo dt en la iteración

//Vector para recibo de datos Bluetooth
char BT_data[32]="";

// Interruptor encendido/apagado y Power on
boolean swon, swoff, pwon;

//Valor para filtrado de corriente y velocidad
float alphaI=0.5;
float alphaV=0.9;
// datos para captura de funcionamiento
boolean record, recorded;
short recsamples = 1000;
short recdata1[10000];
short recdata2[10000];
short recdata3[10000];
short rsample = 0;

//Datos calculo velocidad//
 uint16_t pos[2]={0,0};//pos[0]: posicion anterior, pos[1]: posicion actual
 float posEst=0;
 float Vest;
 //float V_K=3000;
 //float V_K=195;// 200
 float V_K=250;// 200
float Vrealim=0;
float VrealimFilt=0;
float V_Ui_realim=0;
float V_Ti_realim=100000000;
float error=0;

  //LIMITES
//(3/2)^0.5 del valor eficaz por conduccion rectangular sobre 2/3 del periodo
//float Ilim=2.5;

float Ilim=7; //limite a partir del cual se apaga el inversor
float IConslim=13;
float VConslim=3000;

//float I_lim_sup=2.57; //2.1*(3/2)^0.5
//float I_lim_inf=-2.57;//-2.1*(3/2)^0.5

float I_lim_sup=2.2; //2.1*(3/2)^0.5
float I_lim_inf=-2.2;//-2.1*(3/2)^0.5

float DC_lim_sup = 0.5; //0.5
float DC_lim_inf = -0.5;//-0.5

float I_Kc=0.035; //Ganancia proporcional Intensidad
float I_Ti=0.004; //Tiempo integral Intensidad

float V_Kc=0.025; //Ganancia proporcional Velocidad 0.01
float V_Ti=0.1; //Tiempo integral Velocidad 1, 0.1

float I_Upid = 0; //accion de control total Intensidad
float I_Upid_Sat = 0.5; //accion de control total Intensidad Saturada
float I_Up=0; //accion de control proporcional Intensidad
float I_Ui=0; //accion de control integral actual(1) y anterior(0) Intensidad

float V_Upid=0; //accion de control total Velocidad
float V_Upid_Sat = 0;
float V_Up=0; //accion de control proporcional Velocidad
float V_Ui=0; //accion de control integral actual(1) y anterior(0) Velocidad

float I_err=0;//error intensidad
float V_err=0; //error velocidad

float I_consigna; //Intensidad consigna
float V_consigna; //Velocidad consigna


//Pines PWM
#define GPIO_PWMA 4   
#define GPIO_PWMB 2   
#define GPIO_PWMC 15   

//Pines Habilitacion
#define GPIO_HABA 14  
#define GPIO_HABB 12  
#define GPIO_HABC 13  

//pin posicion
#define GPIO_POS 23 


#define GPIO_NOTEMSTOP 16   //nivel alto: Encendido // nivel bajo: apagado
#define GPIO_TESTCLK 17


const int freqmin=5;
const int freqmax=50;

//pwm control master setup
const int fpwm = 10000;
const int rpwm = 12;
const int pwmA = 0;
const int pwmB = 1;
const int pwmC = 2;


TaskHandle_t Task2;


void setup() {

  // assign freq & resolution to 3 led pwm channels
  ledcSetup(pwmA, fpwm, rpwm);
  ledcSetup(pwmB, fpwm, rpwm);
  ledcSetup(pwmC, fpwm, rpwm);
  
  // attach GPIOs to led pwm channels
  ledcAttachPin(GPIO_PWMA, pwmA);
  ledcAttachPin(GPIO_PWMB, pwmB);
  ledcAttachPin(GPIO_PWMC, pwmC);
  
  pinMode(GPIO_NOTEMSTOP, OUTPUT);
  digitalWrite(GPIO_NOTEMSTOP, LOW);
  pinMode(GPIO_TESTCLK, OUTPUT);
  digitalWrite(GPIO_TESTCLK, LOW);
  
  //canales habilitacion fases y posicion
  pinMode(GPIO_HABA, OUTPUT);
  digitalWrite(GPIO_HABA, LOW);
   pinMode(GPIO_HABB, OUTPUT);
  digitalWrite(GPIO_HABB, LOW);
   pinMode(GPIO_HABC, OUTPUT);
  digitalWrite(GPIO_HABC, LOW);
  pinMode(GPIO_POS, OUTPUT);
  digitalWrite(GPIO_POS, LOW);
  
  //configuración pines encoder
  pinMode(ENC_CLK, OUTPUT);
  pinMode(ENC_CS, OUTPUT);
  pinMode(ENC_DATA, INPUT);

  dtus = dt*1e6;

  Serial.begin(115200);
 
  // Comienza la comunicación Bluetooth
  ESP_BT.begin("ESP32Drive"); //Nombre de la señal Bluetooth
  
  //Se crea una tarea que será ejecutada en la función task2code() con prioridad 1 en el nucleo 0
  
  xTaskCreatePinnedToCore(
                    Task2code,   /* función de la tarea */
                    "Task2",     /* nombre de la tarea */
                    10000,       /* tamaño de la tarea */
                    NULL,        /* parametro de la tarea */
                    1,           /* prioridad de la tarea*/
                    &Task2,      /* identificador de la tarea */
                    0);          /* nucleo en que se ejecuta la tarea */ 
}


void loop() {

  if(swon){

    pwon = true;
    swon = false;
    //record=true;
    t1=0;
    t2=0;
    I_consigna=0;
    I_Ui=0;
    //alphaI = 0;
    digitalWrite(GPIO_NOTEMSTOP,HIGH);
    
   }
  if(swoff){
    digitalWrite(GPIO_NOTEMSTOP,LOW);
    pwon = false;
    swoff = false;
    I_Ui=0;
    I_consigna=0;
    t1=0;
    t2=0;
    dc=0.5;
    
  }

 //Se recogen datos fuera del Bucle de encendido
  posicion = medirPos();  
  posicionSect = (posicion*3) & 1023;//para tener en cuenta los 3 pares de polos//
  CalcSector(posicionSect);
  Vrealim = CalcV();
  Ia=analogRead(39);
  Ib=analogRead(36);
  Irealim = CalcI(Ia,Ib,posicion);
  
  Ifilt=alphaI*Ifilt+(1-alphaI)*Irealim;
  
  if(Ifilt<0.1 and Ifilt>-0.1 and (ia+ib+ic)<0.2 and (ia+ib+ic)>-0.2) Ifilt=0;
  
  VrealimFilt=alphaV*VrealimFilt+(1-alphaV)*Vrealim;
  
  if((VrealimFilt<5) and Vrealim<50 and VrealimFilt>-5 and Vrealim>-50)VrealimFilt=0;

  if(Vrealim>0) sentido=1;
  if(Vrealim<0) sentido=-1;
  
  
  unsigned int nowus, nextus;

  nextus = micros()+dtus; //nextus: valor del tiempo que deberá pasar en la siguiente iteración, en cada iteración
  
  while(pwon&&!swoff){
    
    digitalWrite(GPIO_TESTCLK,true);
       
    //REGISTRO DE DATOS

    posicion = medirPos();
    posicionSect = (posicion*3) & 1023;//para tener en cuenta los 3 pares de polos//
    CalcSector(posicionSect);
    Vrealim = CalcV();
    Ia=analogRead(39);
    Ib=analogRead(36);
    Irealim = CalcI(Ia,Ib,sector);
    
    Ifilt=alphaI*Ifilt+(1-alphaI)*Irealim;
    if(Ifilt<0.1 and Ifilt>-0.1 and (ia+ib+ic)<0.2 and (ia+ib+ic)>-0.2) Ifilt=0;
    
     
    //LIMITE DE CORRIENTE
    if(abs(Ifilt)>Ilim){
      digitalWrite(GPIO_NOTEMSTOP,false);
      swoff=true;
      ESP_BT.println("Sobrecorriente");
    }
    
  
  ///////PID EN CASCADA///////

    //LAZO VELOCIDAD

    //se obtiene el error
    V_err = V_consigna-Vrealim;

    //se calculan las acciones proporcional e integral
    V_Up = V_Kc*V_err;

    //Antiwindup
    if(abs(V_Upid - V_Upid_Sat)>0.1){
      
      //no se integra
      
    }else{
      
       V_Ui = V_Ui+((V_Kc*Ts)/V_Ti)*V_err;
      
    }
    
    //Calculo de la acción de control
    V_Upid = V_Up + V_Ui;
    
    //Saturamos en el rango [I_lim_sup; I_lim_inf]
    if(V_Upid>I_lim_sup) V_Upid_Sat=I_lim_sup;
    else if(V_Upid<I_lim_inf)V_Upid_Sat=I_lim_inf;
    else V_Upid_Sat=V_Upid;

    //Asociamos la accion de control tras la saturación a la consigna de corriente
    I_consigna = V_Upid_Sat;

    //LAZO DE CORRIENTE

    //Se obtiene el error
    I_err=I_consigna-Irealim;


    //Se calculan las acciones proporcional e integral
    I_Up = I_Kc*I_err;

    //Antiwindup
    if((I_Upid-I_Upid_Sat)>0.1 or (I_Upid-I_Upid_Sat)<-0.1){
      
      // No se integra
      
    }else{

      I_Ui = I_Ui+((I_Kc*Ts)/I_Ti)*I_err;
      
    }

    //Calculo de la accion de control
    I_Upid = I_Up + I_Ui;

    //Saturamos en el rango [DC_lim_sup; DC_lim_inf]
    
    if(I_Upid>DC_lim_sup) I_Upid_Sat=DC_lim_sup;
    
    else if(I_Upid<DC_lim_inf)I_Upid_Sat=DC_lim_inf;
    
    else I_Upid_Sat=I_Upid;

    //se asocia la accion de control despues de la saturación al duty cycle

    dc=I_Upid_Sat+0.5;

  ///////FIN DEL PID//////////
    
    GeneraPWM(sector,dc);//Solo si está encendido   //Porque dc como parametro??
    

    digitalWrite(GPIO_TESTCLK,false);
    while(nextus > micros()){}//Espera hasta que micros = nexus
    nextus += dtus; 
    
  }
  digitalWrite(GPIO_NOTEMSTOP,LOW);
}

void Task2code( void * pvParameters ){

  short posBT = 0;

  while(true){

    while(ESP_BT.available()){ //Check if we receive anything from Bluetooth
      
      BT_data[posBT]=ESP_BT.read();
      posBT++;
      Serial.println(BT_data[posBT]);

    }

    if(posBT){
    //Lectura de datos por Bluetooth
      /* ENCENDER*/
      if(BT_data[0] == 49 /*1*/){
        swon=true;
        ESP_BT.println("Encender");
      }
      /*APAGAR */
      else if(BT_data[0] == 48 /*0*/){
        swoff=true;
        ESP_BT.println("Apagar");
      }
      /*CONSIGNA VELOCIDAD */
      else if(BT_data[0] == 86 /*V*/){
        
       String Vcon=""; 
       
        for( int pos=1 ; pos<32 ; pos++ ){

            if(BT_data[0]==102/*f*/){
              break;
            }
            Vcon=Vcon+BT_data[pos];
        }
        if(abs(Vcon.toFloat())<VConslim){
          I_Ui=0;
          V_Ui=0;
           V_consigna=Vcon.toFloat();
        }
      }
    }
    posBT = 0;
    
    //Envio de datos por bluetooth
    ESP_BT.print("T"+String(millis())+"f"); //Tiempo
    ESP_BT.println(";");
    ESP_BT.print("D"+String(dc)+"f"); //Duty Cycle
    ESP_BT.println(";");
    ESP_BT.print("I"+String(Irealim)+"f"); //Corriente
    ESP_BT.println(";");
    ESP_BT.print("V"+String(Vrealim)+"f"); //Velocidad
    ESP_BT.println(";");

     
    delay(100);

    
  }
}

//Funciones para lectura de posición
void delayshort(unsigned short nnops){ 

  for (unsigned short i=0; i<nnops; i++) __asm__("nop\n\t"); 

} 

uint16_t medirPos(){ 

  uint16_t pos=0; 

  digitalWrite(ENC_CS, LOW);  

 digitalWrite(ENC_CLK, LOW); delayshort(44); 

  for (int i=9; i>=0; i--) { 

    digitalWrite(ENC_CLK, HIGH); delayshort(44); 

    pos=pos|(digitalRead(ENC_DATA)<<i); 

    digitalWrite(ENC_CLK, LOW); delayshort(22); 
 } 

 

  digitalWrite(ENC_CLK, HIGH); 

  digitalWrite(ENC_CS, HIGH); 

   

  return pos; 
}

short CalcD(float dc){
  
/*Calcula el duty cycle como valor entero a partir del valor decimal [0,1] ---> [0,2^res-1]*/

    short DC;

    DC = dc*(pow(2,rpwm)-1);

    return DC;

}



void GeneraPWM(int sector,float dc){

switch(sector){
    
    case 1:/* Fase A: +; Fase B: 0; Fase C: - */
      
      digitalWrite(GPIO_HABA, LOW);

      digitalWrite(GPIO_HABB, HIGH);
  
      digitalWrite(GPIO_HABC, LOW);

      dA=dc;
      dB=0;
      dC=1-dc;

      dcAc =CalcD(dA);
      dcBc =CalcD(dB);
      dcCc =CalcD(dC);

      ledcWrite(pwmA,dcAc);
      ledcWrite(pwmB,dcBc);
      ledcWrite(pwmC,dcCc);
   
      break;

      case 2:/* Fase A: 0; Fase B: +; Fase C: - */
      
      digitalWrite(GPIO_HABA, HIGH);

      digitalWrite(GPIO_HABB, LOW);
    
      digitalWrite(GPIO_HABC, LOW);
  
      dA=0;
      dB=dc;
      dC=1-dc;

      dcAc =CalcD(dA);
      dcBc =CalcD(dB);
      dcCc =CalcD(dC);

      ledcWrite(pwmA,dcAc);
      ledcWrite(pwmB,dcBc);
      ledcWrite(pwmC,dcCc);

      
      break;

      case 3:
      /* Fase A: -; Fase B: +; Fase C: 0 */
      
      digitalWrite(GPIO_HABA, LOW);

      digitalWrite(GPIO_HABB, LOW);
    
      digitalWrite(GPIO_HABC, HIGH);
  
      dA=1-dc;
      dB=dc;
      dC=0;

      dcAc =CalcD(dA);
      dcBc =CalcD(dB);
      dcCc =CalcD(dC);

      ledcWrite(pwmA,dcAc);
      ledcWrite(pwmB,dcBc);
      ledcWrite(pwmC,dcCc);
      break;

      case 4:
      /* Fase A: -; Fase B: 0; Fase C: + */
      
      digitalWrite(GPIO_HABA, LOW);

      digitalWrite(GPIO_HABB, HIGH);
    
      digitalWrite(GPIO_HABC, LOW);
  
      dA=1-dc;
      dB=0;
      dC=dc;

      dcAc =CalcD(dA);
      dcBc =CalcD(dB);
      dcCc =CalcD(dC);

      ledcWrite(pwmA,dcAc);
      ledcWrite(pwmB,dcBc);
      ledcWrite(pwmC,dcCc);
      
      break;

      case 5:
      /* Fase A: 0; Fase B: -; Fase C: + */
      
      digitalWrite(GPIO_HABA, HIGH);

      digitalWrite(GPIO_HABB, LOW);
    
      digitalWrite(GPIO_HABC, LOW);
  
      dA=0;
      dB=1-dc;
      dC=dc;

      dcAc =CalcD(dA);
      dcBc =CalcD(dB);
      dcCc =CalcD(dC);

      ledcWrite(pwmA,dcAc);
      ledcWrite(pwmB,dcBc);
      ledcWrite(pwmC,dcCc);
      break;

      case 6:/* Fase A: +; Fase B: -; Fase C: 0 */
      
      digitalWrite(GPIO_HABA, LOW);

      digitalWrite(GPIO_HABB, LOW);
    
      digitalWrite(GPIO_HABC, HIGH);
  
      dA=dc;
      dB=1-dc;
      dC=0;

      dcAc =CalcD(dA);
      dcBc =CalcD(dB);
      dcCc =CalcD(dC);

      ledcWrite(pwmA,dcAc);
      ledcWrite(pwmB,dcBc);
      ledcWrite(pwmC,dcCc);
      break;
     
    }
}

 float CalcI(uint16_t Ia,uint16_t Ib,int sector){

 
  ia = 0.00288*Ia-5.4506;
  ib = 0.00288*Ib-5.4327;
  ic = -ia-ib;
  
    switch(sector){

        case 1: //A:+, B:0, C:-
          return (ia-ib*(sentido)-ic)/2;
          break;

        case 2: //A:0, B:+, C:-
          return (+ia*(sentido)+ib-ic)/2;
          break;

        case 3: //A:-, B:+, C:0
           return (ib-ia-ic*(sentido))/2;
           break;

        case 4: //A:-, B:0, C:+
            return (+ib*(sentido)-ia+ic)/2;
            break;
            
        case 5: //A:0, B:-, C:+
          return (-ib-ia*(sentido)+ic)/2;
          break;
          
        case 6: //A:+, B:-, C:0
          return (ia-ib+ic*(sentido))/2;
          break;

        
    }
 }

void CalcSector(uint16_t posicionSect){
  
    if(posicionSect<89)sector=6;
    else if(posicionSect<260)sector=1;
    else if(posicionSect<430)sector=2;
    else if(posicionSect<601)sector=3;
    else if(posicionSect<772)sector=4;
    else if(posicionSect<942)sector=5;
    else sector=6;

  
}

float CalcV(){

  float Velocidad;
  
  pos[1]=posicion;
  
  posEst=posEst+Vest*dt;
  
  if(posEst>=1024.0){
    
    posEst-=1024.0;
  }
  if(posEst<0.0){
    posEst =1024.0-posEst;
  }
 
  error=(pos[1]-posEst);

  if(error<-500) error+=1024;
  if(error>500) error-=1024;
   
  V_Ui_realim=V_Ui_realim+((V_K*dt)/V_Ti_realim)*error;
  
  Vest = V_K*error+V_Ui_realim;
  
  pos[0]=pos[1];

  Velocidad= Vest*(1/1024.00)*60.00;
  
  return Velocidad;

  
}

 
