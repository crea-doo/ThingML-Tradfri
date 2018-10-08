package org.thingml.tradfri.listener;

import org.thingml.tradfri.packet.TradfriRemotePacket;

public interface TradfriRemoteListener {

	public void remoteStateChanged(TradfriRemotePacket remote);

}
