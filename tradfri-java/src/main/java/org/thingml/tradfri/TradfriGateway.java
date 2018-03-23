package org.thingml.tradfri;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradfriGateway implements Runnable {

	/**
	 * Logger to be used for all console outputs, errors and exceptions
	 */
	private static final Logger log = LoggerFactory.getLogger(TradfriGateway.class);
	
	/**
	 * Observer pattern for asynchronous event notification
	 */
	private final List<TradfriGatewayListener> listeners = new ArrayList<TradfriGatewayListener>();
	
	/**
	 * Collection of bulbs registered on the gateway
	 */
	private final List<TradfriLightBulbPacket> bulbs = new ArrayList<TradfriLightBulbPacket>();

	/**
	 * COAPS helpers to GET and SET on the IKEA Tradfri gateway using Californium
	 */
	private CoapEndpoint coap = null;
	
	private final NetworkConfig networkConfig;
	
	/**
	 * Gateway properties and constructor
	 */
	private String gatewayIp = null;
	private String securityKey = null;
	private int pollingRate = 5000;

	public TradfriGateway() {
		this.networkConfig = NetworkConfig.getStandard();
	}

	public TradfriGateway(final int pollingRate) {
		setPollingRate(pollingRate);
		this.networkConfig = NetworkConfig.getStandard();
	}

	public TradfriGateway(final NetworkConfig networkConfig) {
		this.networkConfig = networkConfig;
	}

	public TradfriGateway(final int pollingRate, final NetworkConfig networkConfig) {
		setPollingRate(pollingRate);
		this.networkConfig = networkConfig;
	}

	public TradfriGateway(final String gatewayIp, final String securityKey) {
		this.gatewayIp = gatewayIp;
		this.securityKey = securityKey;
		this.networkConfig = NetworkConfig.getStandard();
	}

	public TradfriGateway(final String gatewayIp, final String securityKey, final NetworkConfig networkConfig) {
		this.gatewayIp = gatewayIp;
		this.securityKey = securityKey;
		this.networkConfig = networkConfig;
	}

	public TradfriGateway(final String gatewayIp, final String securityKey, final int pollingRate) {
		this.gatewayIp = gatewayIp;
		this.securityKey = securityKey;
		setPollingRate(pollingRate);
		this.networkConfig = NetworkConfig.getStandard();
	}

	public TradfriGateway(final String gatewayIp, final String securityKey, final int pollingRate, final NetworkConfig networkConfig) {
		this.gatewayIp = gatewayIp;
		this.securityKey = securityKey;
		setPollingRate(pollingRate);
		this.networkConfig = networkConfig;
	}

	public String getGatewayIp() {
		return gatewayIp;
	}

	public void setGatewayIp(final String gatewayIp) {
		this.gatewayIp = gatewayIp;
	}

	public String getSecurityKey() {
		return securityKey;
	}

	public void setSecurityKey(final String securityKey) {
		this.securityKey = securityKey;
	}

	public int getPollingRate() {
		return pollingRate;
	}

	public void setPollingRate(final int pollingRate) {
		// between 1 and 60 seconds
		if (pollingRate < 1000)
			this.pollingRate = 1000;
		else if (pollingRate > 60000)
			this.pollingRate = 60000;
		else
			this.pollingRate = pollingRate;
	}
	
	public List<TradfriLightBulbPacket> getBulbs() {
		return bulbs;
	}

	private final AtomicBoolean running = new AtomicBoolean(false);

	private final AtomicBoolean cancel = new AtomicBoolean(false);

	public boolean isRunning() {
		return running.get();
	}

	public void addListener(final TradfriGatewayListener l) {
		listeners.add(l);
	}

	public void removeListener(final TradfriGatewayListener l) {
		listeners.remove(l);
	}

	public void clearTradfriGatewayListener() {
		listeners.clear();
	}

	/**
	 * Gateway public API
	 */
	public void start() {
		if (running.get())
			return;
		running.set(true);
		new Thread(this).start();
	}

	public void stop() {
		cancel.set(true);
	}

	public void run() {
		// Notify all listeners
		for (TradfriGatewayListener listener : listeners) {
			try {
				listener.gatewayInitializing(this);
			} catch (Exception ex) {
				//
			}
		}
		
		log.debug("Tradfri Gateway is initalizing...");
		initCoap();
		log.debug("Discovering devices...");
		if (dicoverDevices()) {
			log.debug("Discovered " + bulbs.size() + " bulbs.");
			for (TradfriGatewayListener l : listeners)
				l.gatewayStarted(this);
			try {
				while (!cancel.get()) {
					Thread.sleep(getPollingRate());
					log.debug("Polling bulbs status...");

					// Notify all listeners
					for (TradfriGatewayListener listener : listeners) {
						try {
							listener.pollingStarted(this);
						} catch (Exception ex) {
							//
						}
					}

					long before = System.currentTimeMillis();
					for (TradfriLightBulbPacket bulb : bulbs) {
						bulb.updateBulb();
					}
					long after = System.currentTimeMillis();

					// Notify all listeners
					for (TradfriGatewayListener listener : listeners) {
						try {
							listener.pollingCompleted(this, bulbs.size(), (int) (after - before));
						} catch (Exception ex) {
							//
						}
					}
				}
			} catch (InterruptedException ex) {
				log.error("Error", ex);
			}
		}
		running.set(false);
		cancel.set(false);

		// Notify all listeners
		for (TradfriGatewayListener listener : listeners) {
			try {
				listener.gatewayStoped(this);
			} catch (Exception ex) {
				//
			}
		}
		
		coap.destroy();
		coap = null;
	}

	protected boolean dicoverDevices() {
		bulbs.clear();
		try {
			final CoapResponse responsedevices = get(TradfriConstants.DEVICES);
			if (responsedevices == null) {
				return false;
			}
			final JSONArray devices = new JSONArray(responsedevices.getResponseText());

			// Notify all listeners
			for (TradfriGatewayListener listener : listeners) {
				try {
					listener.bulbDiscoveryStarted(this, devices.length());
				} catch (Exception ex) {
					//
				}
			}
			
			for (int i = 0; i < devices.length(); i++) {
				final CoapResponse responseDevice = get(TradfriConstants.DEVICES + "/" + devices.getInt(i));
				if (responseDevice != null) {
					final JSONObject json = new JSONObject(responseDevice.getResponseText());
					if (json.has(TradfriConstants.TYPE) && json.getInt(TradfriConstants.TYPE) == TradfriConstants.TYPE_BULB) {
						final TradfriLightBulbPacket b = new TradfriLightBulbPacket(json.getInt(TradfriConstants.INSTANCE_ID), this, responseDevice);
						bulbs.add(b);
						
						// Notify all listeners
						for (TradfriGatewayListener listener : listeners) {
							try {
								listener.bulbDiscovered(this, b);
							} catch (Exception ex) {
								//
							}
						}
					} else if (json.has(TradfriConstants.TYPE) && json.getInt(TradfriConstants.TYPE) == TradfriConstants.TYPE_REMOTE) {
						log.debug("REMOTE FOUND: " + json);
					}
				}

			}
			
			// Notify all listeners
			for (TradfriGatewayListener listener : listeners) {
				try {
					listener.bulbDiscoveryCompleted(this);
				} catch (Exception ex) {
					//
				}
			}
		} catch (JSONException e) {
			log.error("Error parsing response from the Tradfri gateway", e);
			return false;

		}
		return true;
	}

	protected void initCoap() {
		final DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder();
		builder.setPskStore(new StaticPskStore("", securityKey.getBytes()));
		coap = new CoapEndpoint(new DTLSConnector(builder.build()), networkConfig);
	}

	protected CoapResponse get(final String path) {
		//log.debug("GET: " + "coaps://" + gatewayIp + "/" + path);
		final CoapClient client = new CoapClient("coaps://" + gatewayIp + "/" + path);
		client.setEndpoint(coap);
		final CoapResponse response = client.get(1);
		if (response == null) {
			log.error("Connection to Gateway timed out, please check ip address or increase the ACK_TIMEOUT in the Californium.properties file");
		}
		return response;
	}

	protected void set(final String path, final String payload) {
		log.debug("SET: " + "coaps://" + gatewayIp + "/" + path + " = " + payload);
		final CoapClient client = new CoapClient("coaps://" + gatewayIp + "/" + path);
		client.setEndpoint(coap);
		final CoapResponse response = client.put(payload, MediaTypeRegistry.TEXT_PLAIN);
		if (response != null && response.isSuccess()) {
			// System.out.println("Yay");
		} else {
			log.error("Sending payload to " + "coaps://" + gatewayIp + "/" + path + " failed!");
		}
		client.shutdown();
	}
}
