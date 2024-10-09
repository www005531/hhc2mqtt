package ao.hhc;

import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.util.BitVector;

import java.util.Date;

//import static junit.framework.TestCase.assertFalse;
//import static org.junit.Assert.*;


public class ModbusTCPAlarm {
    static ModbusTCPMaster master;
    static int errorCounter = 0;
    static String lastErrorMessage="";
    MQTTClient mqtt;
    public ModbusTCPAlarm(MQTTClient mqttClient){
        this.mqtt = mqttClient;
    }

    public BitVector readDiscreteInputs(int unitId, int ref, int count) throws Exception {
        try {
            return master.readInputDiscretes(unitId, ref, count);
        } catch (Exception e) {
            errorCounter++;
            lastErrorMessage = new Date()+" "+ e.getMessage();
            System.out.println("ERROR " + e.getMessage());
            connect();
            return null;
        }
    }
    //https://github.com/steveohara/j2mod/blob/development/src/test/java/com/ghgande/j2mod/modbus/utils/AbstractTestModbusSerialRTUMaster.java

//https://github.com/steveohara/j2mod/blob/development/src/test/java/com/ghgande/j2mod/modbus/utils/AbstractTestModbusSerialASCIIMaster.java
    public void process() throws Exception {
        connect();
        int UNIT_ID = 5; // ALARM WP8026

        int counter = 0;
        String lastInfo = "";
        while(true) {
            try {
                counter++;
                BitVector bv = readDiscreteInputs(UNIT_ID, 0, 15);
                if(bv==null) continue;;
                long end = System.currentTimeMillis();

                String info = "";
                int okCount=0;

                for(int i=0;i<bv.size();i++){
                    //System.out.print(bv.getBit(i)+", ");
                    if(bv.getBit(i)) okCount++;
                    info+=bv.getBit(i)?"\"ok\"":"\"naruszenie\"";
                    if(i<bv.size()-1) info+=",";
                }
                if(!info.equals(lastInfo)){
                    lastInfo = info;
                    System.out.println(counter+". "+info+" okCount:"+okCount+" eC="+errorCounter+" lEM="+lastErrorMessage);

                    String msg = "{\"data\":{\"in\":["+info+"]},\"counter\":"+counter+",\"okCount\":"+okCount+
                            ",\"errorCounter\":"+errorCounter+",\"lastErrorMessage\":\""+lastErrorMessage+"\"}";
                    mqtt.publish("WP8026ADAP_Alarm_Addr5", msg);

                    //Thread.sleep(100);

                }else{
                    //Thread.sleep(100);
                    //continue;
                }
                info="";

                Thread.sleep(100);

            }catch (Exception e){
                System.out.println(e.getMessage());
                lastErrorMessage = new Date()+" "+e.getMessage();
                errorCounter++;
                if(true){
                    Thread.sleep(1000);
                }
            }
        }



    }

    private void connect() {
        //master = new ModbusTCPMaster("192.168.3.168",4196);
        synchronized (Object.class) {
            if (master != null && master.isConnected()) return;
            boolean connected = false;
            while (!connected) {
                try {
                    master = new ModbusTCPMaster("192.168.3.117", 4196);
                    master.setTimeout(1000);
                    master.setRetries(Integer.MAX_VALUE);
                    master.connect();
                } catch (Exception e) {
                    System.out.println("Modbus#97 " + e.getMessage());
                    connected = false;
                    try {
                        Thread.sleep(2000);
                    }catch (Exception e2){}
                }
                connected = true;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        MQTTClient mqtt = new MQTTClient();
        ModbusTCPAlarm mta = new ModbusTCPAlarm(mqtt);
        mta.process();

    }
}
