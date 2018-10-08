package org.thingml.tradfri.packet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.californium.core.CoapResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.thingml.tradfri.TradfriConstants;
import org.thingml.tradfri.TradfriGateway;

public abstract class TradfriHardwarePacket<T> extends TradfriPacket {

	/**
	 * Logger to be used for all console outputs, errors and exceptions
	 */
	private static final Logger log = LoggerFactory.getLogger(TradfriHardwarePacket.class);

	protected final List<T> listeners = new ArrayList<T>();

	private TradfriGateway gateway;
	
	private String manufacturer;
	private String type;
	private String firmware;

	// Dates
	private Date dateInstalled;
	private Date dateLastSeen;

	// Immutable information
	private int id;
	private String name;

	private JSONObject jsonObject;

	public TradfriHardwarePacket(final String packetType, final int id, final TradfriGateway gateway) {
		super(packetType);
		
		this.id = id;
		this.gateway = gateway;
	}

	public TradfriHardwarePacket(final String packetType, final int id, final TradfriGateway gateway, final CoapResponse response) {
		super(packetType);
		
		this.id = id;
		this.gateway = gateway;
		
		if (response != null) {
			parseResponseBase(response);
		}
	}

	public abstract void update() throws JSONException;

	public void addListener(final T l) {
		listeners.add(l);
	}

	public void removeListener(final T l) {
		listeners.remove(l);
	}

	public void clearListeners() {
		listeners.clear();
	}

	protected TradfriGateway getGateway() {
		return gateway;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public String getType() {
		return type;
	}

	public String getFirmware() {
		return firmware;
	}

	public Date getDateInstalled() {
		return dateInstalled;
	}

	public Date getDateLastSeen() {
		return dateLastSeen;
	}

	public JSONObject getJsonObject() {
		return jsonObject;
	}

	public void sendJSONPayload(String json) {
		gateway.set(TradfriConstants.DEVICES + "/" + this.getId(), json);
	}

	protected boolean parseResponseBase(final CoapResponse response) {
		boolean updateListeners = false;
		
		log.debug(response.getResponseText());
		
		try {
			JSONObject json = new JSONObject(response.getResponseText());
			jsonObject = json;
			String new_name = json.getString(TradfriConstants.NAME);
			if (name == null || !name.equals(new_name))
				updateListeners = true;
			name = new_name;

			dateInstalled = new Date(json.getLong(TradfriConstants.DATE_INSTALLED) * 1000);
			dateLastSeen = new Date(json.getLong(TradfriConstants.DATE_LAST_SEEN) * 1000);

			manufacturer = json.getJSONObject("3").getString("0");
			type = json.getJSONObject("3").getString("1");
			firmware = json.getJSONObject("3").getString("3");
		} catch (JSONException e) {
			log.error("Cannot update device info: error parsing the response from the gateway", e);
		}
		
		return updateListeners;
	}
	
}
