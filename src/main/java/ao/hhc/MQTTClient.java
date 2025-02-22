package ao.hhc;


import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class MQTTClient {
    public static final String SERVER_URI = System.getProperty("MQTT_SERVER_URI", "tcp://192.168.3.4:1883");
    public static final String MQTT_SERVER_LOGIN = System.getProperty("MQTT_SERVER_LOGIN", "robo1");
    public static final String MQTT_SERVER_PASSWORD = System.getProperty("MQTT_SERVER_PASSWORD", "Qwas!100");
    public static final String BASE_TOPIC = System.getProperty("BASE_TOPIC", "homeassistant");
    public static final String HA_DISCOVERY_TOPIC = System.getProperty("HA_DISCOVERY_TOPIC", "homeassistant");


    IMqttClient client;
    HHCClient hhc;

    static {
        // BASE_TOPIC = HA_DISCOVERY_TOPIC;

        // BasicConfigurator.configure();
    }

    String lastInput = null;

    private static final Logger logger = LogManager.getLogger(MQTTClient.class);

    public void connect() throws Exception {
        synchronized (Object.class) {
            try {
                if (client != null && client.isConnected()) return;
                String publisherId = "HHCClient_" + UUID.randomUUID();
                client = new MqttClient(SERVER_URI, publisherId,
                        new MqttDefaultFilePersistence(System.getProperty("TEMP") + "/MQTTCLIENT"));

                MqttConnectOptions options = new MqttConnectOptions();
                options.setUserName(MQTT_SERVER_LOGIN);
                options.setPassword(MQTT_SERVER_PASSWORD.toCharArray());
                options.setAutomaticReconnect(true);
                options.setCleanSession(false); // czyli w razie rozłączenia przechowywana jest informacja o subskrypcjach
                options.setConnectionTimeout(10);
                options.setMaxInflight(100);
                client.connect(options);
            } catch (Exception e) {
                throw new Exception("MQTT#connect:" + e.getMessage());
            }
        }

    }

    public void publish(String topic, String message) throws Exception {
        connect();
        //  publisher.publish("hhc/sw/1/available",new MqttMessage("offline".getBytes()));
        client.publish(topic, new MqttMessage(message.getBytes()));
    }

    public void process() throws Exception {

        connect();

        //msg.setQos(0);
        //msg.setRetained(true);

        //  publisher.publish("hhc/sw/1/available",new MqttMessage("offline".getBytes()));


//        publisher.publish("hhc/sw/1/state",new MqttMessage("ON".getBytes()));
        discoverSwitches();

        discoverBinaryInputs();


        for (int dev = 1; dev < 9; dev++) {
            cleanUpOldVersion(dev);

        }


//        client.publish(BASE_TOPIC+"/switch/" + cc.getDeviceName(1) + "/state", new MqttMessage("ON".getBytes()));
// command //        client.publish("hhc/sw/1/set",new MqttMessage("OFF".getBytes()));

    }


    public void discoverBinaryInputs() throws Exception {
        connect();
        for (int dev = 1; dev < 9; dev++) {

            MqttMessage msg = new MqttMessage(("{\"name\": \"" + hhc.getDeviceElementName(dev, false) + "\", " +
                    //   " \"force_update\": true, \"qos\": 1 , " +
                    " \"unique_id\": \"" + hhc.getDeviceElementUUID(dev, false) + "\", " +
                    " \"availability\": [{ \"topic\": \"" + BASE_TOPIC + "/binary_sensor/" + hhc.getDeviceName() + "/input" + dev + "/availability\", " +
                    " \"payload_available\": \"online\", \"payload_not_available\": \"offline\" } ] , " +
                    // " \"qos\": \"0\", " +
                    //    " \"device_class\": \"motion\", " +

                    " \"device\":{\"identifiers\":[\"" + hhc.getDeviceName() + "\"]," +
                    " \"manufacturer\":\"CHRL\",\"model\":\"HHC-N8180P\",\"name\":\"HHC" + "\",\"sw_version\":\"" + HHCClient.PREFIX + "1.1\", " +
                    " \"suggested_area\": \"AUTOMATYKA\"}, " +
                    // " \"command_topic\": \""+BASE_TOPIC+"/switch/" + cc.getDeviceName(dev) + "/set\", " +
                    " \"state_topic\": \"" + BASE_TOPIC + "/binary_sensor/" + hhc.getDeviceName() + "/input" + dev + "/state\" } ").getBytes()
            );
            msg.setRetained(true);
            client.publish(HA_DISCOVERY_TOPIC + "/binary_sensor/" + hhc.getDeviceName() + "/input" + dev + "/config",
                    msg);
            //subscribe(dev);
        }
        for (int dev = 1; dev < 9; dev++) {

            //subscribe(dev);
        }
        new Thread(() -> {
            while (true) {
                for (int dev = 1; dev < 9; dev++) {
                    try {
                        setBinaryInputsAvailable(dev);
                    } catch (Exception e) {

                    }
                }
                try {
                    Thread.sleep(10000);
                } catch (Exception e2) {
                }
            }

        }).start();

        new Thread(() -> {

            while (true) {
                try {
                    //Thread.sleep(100);
                    String in = hhc.sendMessage("input");
                    if (in == null) in = "";
                    in = in.trim();
                    if (in.equals(lastInput)) continue;
                    lastInput = in;
                    char[] a = in.toCharArray();
                    debug("input: " + in);
                    // "input00000001";
                    if (in.contains("input")) {
                        for (int dev = 1; dev < 9; dev++) {
                            boolean one = ("" + a[9 - dev + 4]).equals("1");
                            debug("dev[" + dev + "]=" + (one ? "1" : "0"));
                            client.publish(BASE_TOPIC + "/binary_sensor/" + hhc.getDeviceName() + "/input" + dev + "/state",
                                    new MqttMessage((one ? "ON" : "OFF").getBytes()));
                            // setBinaryInputsAvailable(dev)
                        }

                    }


                } catch (Exception e) {
                    error("MQTT#144 " + e.getMessage());
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e2) {
                        error(e2.getMessage());
                    }
                }

            }
        }).start();

    }


    ////////////////////////


    public void discoverSwitches() throws Exception {
        connect();
        for (int dev = 1; dev < 9; dev++) {

            MqttMessage msg = new MqttMessage(("{\"name\": \"" + hhc.getDeviceElementName(dev, true) + "\", " +
                    //   " \"force_update\": true, \"qos\": 1 , " +
                    " \"unique_id\": \"" + hhc.getDeviceElementUUID(dev, true) + "\", " +
                    " \"availability\": [{ \"topic\": \"" + BASE_TOPIC + "/switch/" + hhc.getDeviceName() + "/switch" + dev + "/availability\", " +
                    " \"payload_available\": \"online\", \"payload_not_available\": \"offline\" } ] , " +
                    // " \"qos\": \"0\", " +
                    //   " \"device_class\": \"power\", " +

                    " \"device\":{\"identifiers\":[\"" + hhc.getDeviceName() + "\"]," +
                    " \"manufacturer\":\"CHRL\",\"model\":\"HHC-N8180P\",\"name\":\"HHC" + "\",\"sw_version\":\"" + HHCClient.PREFIX + "1.1\", " +
                    " \"suggested_area\": \"AUTOMATYKA\"}, " +
                    " \"command_topic\": \"" + BASE_TOPIC + "/switch/" + hhc.getDeviceName() + "/switch" + dev + "/set\", " +
                    " \"state_topic\": \"" + BASE_TOPIC + "/switch/" + hhc.getDeviceName() + "/switch" + dev + "/state\" } ").getBytes()
            );
            msg.setRetained(true);
            String s2 = HA_DISCOVERY_TOPIC + "/switch/" + hhc.getDeviceName() + "/switch" + dev + "/config";
            client.publish(s2,
                    msg);
            logger.debug("s2=" + s2 + " msg=" + new String(msg.getPayload()));
            //MqttTopic topic = client.getTopic(s2);
            //subscribe(dev);
            //logger.debug("topic="+topic);
        }
        for (int dev = 1; dev < 9; dev++) {

            subscribeSwitch(dev);
        }
        readSwitches();

        new Thread(() -> {
            while (true) {
                for (int dev = 1; dev < 9; dev++) {
                    try {
                        setSwitchesAvailable(dev);
                    } catch (Exception e) {

                    }
                }
                try {
                     Thread.sleep(10000);
                } catch (Exception e2) {
                }
            }

        }).start();


        new Thread(new Runnable() {
            @Override
            public void run() {

                while (true) {
                    try {
                        if (!client.isConnected()) {
                            debug("MQTT Client is disconnected");
                            Thread.sleep(1000);
                            continue;
                        }
                        for (int dev = 1; dev < 9; dev++) {
                            setSwitchesAvailable(dev);
                            setBinaryInputsAvailable(dev);
                        }
                        Thread.sleep(60 * 1000);
                    } catch (Exception e) {
                        error("MONITOR: " + e.getMessage());

                    }
                }
            }
        }).start();

    }


    public void subscribeSwitch(int dev) throws Exception {
        connect();
        debug("subscribe " + dev);
        client.subscribe(BASE_TOPIC + "/switch/" + hhc.getDeviceName() + "/switch" + dev + "/set", (topic, msg) -> {
            try {
                debug("<subscribe>");
                byte[] payload = msg.getPayload();
                debug(new String(payload) + "=");

                String devCmd = new String(payload);
                if (devCmd.toUpperCase().contains("ON")) {
                    devCmd = "on" + dev;
                } else {
                    devCmd = "off" + dev;
                }
                String ret = hhc.sendMessage(devCmd);
                debug("ret=[" + ret + "]");

//                int i = Integer.parseInt("" + new String(payload).replaceAll("on", "").replaceAll("off", ""));
                String stateTopic = BASE_TOPIC + "/switch/" + hhc.getDeviceName() + "/switch" + dev + "/state";
                debug("sending on topic=" + topic + " stateTopic=" + stateTopic + " message: " + msg);
                client.publish(stateTopic, new MqttMessage(payload));
                debug("Message sent.");
            } catch (Exception e) {
                error("MQTT#249 " + e.getMessage());
            }
        });
    }


    public void setBinaryInputsAvailable(int dev) throws Exception {
        setAvailable(dev, true, true);
    }

    public void setSwitchesAvailable(int dev) throws Exception {
        setAvailable(dev, true, false);
    }

    private void setAvailable(int dev, boolean available, boolean isBinaryInput) throws Exception {
        connect();
        debug("sending available dev:" + dev + " available=" + available);
        String topic = BASE_TOPIC + "/switch/" + hhc.getDeviceName() + "/switch" + dev + "/availability";

        if (isBinaryInput)
            topic = BASE_TOPIC + "/binary_sensor/" + hhc.getDeviceName() + "/input" + dev + "/availability";
        client.publish(topic, new MqttMessage((available ? "online" : "offline").getBytes()));
    }

    public void cleanUpOldVersion(int dev) throws Exception {
        connect();

        String removeTopic = HA_DISCOVERY_TOPIC + "/switch/" + hhc.getDeviceElementName(dev, true).replaceAll(HHCClient.PREFIX, HHCClient.OLD_PREFIX).
                replaceAll("5000", "4999").replaceAll("10212", "1024") + "/switch/config";
        debug("removeTopic=" + removeTopic);
        client.publish(removeTopic, new byte[0], 0, true);
        removeTopic = HA_DISCOVERY_TOPIC + "/switch/" + hhc.getDeviceElementName(dev, true).replaceAll(HHCClient.PREFIX, HHCClient.OLD_PREFIX)
                + "/switch/config";
        debug("removeTopic=" + removeTopic);
        client.publish(removeTopic, new byte[0], 0, true);

        removeTopic = BASE_TOPIC + "/switch/" + hhc.getDeviceElementName(dev, true).replaceAll(HHCClient.PREFIX, HHCClient.OLD_PREFIX).
                replaceAll("5000", "4999").replaceAll("10212", "1024") + "/switch";
        debug("removeTopic=" + removeTopic);
        client.publish(removeTopic, new byte[0], 0, true);

        removeTopic = BASE_TOPIC + "/switch/" + hhc.getDeviceElementName(dev, true).replaceAll(HHCClient.PREFIX, HHCClient.OLD_PREFIX) + "/switch";
        debug("removeTopic=" + removeTopic);
        client.publish(removeTopic, new byte[0], 0, true);


        removeTopic = BASE_TOPIC + "/switch/" + hhc.getDeviceElementName(dev, true).replaceAll(HHCClient.PREFIX, HHCClient.OLD_PREFIX).
                replaceAll("5000", "4999").replaceAll("10212", "1024") + "/";
        debug("removeTopic=" + removeTopic);
        client.publish(removeTopic, new byte[0], 0, true);

        removeTopic = BASE_TOPIC + "/switch/" + hhc.getDeviceElementName(dev, true).replaceAll(HHCClient.PREFIX, HHCClient.OLD_PREFIX) + "/";
        debug("removeTopic=" + removeTopic);
        client.publish(removeTopic, new byte[0], 0, true);
    }


    public void readSwitches() throws Exception {
        connect();
        String in = hhc.sendMessage("read");
        if (in == null) in = "";
        in = in.trim();
        char[] a = in.toCharArray();
        debug("relay: " + in);
        // "input00000001";
        if (in.contains("relay")) {
            for (int dev = 1; dev < 9; dev++) {
                boolean one = ("" + a[9 - dev + 4]).equals("1");
                debug("dev[" + dev + "]=" + (one ? "1" : "0"));
                client.publish(BASE_TOPIC + "/switch/" + hhc.getDeviceName() + "/switch" + dev + "/state",
                        new MqttMessage((one ? "ON" : "OFF").getBytes()));
                //setSwitchesAvailable(dev);
            }
        }
    }

    public static void debug(String s) {
        logger.debug(s);
    }

    public static void error(String e) {
        logger.error(e);
    }


    public static void main(String[] args) {
        debug("STARTING");
        error("STARTING");
        System.out.println("Starting");
        final MQTTClient mqtt = new MQTTClient();
        System.out.println("Connected to MQTT");

        mqtt.hhc = new HHCClient();
        mqtt.hhc.restart();

        new Thread(() -> {
            boolean error = true;
            while (error) {
                try {
                    System.out.println("Start processing HHC");
                    mqtt.process();
                    error = false;
                } catch (Exception e) {
                    error = true;
                    System.out.println("MQTT#333 " + e.getMessage());
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e2) {
                    }
                }
            }
        }).start();

        ModbusTCPAlarm mta = new ModbusTCPAlarm(mqtt);

        new Thread(() -> {
            boolean error = true;
            while (error) {
                try {
                    System.out.println("Start processing Modbus");
                    mqtt.connect();
                    mta.process();
                    error = false;
                } catch (Exception e) {
                    error = true;
                    System.out.println("MQTT#357 " + e.getMessage());
                    error("MQTT#371 " + e.getMessage());
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e2) {
                    }
                }
            }
        }
        ).start();
    }
}
