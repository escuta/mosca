#!/usr/bin/env python
"""
Adapted from pozyx orientation demo (c) Pozyx Labs
please check out https://www.pozyx.io/Documentation/Tutorials/getting_started/Python

This demo requires one pozyx shields. It demonstrates the 3D orientation and the functionality
to remotely read register data from a pozyx device. Connect one of the Pozyx devices with USB and run this script.

This script reads the following sensor data:
- the heading, roll and pitch
- X Y Z coordinates

The data is sent through osc (port 8888)
"""
from time import time

from pypozyx import EulerAngles, Coordinates, DeviceCoordinates, PozyxConstants, SingleRegister, POZYX_POS_ALG_UWB_ONLY, POZYX_3D, POZYX_SUCCESS, get_first_pozyx_serial_port, PozyxSerial, get_serial_ports

from pypozyx.definitions.bitmasks import POZYX_INT_MASK_IMU
from pythonosc.osc_message_builder import OscMessageBuilder
from pythonosc.udp_client import SimpleUDPClient

from pypozyx.tools.version_check import perform_latest_version_check


class Position3D(object):
    """Reads coordinates and orientation data from a local Pozyx"""

    def __init__(self, pozyx, osc_udp_client, remote_id=None):
        self.pozyx = pozyx        
        
        self.anchors = anchors
        self.algorithm = algorithm
        self.dimension = dimension
        self.height = height

        self.remote_id = remote_id
        self.osc_udp_client = osc_udp_client

    def setup(self):
        """There is no specific setup functionality"""
        self.pozyx.printDeviceInfo(self.remote_id)

        self.current_time = time()

    def loop(self):
        """Gets new IMU sensor data"""
        angles = EulerAngles()
        coordinates = Coordinates()  
        if self.remote_id is not None or self.pozyx.checkForFlag(POZYX_INT_MASK_IMU, 0.01) == POZYX_SUCCESS:
            status = self.pozyx.getEulerAngles_deg(angles, self.remote_id)
            status &= self.pozyx.doPositioning(coordinates, 
                self.dimension, self.height, self.algorithm, remote_id=self.remote_id)
            if status == POZYX_SUCCESS:
                self.publishSensorData(angles, coordinates)

    def publishSensorData(self, angles, coordinates):
        """Makes the OSC sensor data package and publishes it"""
        self.msg_builder = OscMessageBuilder("/position")
        self.addComponentsOSC(angles)
        self.addComponentsOSC(coordinates)
        self.osc_udp_client.send(self.msg_builder.build())

    def addComponentsOSC(self, component):
        """Adds a sensor data component to the OSC message"""
        for data in component.data:
            self.msg_builder.add_arg(float(data))
            
    def setAnchorsManual(self, save_to_flash=False):
        """Adds the manually measured anchors to the Pozyx's device list one for one."""
        status = self.pozyx.clearDevices(remote_id=self.remote_id)
        for anchor in self.anchors:
            status &= self.pozyx.addDevice(anchor, remote_id=self.remote_id)
        if len(self.anchors) > 4:
            status &= self.pozyx.setSelectionOfAnchors(PozyxConstants.ANCHOR_SELECT_AUTO, len(self.anchors), remote_id=self.remote_id)

        if save_to_flash:
            self.pozyx.saveAnchorIds(remote_id=self.remote_id)
            self.pozyx.saveRegisters([PozyxRegisters.POSITIONING_NUMBER_OF_ANCHORS], remote_id=self.remote_id)
        return status

if __name__ == '__main__':
    # shortcut to not have to find out the port yourself
    serial_port = get_first_pozyx_serial_port()
    if serial_port is None:
        print("No Pozyx connected. Check your USB cable or your driver!")
        quit()

    # remote device network ID
    remote_id = 0x6e66
    # whether to use a remote device. If on False, remote_id is set to None which means the local device is used
    remote = False
    if not remote:
        remote_id = None

    # configure if you want to route OSC to outside your localhost. Networking knowledge is required.
    ip = "127.0.0.1"
    network_port = 8888
    
    # necessary data for calibration, change the IDs and coordinates yourself according to your measurement
    anchors = [DeviceCoordinates(0x6F40, 1, Coordinates(0, 0, 250)),
               DeviceCoordinates(0x6F4B, 1, Coordinates(0, 6330, 2750)),
               DeviceCoordinates(0x6F55, 1, Coordinates(9400, 5640, 250)),
               DeviceCoordinates(0x6F5B, 1, Coordinates(9400, 0, 2750))]

    # positioning algorithm to use, other is PozyxConstants.POSITIONING_ALGORITHM_TRACKING
    algorithm = PozyxConstants.POSITIONING_ALGORITHM_UWB_ONLY
    # positioning dimension. Others are PozyxConstants.DIMENSION_2D, PozyxConstants.DIMENSION_2_5D
    dimension = PozyxConstants.DIMENSION_3D
    # height of device, required in 2.5D positioning
    height = 1750   

    pozyx = PozyxSerial(serial_port)
    osc_udp_client = SimpleUDPClient(ip, network_port)

    position_3d = Position3D(pozyx, osc_udp_client, remote_id=remote_id)
    position_3d.setup()
    print("Set up 3D positioning")
    while True:
        position_3d.loop()
