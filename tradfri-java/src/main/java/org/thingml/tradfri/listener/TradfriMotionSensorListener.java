package org.thingml.tradfri.listener;

import org.thingml.tradfri.packet.TradfriMotionSensorPacket;

public interface TradfriMotionSensorListener {

	public void motionSensorStateChanged(TradfriMotionSensorPacket motionSensor);

}
