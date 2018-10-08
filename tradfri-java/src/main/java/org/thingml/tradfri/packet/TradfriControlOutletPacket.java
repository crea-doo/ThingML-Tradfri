package org.thingml.tradfri.packet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingml.tradfri.TradfriConstants;
import org.thingml.tradfri.TradfriGateway;
import org.thingml.tradfri.listener.TradfriControlOutletListener;
import org.eclipse.californium.core.CoapResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TradfriControlOutletPacket extends TradfriHardwarePacket<TradfriControlOutletListener> {

	/**
	 * Logger to be used for all console outputs, errors and exceptions
	 */
	private static final Logger log = LoggerFactory.getLogger(TradfriControlOutletPacket.class);

	// Status
	private boolean online;

	// State of the control outlet
	private boolean on;

	public TradfriControlOutletPacket(final int id, final TradfriGateway gateway) {
		super(PACKET_TYPE_CONTROL_OUTLET, id, gateway);
	}

	public TradfriControlOutletPacket(final int id, final TradfriGateway gateway, final CoapResponse response) {
		super(PACKET_TYPE_CONTROL_OUTLET, id, gateway, response);
	}

	public boolean isOnline() {
		return online;
	}

	public boolean isOn() {
		return on;
	}

	public void setOn(boolean on) {
		try {
			JSONObject json = new JSONObject();
			JSONObject settings = new JSONObject();
			JSONArray array = new JSONArray();
			array.put(settings);
			json.put(TradfriConstants.LIGHT, array);
			settings.put(TradfriConstants.ONOFF, (on) ? 1 : 0);
			String payload = json.toString();
			getGateway().set(TradfriConstants.DEVICES + "/" + this.getId(), payload);

		} catch (JSONException ex) {
			log.error("Error", ex);
		}
		this.on = on;
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

			final JSONObject control = json.getJSONArray(TradfriConstants.CONTROL).getJSONObject(0);
			if (control.has(TradfriConstants.ONOFF)) {
				final boolean new_on = (control.getInt(TradfriConstants.ONOFF) != 0);
				if (on != new_on)
					updateListeners = true;
				on = new_on;
			} else {
				if (online)
					updateListeners = true;
				online = false;
			}
			
			if (updateListeners) {
				// Notify all listeners
				for (TradfriControlOutletListener listener : listeners) {
					try {
						listener.controlOutletStateChanged(this);
					} catch (Exception ex) {
						//
					}
				}
			}
		}
	}

	public String toString() {
		String result = "[CONTROL_OUUTLET " + getId() + "]";
		if (online)
			result += "\ton:" + on;
		else
			result += "  ********** OFFLINE *********** ";
		result += "\ttype: " + getType() + "\tname: " + getName();
		return result;
	}
	
}
