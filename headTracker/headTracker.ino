/*
 * Created by Iain Mott
 * updated by Gaël Jaton
 *  
 * Official BNO055 support for Arduino can be downloaded form 
 * https://github.com/arduino-libraries/NineAxesMotion/archive/master.zip 
 * and unziped in your Arduino home folder
 */

//#define GPS      // Use the Neo GPS module
//#define SIDEWAYS  // For a device rotated by 90 degrees on Headphones
//#define DEBUG    // Print out human readable data
//#define POT   // optional volume pot on potPin

#include "NAxisMotion.h"
#include <Wire.h>

#ifdef POT
int potPin = 2;
#endif

#ifdef GPS

// The NeoGPS and AltSoftSerial Library can be installed through the Library manager found in the /Tools tab or by pressing "Ctl+Maj+I"
#include <NMEAGPS.h>
//#include <AltSoftSerial.h>
#include <NeoSWSerial.h>

NMEAGPS gps;
//gps_fix       fix;
bool receivedFix = false;

static const uint32_t GPSBaud = 9600;
//AltSoftSerial gpsPort( 9, 8);  //  only use RXPin = 9, TXPin = 8
NeoSWSerial gpsPort(9, 8);  //  only use RXPin = 9, TXPin = 8


struct message_t {
  uint16_t heading;
  uint16_t roll;
  uint16_t pitch;
  int32_t lat;
  int32_t lon;
  int32_t alt;
  #ifdef POT
  int vol;
  #endif
};
#else
struct message_t {
  uint16_t heading;
  uint16_t roll;
  uint16_t pitch;
  #ifdef POT
  int vol;
  #endif
};


#endif


message_t message;

NAxisMotion mySensor;
unsigned long lastStreamTime = 0;  // the last streamed time stamp
const int streamPeriod = 20;       // stream at 50Hz (time period(ms) =1000/frequency(Hz))

const float twopi = 6.28319;
/* 
double constrainAngle(double x){
    x = fmod(x,360);
    if (x < 0)
        x += 360;
    return x;
} */
double constrainAngle(double x) {
  x = fmod(x, twopi);
  if (x < 0)
    x += twopi;
  return x;
}
void setup() {
  Serial.begin(115200);

#ifdef GPS

  gpsPort.begin(GPSBaud);



#endif

  I2C.begin();

  mySensor.initSensor();  // The I2C Address can be inside this function in the library
  mySensor.setOperationMode(OPERATION_MODE_NDOF);
  mySensor.setUpdateMode(MANUAL);
  // The default is AUTO. Changing to MANUAL requires calling the
  // relevant update functions prior to calling the read functions
  // Setting to MANUAL requires fewer reads to the sensor
}

void loop() {

#ifdef GPS

  while (gps.available(gpsPort)) {
    gps_fix fix = gps.read();

    if (fix.valid.location) {
      message.lat = fix.latitudeL();
      message.lon = fix.longitudeL();
      message.alt = fix.altitude_cm();
    }
    receivedFix = true;
  }

  //if ((millis() > 5000) && !receivedFix) {
  //  Serial.println( F("No GPS detected: check wiring.") );
  //  while(true);
  //}

#endif

  if ((millis() - lastStreamTime) >= streamPeriod) {
    int potVal;  
    lastStreamTime = millis();
    mySensor.updateEuler();

    float headingf = mySensor.readEulerHeading();
    //float headingf = constrainAngle(mySensor.readEulerHeading() - 90);

    headingf = headingf * PI / 180;  // convert heading to radians

    float rollf = mySensor.readEulerRoll();

    rollf = rollf * PI / 180;  // convert to radians

    float pitchf = mySensor.readEulerPitch();

    pitchf = pitchf * PI / 180;  // convert to radians

    #ifdef POT
    potVal = analogRead(potPin) / 3.9;  // read the value from pot
    if (potVal > 255) {
     potVal = 255;
    }
    message.vol = potVal;    
    #endif

#ifdef SIDEWAYS
    // note pitch and roll are swapped below because I rotate z axis of device by 90 degrees
    // message.heading = (headingf + (3 * PI) / 2) * 100;
    message.heading = constrainAngle(headingf - 1.57) * 100;
    message.pitch = (rollf + PI) * 100;
    message.roll = (pitchf + PI) * 100;
#else
    message.heading = (headingf + PI) * 100;
    message.pitch = (pitchf + PI) * 100;
    message.roll = (rollf + PI) * 100;
#endif

#ifdef DEBUG
    Serial.print("Heading = ");
    Serial.print(message.heading);
    Serial.print(". Pitch = ");
    Serial.print(message.pitch);
    //Serial.print(mySensor.readEulerRoll());
    Serial.print(". Roll = ");
#ifdef GPS
    Serial.print(message.roll);
    Serial.print("\t Latitude = ");
    Serial.print(message.lat, 6);
    Serial.print(". Longitude = ");
    Serial.print(message.lon, 6);
    Serial.print(". Altitude = ");
    Serial.println(message.alt, 6);
#else
    //Serial.print(message.roll);
    #ifdef POT
    Serial.print(message.roll);
    Serial.print(". Vol = ");
    Serial.println(message.vol);
    #else
    Serial.println(message.roll);
    #endif
    
#endif
#else
    Serial.write(251);
    Serial.write(252);
    Serial.write(253);
    Serial.write(254);
    Serial.write((uint8_t *)&message, sizeof(message));
    Serial.write(255);
#endif
  }
}
