package org.thingml.tradfri.packet;

import org.eclipse.californium.core.CoapResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.thingml.tradfri.TradfriConstants;
import org.thingml.tradfri.TradfriGateway;
import org.thingml.tradfri.listener.TradfriRemoteListener;

public class TradfriRemotePacket extends TradfriHardwarePacket<TradfriRemoteListener> {

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

	@Override
	public void update() throws JSONException {
		CoapResponse response = getGateway().get(TradfriConstants.DEVICES + "/" + getId());
		if (response != null) {
			boolean updateListeners = super.parseResponseBase(response);
			
			final JSONObject json = new JSONObject(response.getResponseText());

			boolean new_online = json.getInt(TradfriConstants.DEVICE_REACHABLE) != 0;
			if (new_online != online)
				updateListeners = true;
			online = new_online;
			
			if (updateListeners) {
				// Notify all listeners
				for (TradfriRemoteListener listener : listeners) {
					try {
						listener.remoteStateChanged(this);
					} catch (Exception ex) {
						//
					}
				}
			}
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
