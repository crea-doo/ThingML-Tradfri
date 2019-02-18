package org.thingml.tradfri;

import org.thingml.tradfri.TradfriGateway;
import org.thingml.tradfri.packet.TradfriLightBulbPacket;

public class Test {

	protected static String gatewayIp = "10.3.1.180";
	protected static String securityKey = "5HV7ibb4brgWL18x";

	public static void main(String[] args) {

		final TradfriGateway gw = new TradfriGateway(gatewayIp, securityKey);
		gw.initCoap();
		gw.dicoverDevices();
		for (TradfriLightBulbPacket b : gw.getLightBulbs()) {
			// b.updateBulb();
			System.out.println(b.toString());
		}
		System.exit(0);
	}

}
