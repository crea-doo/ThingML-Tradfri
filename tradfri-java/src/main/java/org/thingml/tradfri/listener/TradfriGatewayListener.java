package org.thingml.tradfri.listener;

import org.thingml.tradfri.TradfriGateway;
import org.thingml.tradfri.packet.TradfriControlOutletPacket;
import org.thingml.tradfri.packet.TradfriLightBulbPacket;

/**
 *
 * @author franck
 */
public interface TradfriGatewayListener {

	public void gatewayInitializing(TradfriGateway gateway);

	public void lightBulbDiscoveryStarted(TradfriGateway gateway, int totalDevices);

	public void lightBulbDiscovered(TradfriGateway gateway, TradfriLightBulbPacket lightBulb);

	public void lightBulbDiscoveryCompleted(TradfriGateway gateway);

	public void controlOutletDiscoveryStarted(TradfriGateway gateway, int totalDevices);

	public void controlOutletDiscovered(TradfriGateway gateway, TradfriControlOutletPacket controlOutlet);

	public void controlOutletDiscoveryCompleted(TradfriGateway gateway);

	public void gatewayStarted(TradfriGateway gateway);

	public void gatewayStoped(TradfriGateway gateway);

	public void pollingStarted(TradfriGateway gateway);

	public void pollingCompleted(TradfriGateway gateway, int deviceCount, int totalTime);
	
}
