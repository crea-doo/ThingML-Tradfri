package org.thingml.tradfri.listener;

import org.thingml.tradfri.packet.TradfriLightBulbPacket;

public interface TradfriLightBulbListener {

	public void bulbStateChanged(TradfriLightBulbPacket bulb);

}
