/*
 * Created by Iain Mott
 * updated by Gaël Jaton
 */
 
// Download and unzip https://github.com/arduino-libraries/NineAxesMotion/archive/master.zip in Arduino home folder
#include <NineAxesMotion.h> 

// The NeoGPS and AltSoftSerial Library can be installed through the Library manager found in the /Tools tab or by pressing "Ctl+Maj+I"
#include <NMEAGPS.h>       
#include <AltSoftSerial.h> 
#include <Wire.h>

NMEAGPS       gps;
gps_fix       fix;
bool          receivedFix = false;
//unsigned long lat = 0;
//unsigned long lng = 0;
long int lat = 0;
long int lng = 0;

struct message_t
{
  uint16_t heading;
  uint16_t roll;
  uint16_t pitch;
  int32_t lat;
  int32_t lon;
};
message_t message;

static const uint32_t GPSBaud = 9600;
AltSoftSerial gpsPort;  //  only use RXPin = 9, TXPin = 8

const unsigned char UBLOX_INIT[] PROGMEM = {
  // Rate (pick one)
  //0xB5,0x62,0x06,0x08,0x06,0x00,0x64,0x00,0x01,0x00,0x01,0x00,0x7A,0x12, //(10Hz)
  0xB5,0x62,0x06,0x08,0x06,0x00,0xC8,0x00,0x01,0x00,0x01,0x00,0xDE,0x6A, //(5Hz)
  //0xB5, 0x62, 0x06, 0x08, 0x06, 0x00, 0xFA, 0x00, 0x01, 0x00, 0x01, 0x00, 0x10, 0x96, // (4Hz)
  //0xB5,0x62,0x06,0x08,0x06,0x00,0xE8,0x03,0x01,0x00,0x01,0x00,0x01,0x39, //(1Hz)

  

  // Disable specific NMEA sentences
  0xB5,0x62,0x06,0x01,0x08,0x00,0xF0,0x00,0x00,0x00,0x00,0x00,0x00,0x01,0x00,0x24, // GxGGA off
  //0xB5,0x62,0x06,0x01,0x08,0x00,0xF0,0x01,0x00,0x00,0x00,0x00,0x00,0x01,0x01,0x2B, // GxGLL off
  0xB5,0x62,0x06,0x01,0x08,0x00,0xF0,0x02,0x00,0x00,0x00,0x00,0x00,0x01,0x02,0x32, // GxGSA off
  //0xB5,0x62,0x06,0x01,0x08,0x00,0xF0,0x03,0x00,0x00,0x00,0x00,0x00,0x01,0x03,0x39, // GxGSV off
  //0xB5,0x62,0x06,0x01,0x08,0x00,0xF0,0x04,0x00,0x00,0x00,0x00,0x00,0x01,0x04,0x40, // GxRMC off
  0xB5,0x62,0x06,0x01,0x08,0x00,0xF0,0x05,0x00,0x00,0x00,0x00,0x00,0x01,0x05,0x47, // GxVTG off
  0xB5,0x62,0x06,0x01,0x08,0x00,0xF0,0x08,0x00,0x00,0x00,0x00,0x00,0x01,0x08,0x5C, // GxZDA off
// turn off warnings
  0x06,0x02,10,0,1,0,0,0,0,0,0,0,0,0
};

NineAxesMotion mySensor;
unsigned long lastStreamTime = 0;     // the last streamed time stamp
const int streamPeriod = 20;          // stream at 50Hz (time period(ms) =1000/frequency(Hz))


void setup()
{
  Serial.begin(115200);
  gpsPort.begin(GPSBaud);

  for (size_t i = 0; i < sizeof(UBLOX_INIT); i++) {                        
    gpsPort.write( pgm_read_byte(UBLOX_INIT+i) );
  };


  I2C.begin();

  mySensor.initSensor();   //The I2C Address can be inside this function in the library
  mySensor.setOperationMode(OPERATION_MODE_NDOF);
  mySensor.setUpdateMode(MANUAL);
    // The default is AUTO. Changing to MANUAL requires calling the 
    // relevant update functions prior to calling the read functions
    // Setting to MANUAL requires fewer reads to the sensor  
}

void loop()
{
  if (gps.available( gpsPort )) {
    fix = gps.read();

    if (fix.valid.location) {
      message.lat = fix.latitudeL();
      message.lon = fix.longitudeL();
    //message.lat = -999999999;
   // message.lon = -000000001;
   //Serial.println("validPOS!");
    }
//Serial.println("top");
    receivedFix = true;
  }

  if ((millis() > 5000) && !receivedFix) {
    Serial.println( F("No GPS detected: check wiring.") );
    while(true);
  }


  if ((millis() - lastStreamTime) >= streamPeriod)
  {
    lastStreamTime = millis();    
    mySensor.updateEuler();

    // correct heading data for 90deg rotation of hardware 
    float headingf = mySensor.readEulerHeading() - 90;  
    if (headingf < 0) {
      headingf = (360 + headingf);
    }
    
    //  adjust data to suit the receiving software
    if (headingf > 180) {
     headingf = -180 + (headingf - 180);
    }
    headingf = headingf * PI / 180; // convert heading to radians

    float rollf = mySensor.readEulerRoll();
    if (rollf > 180) {
     rollf = -180 + (rollf - 180);
    }
    rollf = rollf * PI / 180; // convert to radians

    float pitchf = mySensor.readEulerPitch() * -1;
    if (pitchf > 180) {
      pitchf = -180 + (pitchf - 180);
    }
    pitchf = pitchf * PI / 180; // convert to radians

    // note pitch and roll are swapped below because I rotate z axis of device by 90 degrees 
    
    message.heading = (headingf + PI) * 100;
    message.pitch = (rollf    + PI) * 100; 
    message.roll = (pitchf   + PI) * 100;
    
    Serial.write(251); 
    Serial.write(252); 
    Serial.write(253); 
    Serial.write(254);
    Serial.write( (uint8_t *) &message, sizeof(message) );
    Serial.write(255);    

  }
}
