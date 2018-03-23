package org.thingml.tradfri;

/**
 *
 * @author franck
 */
public interface TradfriGatewayListener {

	public void gatewayInitializing(TradfriGateway gateway);

	public void bulbDiscoveryStarted(TradfriGateway gateway, int totalDevices);

	public void bulbDiscovered(TradfriGateway gateway, TradfriLightBulbPacket bulb);

	public void bulbDiscoveryCompleted(TradfriGateway gateway);

	public void gatewayStarted(TradfriGateway gateway);

	public void gatewayStoped(TradfriGateway gateway);

	public void pollingStarted(TradfriGateway gateway);

	public void pollingCompleted(TradfriGateway gateway, int bulbCount, int totalTime);
	
}
