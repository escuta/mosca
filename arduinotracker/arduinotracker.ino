/****************************************************************************
* Copyright (C) 2011 - 2014 Bosch Sensortec GmbH
*
* Euler.ino
* Date: 2014/09/09
* Revision: 3.0 $
*
* Usage:        Example code to stream Euler data
*
****************************************************************************
/***************************************************************************
* License:
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
*   Redistributions of source code must retain the above copyright
*   notice, this list of conditions and the following disclaimer.
*
*   Redistributions in binary form must reproduce the above copyright
*   notice, this list of conditions and the following disclaimer in the
*   documentation and/or other materials provided with the distribution.
*
*   Neither the name of the copyright holder nor the names of the 
*   contributors may be used to endorse or promote products derived from 
*   this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
* ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
* 
* The information provided is believed to be accurate and reliable.
* The copyright holder assumes no responsibility for the consequences of use
* of such information nor for any infringement of patents or
* other rights of third parties which may result from its use.
* No license is granted by implication or otherwise under any patent or
* patent rights of the copyright holder. 
*/

#include "NAxisMotion.h"        //Contains the bridge code between the API and the Arduino Environment
#include <Wire.h>

NAxisMotion mySensor;         //Object that for the sensor 
unsigned long lastStreamTime = 0;     //To store the last streamed time stamp
const int streamPeriod = 20;          //To stream at 50Hz without using additional timers (time period(ms) =1000/frequency(Hz))

void setup() //This code is executed once
{    
  //Peripheral Initialization
  Serial.begin(115200);           //Initialize the Serial Port to view information on the Serial Monitor
  //Serial.begin(9600);           //Initialize the Serial Port to view information on the Serial Monitor
  I2C.begin();                    //Initialize I2C communication to the let the library communicate with the sensor.
  //Sensor Initialization
  mySensor.initSensor();          //The I2C Address can be changed here inside this function in the library
  mySensor.setOperationMode(OPERATION_MODE_NDOF);   //Can be configured to other operation modes as desired
//mySensor.setOperationMode(OPERATION_MODE_NDOF_FMC_OFF);
  
  mySensor.setUpdateMode(MANUAL);	//The default is AUTO. Changing to MANUAL requires calling the relevant update functions prior to calling the read functions
  //Setting to MANUAL requires fewer reads to the sensor  
}

// the following technique adapted from a post by Fredrik Olofsson:
// http://new-supercollider-mailing-lists-forums-use-these.2681727.n2.nabble.com/getting-all-i2c-accelerometer-values-trough-SerialPort-td7628415.html

void loop() //This code is looped forever
{
  if ((millis() - lastStreamTime) >= streamPeriod)
  {
    lastStreamTime = millis();    
    mySensor.updateEuler();        //Update the Euler data into the structure of the object
    //mySensor.updateCalibStatus();  //Update the Calibration Status

   float headingf;
   // correct heading data for 90deg rotation of hardware on headphones
   headingf = mySensor.readEulerHeading() - 90;  
   if (headingf < 0) {
    headingf = (360 + headingf);
   }; 
    
  //  adjust data to suit ATK in SuperCollider  
   if (headingf > 180) {
    headingf = -180 + (headingf - 180);
   }; 
   headingf = headingf * 3.1415926535898 / 180; // convert heading to radians
   float rollf;
   rollf = mySensor.readEulerRoll();
   if (rollf > 180) {
    rollf = -180 + (rollf - 180);
   }; 
   rollf = rollf * 3.1415926535898 / 180; // convert to radians

   float pitchf;
   pitchf = mySensor.readEulerPitch() * -1;
   if (pitchf > 180) {
    pitchf = -180 + (pitchf - 180);
   }; 
   pitchf = pitchf * 3.1415926535898 / 180; // convert to radians

   // note pitch and roll are swapped below because I rotate z axis of device bt 90 degrees (so USB
   // cable can run down left-hand side of headphones together with audio lead
    
   unsigned int heading = (headingf + 3.1415926535898) * 100;
    unsigned int pitch = (rollf + 3.1415926535898) * 100;         
    unsigned int roll = (pitchf + 3.1415926535898) * 100;
    Serial.write(251); 
    Serial.write(252); 
    Serial.write(253); 
    Serial.write(254); 
    Serial.write(heading >> 8); //high 8bits 
    Serial.write(heading & 255);  //low 8bits 
    Serial.write(roll >> 8); 
    Serial.write(roll & 255); 
    Serial.write(pitch >> 8); 
    Serial.write(pitch & 255); 
    Serial.write(255); 
    //delay(5); 

  }
}
