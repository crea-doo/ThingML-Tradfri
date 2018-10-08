package org.thingml.tradfri.packet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.californium.core.CoapResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.thingml.tradfri.TradfriConstants;
import org.thingml.tradfri.TradfriGateway;
import org.thingml.tradfri.listener.TradfriLightBulbListener;

public class TradfriLightBulbPacket extends TradfriHardwarePacket<TradfriLightBulbListener> {

	/**
	 * Logger to be used for all console outputs, errors and exceptions
	 */
	private static final Logger log = LoggerFactory.getLogger(TradfriLightBulbPacket.class);

	// Status
	private boolean online;

	// State of the light bulb
	private boolean on;
	private int intensity;
	private String color;

	public TradfriLightBulbPacket(final int id, final TradfriGateway gateway) {
		super(PACKET_TYPE_LIGHT_BULB, id, gateway);
	}

	public TradfriLightBulbPacket(final int id, final TradfriGateway gateway, final CoapResponse response) {
		super(PACKET_TYPE_LIGHT_BULB, id, gateway, response);
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

	public int getIntensity() {
		return intensity;
	}

	public void setIntensity(int intensity) {
		try {
			JSONObject json = new JSONObject();
			JSONObject settings = new JSONObject();
			JSONArray array = new JSONArray();
			array.put(settings);
			json.put(TradfriConstants.LIGHT, array);
			settings.put(TradfriConstants.DIMMER, intensity);
			settings.put(TradfriConstants.TRANSITION_TIME, 5);
			String payload = json.toString();
			getGateway().set(TradfriConstants.DEVICES + "/" + this.getId(), payload);

		} catch (JSONException ex) {
			log.error("Error", ex);
		}
		this.intensity = intensity;
	}

	public String getColor() {
		return color;
	}

	public void setRGBColor(int r, int g, int b) {
		double red = r;
		double green = g;
		double blue = b;

		// gamma correction
		red = (red > 0.04045) ? Math.pow((red + 0.055) / (1.0 + 0.055), 2.4) : (red / 12.92);
		green = (green > 0.04045) ? Math.pow((green + 0.055) / (1.0 + 0.055), 2.4) : (green / 12.92);
		blue = (blue > 0.04045) ? Math.pow((blue + 0.055) / (1.0 + 0.055), 2.4) : (blue / 12.92);

		// Wide RGB D65 conversion
		// math inspiration:
		// http://www.brucelindbloom.com/index.html?Eqn_RGB_XYZ_Matrix.html
		double X = red * 0.664511 + green * 0.154324 + blue * 0.162028;
		double Y = red * 0.283881 + green * 0.668433 + blue * 0.047685;
		double Z = red * 0.000088 + green * 0.072310 + blue * 0.986039;

		// calculate the xy values from XYZ
		double x = (X / (X + Y + Z));
		double y = (Y / (X + Y + Z));

		int xyX = (int) (x * 65535 + 0.5);
		int xyY = (int) (y * 65535 + 0.5);

		try {
			JSONObject json = new JSONObject();
			JSONObject settings = new JSONObject();
			JSONArray array = new JSONArray();
			array.put(settings);
			json.put(TradfriConstants.LIGHT, array);
			settings.put(TradfriConstants.COLOR_X, xyX);
			settings.put(TradfriConstants.COLOR_Y, xyY);
			settings.put(TradfriConstants.TRANSITION_TIME, 5);
			String payload = json.toString();
			getGateway().set(TradfriConstants.DEVICES + "/" + this.getId(), payload);

		} catch (JSONException ex) {
			log.error("Error", ex);
		}

	}

	public void setColor(String color) {
		try {
			JSONObject json = new JSONObject();
			JSONObject settings = new JSONObject();
			JSONArray array = new JSONArray();
			array.put(settings);
			json.put(TradfriConstants.LIGHT, array);
			settings.put(TradfriConstants.COLOR, color);
			settings.put(TradfriConstants.TRANSITION_TIME, 5);
			String payload = json.toString();
			getGateway().set(TradfriConstants.DEVICES + "/" + this.getId(), payload);

		} catch (JSONException ex) {
			log.error("Error", ex);
		}
		this.color = color;
	}

	@Override
	public void update() throws JSONException {
		final CoapResponse response = getGateway().get(TradfriConstants.DEVICES + "/" + getId());
		if (response != null) {
			boolean updateListeners = super.parseResponseBase(response);
			
			final JSONObject json = new JSONObject(response.getResponseText());

			final boolean new_online = json.getInt(TradfriConstants.DEVICE_REACHABLE) != 0;
			if (new_online != online)
				updateListeners = true;
			online = new_online;

			final JSONObject light = json.getJSONArray(TradfriConstants.LIGHT).getJSONObject(0);
			if (light.has(TradfriConstants.ONOFF) && light.has(TradfriConstants.DIMMER)) {
				final boolean new_on = (light.getInt(TradfriConstants.ONOFF) != 0);
				final int new_intensity = light.getInt(TradfriConstants.DIMMER);
				if (on != new_on)
					updateListeners = true;
				if (intensity != new_intensity)
					updateListeners = true;
				on = new_on;
				intensity = new_intensity;
			} else {
				if (online)
					updateListeners = true;
				online = false;
			}
			if (light.has(TradfriConstants.COLOR)) {
				String new_color = light.getString(TradfriConstants.COLOR);
				if (color == null || !color.equals(new_color))
					updateListeners = true;
				color = new_color;
			}
			
			if (updateListeners) {
				// Notify all listeners
				for (TradfriLightBulbListener listener : listeners) {
					try {
						listener.bulbStateChanged(this);
					} catch (Exception ex) {
						//
					}
				}
			}
		}
	}

	public String toString() {
		String result = "[LIGHT_BULB " + getId() + "]";
		if (online)
			result += "\ton:" + on + "\tdim:" + intensity + "\tcolor:" + color;
		else
			result += "  ********** OFFLINE *********** ";
		result += "\ttype: " + getType() + "\tname: " + getName();
		return result;
	}
	
}
