# hhc2mqtt

The software automatically integrates the HHC-N8180P device available on the popular auction portal with the Home Assistant.
HHC-N8180P has 8 digital inputs and 8 digital outputs in a DIN rail housing (plus an extra modbus output).

https://prnt.sc/22bfc1r


Integration takes place thanks to the protocol MQTT Discovery 
https://www.home-assistant.io/docs/mqtt/discovery/, 

After running on any server (requires Java 8, e.g. OpenJDK 11 e.g. on the HA machine or on external machine) 
automatically creates HHC* entities for all HHC-N818P contactors and inputs - https://prnt.sc/22bet8i

There is no need for any additional configuration in Home Assistant (apart from creating HHC control automations of course :) 



How to build:

1. mvn clean install
2. change dir to target dir
3. run command java -jar hhc-1.0-SNAPSHOT.jar 
4. folder with libs hhc-1.0-SNAPSHOT.lib is used
5. logs are saved to files hhc2mqtt.debug.log and hhc2mqtt.error.log 

6. program parameters:

java -jar hhc-1.0-SNAPSHOT.jar -DHHC_SERVER_IP="192.168.3.11" -DHHC_SERVER_PORT=5000 -DMQTT_SERVER_URI="tcp://192.168.102.6:1883" -DMQTT_SERVER_LOGIN="XXX" -DMQTT_SERVER_PASSWORD="YYYY" -DBASE_TOPIC="homeassistant" -DHA_DISCOVERY_TOPIC="homeassistant"

If there are any problems with the software, please write or report bugs - I will try to fix it as quickly as possible! 
