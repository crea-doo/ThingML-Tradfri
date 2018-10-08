package org.thingml.tradfri.listener;

import org.thingml.tradfri.packet.TradfriControlOutletPacket;

public interface TradfriControlOutletListener {

	public void controlOutletStateChanged(TradfriControlOutletPacket controlOutlet);

}
