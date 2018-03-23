package org.thingml.tradfri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.californium.core.CoapResponse;
import org.json.JSONException;
import org.json.JSONObject;

public class TradfriRemotePacket extends TradfriHardwarePacket {

	/**
	 * Logger to be used for all console outputs, errors and exceptions
	 */
	private static final Logger log = LoggerFactory.getLogger(TradfriRemotePacket.class);

	// Status
	private boolean online;

	public TradfriRemotePacket(final int id, final TradfriGateway gateway) {
		super(PACKET_TYPE_REMOTE, id, gateway);
	}

	public TradfriRemotePacket(final int id, final TradfriGateway gateway, final CoapResponse response) {
		super(PACKET_TYPE_REMOTE, id, gateway, response);
	}

	public boolean isOnline() {
		return online;
	}

	protected void updateBulb() {
		CoapResponse response = getGateway().get(TradfriConstants.DEVICES + "/" + getId());
		if (response != null)
			parseResponse(response);
	}

	protected void parseResponse(final CoapResponse response) {
		boolean updateListeners = super.parseResponseBase(response);
		
		try {
			JSONObject json = new JSONObject(response.getResponseText());

			boolean new_online = json.getInt(TradfriConstants.DEVICE_REACHABLE) != 0;
			if (new_online != online)
				updateListeners = true;
			online = new_online;
		} catch (JSONException e) {
			log.error("Cannot update bulb info: error parsing the response from the gateway", e);
		}
	}

	public String toString() {
		String result = "[REMOTE " + getId() + "]";
		if (online)
			result += "\tonline:" + isOnline();
		else
			result += "  ********** OFFLINE *********** ";
		result += "\ttype: " + getType() + "\tname: " + getName();
		return result;
	}
	
}
