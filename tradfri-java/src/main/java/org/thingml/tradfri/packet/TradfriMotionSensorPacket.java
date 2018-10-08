package org.thingml.tradfri.packet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingml.tradfri.TradfriConstants;
import org.thingml.tradfri.TradfriGateway;
import org.thingml.tradfri.listener.TradfriMotionSensorListener;
import org.eclipse.californium.core.CoapResponse;
import org.json.JSONException;
import org.json.JSONObject;

public class TradfriMotionSensorPacket extends TradfriHardwarePacket<TradfriMotionSensorListener> {

	/**
	 * Logger to be used for all console outputs, errors and exceptions
	 */
	private static final Logger log = LoggerFactory.getLogger(TradfriMotionSensorPacket.class);

	// Status
	private boolean online;

	public TradfriMotionSensorPacket(final int id, final TradfriGateway gateway) {
		super(PACKET_TYPE_MOTION_SENSOR, id, gateway);
	}

	public TradfriMotionSensorPacket(final int id, final TradfriGateway gateway, final CoapResponse response) {
		super(PACKET_TYPE_MOTION_SENSOR, id, gateway, response);
	}

	public boolean isOnline() {
		return online;
	}

	@Override
	public void update() throws JSONException {
		CoapResponse response = getGateway().get(TradfriConstants.DEVICES + "/" + getId());
		if (response != null) {
			boolean updateListeners = super.parseResponseBase(response);
			
			final JSONObject json = new JSONObject(response.getResponseText());

			final boolean new_online = json.getInt(TradfriConstants.DEVICE_REACHABLE) != 0;
			if (new_online != online)
				updateListeners = true;
			online = new_online;
			
			if (updateListeners) {
				// Notify all listeners
				for (TradfriMotionSensorListener listener : listeners) {
					try {
						listener.motionSensorStateChanged(this);
					} catch (Exception ex) {
						//
					}
				}
			}
		}
	}

	public String toString() {
		String result = "[MOTION_SENSOR " + getId() + "]";
		if (online)
			result += "\tonline:" + isOnline();
		else
			result += "  ********** OFFLINE *********** ";
		result += "\ttype: " + getType() + "\tname: " + getName();
		return result;
	}
	
}
