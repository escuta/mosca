/****************************************************************************
* Copyright (C) 2011 - 2014 Bosch Sensortec GmbH
*
* NAxisMotion.h
* Date: 2015/02/10
* Revision: 3.0 $
*
* Usage:        Header file of the C++ Wrapper for the BNO055 Sensor API
*
****************************************************************************
*
* Added Arduino M0/M0 Pro support
*
* Date: 07/27/2015
*
* Modified by: Arduino.org development Team.
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
#ifndef __NAXISMOTION_H__
#define __NAXISMOTION_H__




extern "C" {
#include "BNO055.h"
}
#include <Wire.h>
#include "Arduino.h"

//Custom Data structures
//Structure to hold the calibration status
struct bno055_calib_stat_t {
	uint8_t accel;	//Calibration Status of the accelerometer
	uint8_t mag;	//Calibration Status of the magnetometer
	uint8_t gyro;	//Calibration Status of the gyroscope
	uint8_t system;	//Calibration Status of the overall system
};

//Structure to hold the accelerometer configurations
struct bno055_accel_stat_t {
	uint8_t range;		//Range: 2G - 16G
	uint8_t bandwidth;	//Bandwidth: 7.81Hz - 1000Hz
	uint8_t powerMode;	//Power mode: Normal - Deep suspend
};

//GPIO pins used for controlling the Sensor
#define RESET_PIN		4		//GPIO to reset the BNO055 (RESET pin has to be HIGH for the BNO055 to operate)

#if defined(__AVR_ATmega32U4__) //Arduino Yun and Leonardo
#define INT_PIN			4		//GPIO to receive the Interrupt from the BNO055 for the Arduino Uno(Interrupt is visible on the INT LED on the Shield)
#elif defined(ARDUINO_ARCH_SAM)   //INT_PIN is the interrupt number not the interrupt pin
#define INT_PIN			2   
#elif defined(ARDUINO_ARCH_SAMD)
#define INT_PIN 		7  
#else
#define INT_PIN			0
#endif

#define ENABLE			1		//For use in function parameters
#define DISABLE			0		//For use in function parameters
#define NO_MOTION		1		//Enables the no motion interrupt
#define SLOW_MOTION		0		//Enables the slow motion interrupt
#define ANDROID			1		//To set the Output Data Format to Android style

#if defined(ARDUINO_SAM_DUE)
#define I2C				Wire1	//Define which I2C bus is used. Wire1 for the Arduino Due
#else
#define I2C             Wire    //Or Wire
#endif

#define INIT_PERIOD			600		//Initialization period set to 600ms
#define RESET_PERIOD		300		//Reset period set to 300ms
#define POST_INIT_PERIOD	50		//Post initialization delay of 50ms
#define MANUAL				1		//To manually call the update data functions
#define AUTO				0		//To automatically call the update data functions
class NAxisMotion {
private:
	bool 	dataUpdateMode;								//Variable to store the mode of updating data
	struct 	bno055_t 			myBNO;					//Structure that stores the device information
	struct 	bno055_accel_float_t	accelData;				//Structure that holds the accelerometer data
	struct 	bno055_mag_float_t	magData;				//Structure that holds the magnetometer data
	struct 	bno055_gyro_float_t 	gyroData;				//Structure that holds the gyroscope data
	struct 	bno055_quaternion_t	quatData;				//Structure that holds the quaternion data
	struct 	bno055_euler_float_t	eulerData;				//Structure that holds the euler data
	struct 	bno055_linear_accel_float_t	linearAccelData;	//Structure that holds the linear acceleration data
	struct 	bno055_gravity_float_t	gravAccelData;			//Structure that holds the gravity acceleration data
	struct 	bno055_calib_stat_t	calibStatus;			//Structure to hold the calibration status
	struct 	bno055_accel_stat_t	accelStatus;			//Structure to hold the status of the accelerometer configurations
public:
	//Function Declarations
	/*******************************************************************************************
	*Description: Constructor of the class with the default initialization
	*Input Parameters: None
	*Return Parameter: None
	*******************************************************************************************/
	NAxisMotion();

	/*******************************************************************************************
	*Description: Function with the bare minimum initialization
	*Input Parameters: None
	*Return Parameter: None
	*******************************************************************************************/
	void initSensor(unsigned int address = 0x28);

	/*******************************************************************************************
	*Description: This function is used to reset the BNO055
	*Input Parameters: None
	*Return Parameter: None
	*******************************************************************************************/
	void resetSensor(unsigned int address);

	/*******************************************************************************************
	*Description: This function is used to set the operation mode of the BNO055
	*Input Parameters:
	*	byte operationMode: To assign which operation mode the device has to
	*				---------------------------------------------------
	*				Constant Definition			Constant Value	Comment
	*				---------------------------------------------------
	*				OPERATION_MODE_CONFIG		0x00			Configuration Mode
	*																(Transient Mode)
	*				OPERATION_MODE_ACCONLY		0x01			Accelerometer only
	*				OPERATION_MODE_MAGONLY		0x02			Magnetometer only
	*				OPERATION_MODE_GYRONLY		0x03			Gyroscope only
	*				OPERATION_MODE_ACCMAG		0x04			Accelerometer and Magnetometer only
	*				OPERATION_MODE_ACCGYRO		0x05			Accelerometer and Gyroscope only
	*				OPERATION_MODE_MAGGYRO		0x06			Magnetometer and Gyroscope only
	*				OPERATION_MODE_AMG			0x07			Accelerometer, Magnetometer and
	*																Gyroscope (without fusion)
	*				OPERATION_MODE_IMUPLUS		0x08			Inertial Measurement Unit
	*																(Accelerometer and Gyroscope
	*																	Sensor Fusion Mode)
	*				OPERATION_MODE_COMPASS		0x09			Tilt Compensated Compass
	*																(Accelerometer and Magnetometer
	*																	Sensor Fusion Mode)
	*				OPERATION_MODE_M4G			0x0A			Magnetometer and Accelerometer Sensor
	*																Fusion Mode
	*				OPERATION_MODE_NDOF_FMC_OFF	0x0B			9 Degrees of Freedom Sensor Fusion
	*																with Fast Magnetometer Calibration Off
	*				OPERATION_MODE_NDOF			0x0C			9 Degrees of Freedom Sensor Fusion
	*Return Parameter: None
	*******************************************************************************************/
	void setOperationMode(byte operationMode);

	/*******************************************************************************************
	*Description: This function is used to set the power mode
	*Input Parameters:
	*	byte powerMode: To assign the power mode the device has to switch to
	*				--------------------------------------
	*				Constant Definition		Constant Value
	*				--------------------------------------
	*				POWER_MODE_NORMAL		0x00
	*				POWER_MODE_LOWPOWER		0x01
	*				POWER_MODE_SUSPEND		0x02
	*Return Parameter:
	*******************************************************************************************/
	void setPowerMode(byte powerMode);

	/*******************************************************************************************
	*Description: This function is used to update the accelerometer data in m/s2
	*Input Parameters: None
	*Return Parameter: None
	*******************************************************************************************/
	void updateAccel(void);

	/*******************************************************************************************
	*Description: This function is used to update the magnetometer data in microTesla
	*Input Parameters: None
	*Return Parameter: None
	*******************************************************************************************/
	void updateMag(void);

	/*******************************************************************************************
	*Description: This function is used to update the gyroscope data in deg/s
	*Input Parameters: None
	*Return Parameter: None
	*******************************************************************************************/
	void updateGyro(void);

	/*******************************************************************************************
	*Description: This function is used to update the quaternion data
	*Input Parameters: None
	*Return Parameter: None
	*******************************************************************************************/
	void updateQuat(void);

	/*******************************************************************************************
	*Description: This function is used to update the euler data
	*Input Parameters: None
	*Return Parameter: None
	*******************************************************************************************/
	void updateEuler(void);

	/*******************************************************************************************
	*Description: This function is used to update the linear acceleration data in m/s2
	*Input Parameters: None
	*Return Parameter: None
	*******************************************************************************************/
	void updateLinearAccel(void);

	/*******************************************************************************************
	*Description: This function is used to update the gravity acceleration data in m/s2
	*Input Parameters: None
	*Return Parameter: None
	*******************************************************************************************/
	void updateGravAccel(void);

	/*******************************************************************************************
	*Description: This function is used to update the calibration status
	*Input Parameters: None
	*Return Parameter: None
	*******************************************************************************************/
	void updateCalibStatus(void);

	/*******************************************************************************************
	*Description: This function is used to write the accelerometer configurations
	*Input Parameters:
	*	uint8_t range: To assign the range of the accelerometer
	*			--------------------------------------
	*			Constant Definition		Constant Value
	*			--------------------------------------
	*			ACCEL_RANGE_2G			0X00
	*			ACCEL_RANGE_4G			0X01
	*			ACCEL_RANGE_8G			0X02
	*			ACCEL_RANGE_16G			0X03
	*	uint8_t bandwidth: To assign the filter bandwidth of the accelerometer
	*			--------------------------------------
	*			Constant Definition		Constant Value
	*			--------------------------------------
	*			ACCEL_BW_7_81HZ			0x00
	*			ACCEL_BW_15_63HZ		0x01
	*			ACCEL_BW_31_25HZ		0x02
	*			ACCEL_BW_62_5HZ			0X03
	*			ACCEL_BW_125HZ			0X04
	*			ACCEL_BW_250HZ			0X05
	*			ACCEL_BW_500HZ			0X06
	*			ACCEL_BW_1000HZ			0X07
	*	uint8_t powerMode: To assign the power mode of the accelerometer
	*			--------------------------------------
	*			Constant Definition		Constant Value
	*			--------------------------------------
	*			ACCEL_NORMAL			0X00
	*			ACCEL_SUSPEND			0X01
	*			ACCEL_LOWPOWER_1		0X02
	*			ACCEL_STANDBY			0X03
	*			ACCEL_LOWPOWER_2		0X04
	*			ACCEL_DEEPSUSPEND		0X05
	*Return Parameter: None
	*******************************************************************************************/
	void writeAccelConfig(uint8_t range, uint8_t bandwidth, uint8_t powerMode);

	/*******************************************************************************************
	*Description: This function is used to update the accelerometer configurations
	*Input Parameters: None
	*Return Parameter: None
	*******************************************************************************************/
	void updateAccelConfig(void);

	/*******************************************************************************************
	*Description: This function is used to control which axis of the accelerometer triggers the
	*				interrupt
	*Input Parameters:
	*	bool xStatus: To know whether the x axis has to trigger the interrupt
	*				---------------------------------------------------
	*				Constant Definition		Constant Value	Comment
	*				---------------------------------------------------
	*				ENABLE					1				Enables interrupts from that axis
	*				DISABLE					0				Disables interrupts from that axis
	*	bool yStatus: To know whether the x axis has to trigger the interrupt
	*				---------------------------------------------------
	*				Constant Definition		Constant Value	Comment
	*				---------------------------------------------------
	*				ENABLE					1				Enables interrupts from that axis
	*				DISABLE					0				Disables interrupts from that axis
	*	bool zStatus: To know whether the x axis has to trigger the interrupt
	*				---------------------------------------------------
	*				Constant Definition		Constant Value	Comment
	*				---------------------------------------------------
	*				ENABLE					1				Enables interrupts from that axis
	*				DISABLE					0				Disables interrupts from that axis
	*Return Parameter: None
	*******************************************************************************************/
	void accelInterrupts(bool xStatus, bool yStatus, bool zStatus);

	/*******************************************************************************************
	*Description: This function is used to reset the interrupt line
	*Input Parameters: None
	*Return Parameter: None
	*******************************************************************************************/
	void resetInterrupt(void);

	/*******************************************************************************************
	*Description: This function is used to enable the any motion interrupt based on the
	*				accelerometer
	*Input Parameters:
	*	uint8_t threshold: The threshold that triggers the any motion interrupt
	*				The threshold should be entered as an integer. The corresponding value of
	*					the threshold depends on the range that has been set on the
	*					accelerometer. Below is a table showing the value of 1LSB in
	*					corresponding units.
	*				Resolution:
	*					ACCEL_RANGE_2G, 1LSB = 3.91mg = ~0.03835m/s2
	*					ACCEL_RANGE_4G, 1LSB = 7.81mg = ~0.07661m/s2
	*					ACCEL_RANGE_8G, 1LSB = 15.6mg = ~0.15303m/s2
	*					ACCEL_RANGE_16G, 1LSB = 31.3mg = ~0.30705m/s2
	*				Maximum:
	*					ACCEL_RANGE_2G, 1LSB = 996mg = ~9.77076m/s2,
	*					ACCEL_RANGE_4G, 1LSB = 1.99g = ~19.5219m/s2
	*					ACCEL_RANGE_8G, 1LSB = 3.98g = ~39.0438m/s2
	*					ACCEL_RANGE_16G, 1LSB = 7.97g = ~97.1857m/s2
	*	uint8_t duration: The duration for which the desired threshold exist
	*				The time difference between the successive acceleration signals depends
	*				on the selected bandwidth and equates to 1/(2*bandwidth).
	*				In order to suppress false triggers, the interrupt is only generated (cleared)
	*				if a certain number N of consecutive slope data points is larger (smaller)
	*				than the slope 'threshold'. This number is set by the 'duration'.
	*				It is N = duration + 1.
	*				Resolution:
	*					ACCEL_BW_7_81HZ, 1LSB = 64ms
	*					ACCEL_BW_15_63HZ, 1LSB = 32ms
	*					ACCEL_BW_31_25HZ, 1LSB = 16ms
	*					ACCEL_BW_62_5HZ, 1LSB = 8ms
	*					ACCEL_BW_125HZ, 1LSB = 4ms
	*					ACCEL_BW_250HZ, 1LSB = 2ms
	*					ACCEL_BW_500HZ, 1LSB = 1ms
	*					ACCEL_BW_1000HZ, 1LSB = 0.5ms
	*Return Parameter: None
	*******************************************************************************************/
	void enableAnyMotion(uint8_t threshold, uint8_t duration);

	/*******************************************************************************************
	*Description: This function is used to disable the any motion interrupt
	*Input Parameters: None
	*Return Parameter: None
	*******************************************************************************************/
	void disableAnyMotion(void);


	/*******************************************************************************************
	*Description: This function is used to enable the slow or no motion interrupt based on the
	*				accelerometer
	*Input Parameters:
	*	uint8_t threshold: The threshold that triggers the no motion interrupt
	*				The threshold should be entered as an integer. The corresponding value of
	*					the threshold depends on the range that has been set on the
	*					accelerometer. Below is a table showing the value of 1LSB in
	*					corresponding units.
	*				Resolution:
	*					ACCEL_RANGE_2G, 1LSB = 3.91mg = ~0.03835m/s2
	*					ACCEL_RANGE_4G, 1LSB = 7.81mg = ~0.07661m/s2
	*					ACCEL_RANGE_8G, 1LSB = 15.6mg = ~0.15303m/s2
	*					ACCEL_RANGE_16G, 1LSB = 31.3mg = ~0.30705m/s2
	*				Maximum:
	*					ACCEL_RANGE_2G, 1LSB = 996mg = ~9.77076m/s2,
	*					ACCEL_RANGE_4G, 1LSB = 1.99g = ~19.5219m/s2
	*					ACCEL_RANGE_8G, 1LSB = 3.98g = ~39.0438m/s2
	*					ACCEL_RANGE_16G, 1LSB = 7.97g = ~97.1857m/s2
	*	uint8_t duration: The duration for which the desired threshold should be surpassed
	*				The time difference between the successive acceleration signals depends
	*				on the selected bandwidth and equates to 1/(2*bandwidth).
	*				In order to suppress false triggers, the interrupt is only generated (cleared)
	*				if a certain number N of consecutive slope data points is larger (smaller)
	*				than the slope 'threshold'. This number is set by the 'duration'.
	*				It is N = duration + 1.
	*				Resolution:
	*					ACCEL_BW_7_81HZ, 1LSB = 64ms
	*					ACCEL_BW_15_63HZ, 1LSB = 32ms
	*					ACCEL_BW_31_25HZ, 1LSB = 16ms
	*					ACCEL_BW_62_5HZ, 1LSB = 8ms
	*					ACCEL_BW_125HZ, 1LSB = 4ms
	*					ACCEL_BW_250HZ, 1LSB = 2ms
	*					ACCEL_BW_500HZ, 1LSB = 1ms
	*					ACCEL_BW_1000HZ, 1LSB = 0.5ms
	*	bool motion: To trigger either a Slow motion or a No motion interrupt
	*				---------------------------------------------------
	*				Constant Definition		Constant Value	Comment
	*				---------------------------------------------------
	*				NO_MOTION				1				Enables the no motion interrupt
	*				SLOW_MOTION				0				Enables the slow motion interrupt
	*Return Parameter: None
	*******************************************************************************************/
	void enableSlowNoMotion(uint8_t threshold, uint8_t duration, bool motion);

	/*******************************************************************************************
	*Description: This function is used to disable the slow or no motion interrupt
	*Input Parameters: None
	*Return Parameter: None
	*******************************************************************************************/
	void disableSlowNoMotion(void);

	/*******************************************************************************************
	*Description: This function is used to change the mode of updating the local data
	*Input Parameters: None
	*Return Parameter: None
	*******************************************************************************************/
	void setUpdateMode(bool updateMode);

	/*******************************************************************************************
	*Description: This function is used to return the x-axis of the accelerometer data
	*Input Parameters: None
	*Return Parameter:
	*	float:	X-axis accelerometer data in m/s2
	*******************************************************************************************/
	float readAccelX(void);

	/*******************************************************************************************
	*Description: This function is used to return the y-axis of the accelerometer data
	*Input Parameters: None
	*Return Parameter:
	*	float:	Y-axis accelerometer data in m/s2
	*******************************************************************************************/
	float readAccelY(void);

	/*******************************************************************************************
	*Description: This function is used to return the z-axis of the accelerometer data
	*Input Parameters: None
	*Return Parameter:
	*	float:	Z-axis accelerometer data in m/s2
	*******************************************************************************************/
	float readAccelZ(void);

	/*******************************************************************************************
	*Description: This function is used to return the x-axis of the gyroscope data
	*Input Parameters: None
	*Return Parameter:
	*	float:	X-axis gyroscope data in deg/s
	*******************************************************************************************/
	float readGyroX(void);

	/*******************************************************************************************
	*Description: This function is used to return the y-axis of the gyroscope data
	*Input Parameters: None
	*Return Parameter:
	*	float:	Y-axis gyroscope data in deg/s
	*******************************************************************************************/
	float readGyroY(void);

	/*******************************************************************************************
	*Description: This function is used to return the z-axis of the gyroscope data
	*Input Parameters: None
	*Return Parameter:
	*	float:	Z-axis gyroscope data in deg/s
	*******************************************************************************************/
	float readGyroZ(void);

	/*******************************************************************************************
	*Description: This function is used to return the x-axis of the magnetometer data
	*Input Parameters: None
	*Return Parameter:
	*	float:	X-axis magnetometer data in �T
	*******************************************************************************************/
	float readMagX(void);

	/*******************************************************************************************
	*Description: This function is used to return the y-axis of the magnetometer data
	*Input Parameters: None
	*Return Parameter:
	*	float:	Y-axis magnetometer data in �T
	*******************************************************************************************/
	float readMagY(void);

	/*******************************************************************************************
	*Description: This function is used to return the z-axis of the magnetometer data
	*Input Parameters: None
	*Return Parameter:
	*	float:	Z-axis magnetometer data in �T
	*******************************************************************************************/
	float readMagZ(void);

	/*******************************************************************************************
	*Description: This function is used to return the w-axis of the quaternion data
	*Input Parameters: None
	*Return Parameter:
	*	int16_t:	W-axis quaternion data multiplied by 1000 (for 3 decimal places accuracy)
	*******************************************************************************************/
	int16_t readQuatW(void);

	/*******************************************************************************************
	*Description: This function is used to return the x-axis of the quaternion data
	*Input Parameters: None
	*Return Parameter:
	*	int16_t:	X-axis quaternion data multiplied by 1000 (for 3 decimal places accuracy)
	*******************************************************************************************/
	int16_t readQuatX(void);

	/*******************************************************************************************
	*Description: This function is used to return the y-axis of the quaternion data
	*Input Parameters: None
	*Return Parameter:
	*	int16_t:	Y-axis quaternion data multiplied by 1000 (for 3 decimal places accuracy)
	*******************************************************************************************/
	int16_t readQuatY(void);

	/*******************************************************************************************
	*Description: This function is used to return the z-axis of the quaternion data
	*Input Parameters: None
	*Return Parameter:
	*	int16_t:	Z-axis quaternion data multiplied by 1000 (for 3 decimal places accuracy)
	*******************************************************************************************/
	int16_t readQuatZ(void);

	/*******************************************************************************************
	*Description: This function is used to return the heading(yaw) of the euler data
	*Input Parameters: None
	*Return Parameter:
	*	float:	Heading of the euler data
	*******************************************************************************************/
	float readEulerHeading(void);

	/*******************************************************************************************
	*Description: This function is used to return the roll of the euler data
	*Input Parameters: None
	*Return Parameter:
	*	float:	Roll of the euler data
	*******************************************************************************************/
	float readEulerRoll(void);

	/*******************************************************************************************
	*Description: This function is used to return the pitch of the euler data
	*Input Parameters: None
	*Return Parameter:
	*	float:	Pitch of the euler data
	*******************************************************************************************/
	float readEulerPitch(void);

	/*******************************************************************************************
	*Description: This function is used to return the x-axis of the linear acceleration data
	*					(accelerometer data without the gravity vector)
	*Input Parameters: None
	*Return Parameter:
	*	float:	X-axis Linear Acceleration data in m/s2
	*******************************************************************************************/
	float readLinearAccelX(void);

	/*******************************************************************************************
	*Description: This function is used to return the y-axis of the linear acceleration data
	*					(accelerometer data without the gravity vector)
	*Input Parameters: None
	*Return Parameter:
	*	float:	Y-axis Linear Acceleration data in m/s2
	*******************************************************************************************/
	float readLinearAccelY(void);

	/*******************************************************************************************
	*Description: This function is used to return the z-axis of the linear acceleration data
	*					(accelerometer data without the gravity vector)
	*Input Parameters: None
	*Return Parameter:
	*	float:	Z-axis Linear Acceleration data in m/s2
	*******************************************************************************************/
	float readLinearAccelZ(void);

	/*******************************************************************************************
	*Description: This function is used to return the x-axis of the gravity acceleration data
	*					(accelerometer data with only the gravity vector)
	*Input Parameters: None
	*Return Parameter:
	*	float:	X-axis Gravity Acceleration data in m/s2
	*******************************************************************************************/
	float readGravAccelX(void);

	/*******************************************************************************************
	*Description: This function is used to return the y-axis of the gravity acceleration data
	*					(accelerometer data with only the gravity vector)
	*Input Parameters: None
	*Return Parameter:
	*	float:	Y-axis Gravity Acceleration data in m/s2
	*******************************************************************************************/
	float readGravAccelY(void);

	/*******************************************************************************************
	*Description: This function is used to return the z-axis of the gravity acceleration data
	*					(accelerometer data with only the gravity vector)
	*Input Parameters: None
	*Return Parameter:
	*	float:	Z-axis Gravity Acceleration data in m/s2
	*******************************************************************************************/
	float readGravAccelZ(void);

	/*******************************************************************************************
	*Description: This function is used to return the accelerometer calibration status
	*Input Parameters: None
	*Return Parameter:
	*	uint8_t:	Accelerometer calibration status, 0-3 (0 - low, 3 - high)
	*******************************************************************************************/
	uint8_t readAccelCalibStatus(void);

	/*******************************************************************************************
	*Description: This function is used to return the gyroscope calibration status
	*Input Parameters: None
	*Return Parameter:
	*	uint8_t:	Gyroscope calibration status, 0-3 (0 - low, 3 - high)
	*******************************************************************************************/
	uint8_t readGyroCalibStatus(void);

	/*******************************************************************************************
	*Description: This function is used to return the magnetometer calibration status
	*Input Parameters: None
	*Return Parameter:
	*	uint8_t:	Magnetometer calibration status, 0-3 (0 - low, 3 - high)
	*******************************************************************************************/
	uint8_t readMagCalibStatus(void);

	/*******************************************************************************************
	*Description: This function is used to return the system calibration status
	*Input Parameters: None
	*Return Parameter:
	*	uint8_t:	System calibration status, 0-3 (0 - low, 3 - high)
	*******************************************************************************************/
	uint8_t readSystemCalibStatus(void);

	/*******************************************************************************************
	*Description: This function is used to return the accelerometer range
	*Input Parameters: None
	*Return Parameter:
	*	uint8_t range: Range of the accelerometer
	*			--------------------------------------
	*			Constant Definition		Constant Value
	*			--------------------------------------
	*			ACCEL_RANGE_2G			0X00
	*			ACCEL_RANGE_4G			0X01
	*			ACCEL_RANGE_8G			0X02
	*			ACCEL_RANGE_16G			0X03
	*******************************************************************************************/
	uint8_t readAccelRange(void);

	/*******************************************************************************************
	*Description: This function is used to return the accelerometer bandwidth
	*Input Parameters: None
	*Return Parameter:
	*	uint8_t bandwidth: Bandwidth of the accelerometer
	*			--------------------------------------
	*			Constant Definition		Constant Value
	*			--------------------------------------
	*			ACCEL_BW_7_81HZ			0x00
	*			ACCEL_BW_15_63HZ		0x01
	*			ACCEL_BW_31_25HZ		0x02
	*			ACCEL_BW_62_5HZ			0X03
	*			ACCEL_BW_125HZ			0X04
	*			ACCEL_BW_250HZ			0X05
	*			ACCEL_BW_500HZ			0X06
	*			ACCEL_BW_1000HZ			0X07
	*******************************************************************************************/
	uint8_t readAccelBandwidth(void);

	/*******************************************************************************************
	*Description: This function is used to return the accelerometer power mode
	*Input Parameters: None
	*Return Parameter:
	*	uint8_t powerMode: Power mode of the accelerometer
	*			--------------------------------------
	*			Constant Definition		Constant Value
	*			--------------------------------------
	*			ACCEL_NORMAL			0X00
	*			ACCEL_SUSPEND			0X01
	*			ACCEL_LOWPOWER_1		0X02
	*			ACCEL_STANDBY			0X03
	*			ACCEL_LOWPOWER_2		0X04
	*			ACCEL_DEEPSUSPEND		0X05
	*******************************************************************************************/
	uint8_t readAccelPowerMode(void);



};

/******************** Bridge Functions for the Sensor API to control the Arduino Hardware******************************************/
signed char BNO055_I2C_bus_read(unsigned char,unsigned char, unsigned char*, unsigned char);
signed char BNO055_I2C_bus_write(unsigned char ,unsigned char , unsigned char* , unsigned char );
void _delay(u_32);

#endif __NAXISMOTION_H__
