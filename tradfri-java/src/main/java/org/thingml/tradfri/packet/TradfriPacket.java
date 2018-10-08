package org.thingml.tradfri.packet;

public abstract class TradfriPacket {

	protected static final String PACKET_TYPE_CONTROL_OUTLET = "controlOutlet";

	protected static final String PACKET_TYPE_LIGHT_BULB = "lightBulb";

	protected static final String PACKET_TYPE_MOTION_SENSOR = "motionSensor";

	protected static final String PACKET_TYPE_REMOTE = "remote";

	protected String packetType;

	protected long timestamp = 0L;

	public TradfriPacket(final String packetType) {
		this.packetType = packetType;
	}

	public String getPacketType() {
		return packetType;
	}

	protected final void setPacketType(final String packetType) {
		this.packetType = packetType;
	}

	public final long getTimestamp() {
		return timestamp;
	}

	protected final void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
	}

}
