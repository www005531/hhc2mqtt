package ao.hhc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HHCClient {
    public static final int READ_TIMEOUT = 10 * 1000;
    public static final int LINGER_TIME = 100;
    public static final int SEND_DELAY = 0;
    public static final int RESPONSE_DELAY = 100;
    public static final String _IP = System.getProperty("HHC_SERVER_IP", "192.168.3.11");
    public static final String quasiIP = _IP;
    public static final int PORT = Integer.parseInt(System.getProperty("HHC_SERVER_PORT", "5000"));
    //public static final String IP = "192.168.102.4";
    //public static final int PORT = 4999;

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    String ip;
    int port;
    public static final String OLD_PREFIX = "B-".toLowerCase();
    public static final String PREFIX = "D_".toLowerCase();
    private static final Logger logger = LogManager.getLogger(HHCClient.class);

    public HHCClient()  {
        ip = _IP;
        port = PORT;
    }

    public void restart() {
        System.out.println("HHC RESTART");
        close();
        while(!startConnection(ip, port)){
            close();
            try { Thread.sleep(2000);}catch (Exception e2){}
        }
    }

    public String getDeviceName() {
        // IP = "19216810212"
        String s = PREFIX + ("HHC_" + quasiIP + ":" + PORT).replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        s = PREFIX + ("HHC_").replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        //s = s.replaceAll("192168311", "19216810212");
        return s;
    }

    public String getDeviceElementName(int dev, boolean isSwitch) {

        String s =  PREFIX + ("HHC_" + quasiIP + ":" + PORT).replaceAll("[^A-Za-z0-9]", "").toLowerCase() + (isSwitch ? "-switch" : "-in") + dev;
        s =  PREFIX + ("HHC_" ).replaceAll("[^A-Za-z0-9]", "").toLowerCase() + (isSwitch ? "_switch" : "_in") + dev;
        //s = s.replaceAll("192168311", "19216810212");

        return s;
    }

    public String getDeviceElementUUID(int dev, boolean isSwitch) {
        String s=  PREFIX + ("HHC_" + quasiIP + ":" + PORT).replaceAll("[^A-Za-z0-9]", "").toLowerCase() + (isSwitch ? "-switch" : "-in") + dev;
        s=  PREFIX + ("HHC_").replaceAll("[^A-Za-z0-9]", "").toLowerCase() + (isSwitch ? "_switch" : "_in") + dev;
        //s = s.replaceAll("192168311", "19216810212");
        return s;
    }


    private synchronized  boolean startConnection(String ip, int port)   {
        synchronized (Object.class) {
            if (clientSocket != null && clientSocket.isConnected()) return true;
            System.out.println("hhc#74 Connecting to " + ip + ":" + port);
            boolean tryConnect = true;
            int retryCount = 0;
            while (tryConnect) {
                try {
                    retryCount++;
                    clientSocket = new Socket(ip, port);
                    clientSocket.setSoLinger(true, LINGER_TIME);
                    clientSocket.setSoTimeout(READ_TIMEOUT);
                    out = new PrintWriter(clientSocket.getOutputStream(), false);
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    tryConnect = false;
                } catch (Exception e) {
                    System.out.println("HHC ERROR#69#" + e.getMessage());
                    error("hhc#88 "+e.getMessage());
                    error("hhc#89 starting connection... retry count=" + (retryCount++) + " threadId=" + Thread.currentThread().getId());
                    return false;
                }
            }
            System.out.println("hhc#93 connection started for thread " + Thread.currentThread().getId() + ". redone " + retryCount + " times.");
            debug("connection started for thread " + Thread.currentThread().getId() + ". redone " + retryCount + " times.");
            return true;
        }
    }

    static int lp = 0;

    public synchronized String sendMessage(String msg) throws Exception {
        return sendMessage(msg, false);
    }

    private String sendMessage(String msg, boolean debug) throws Exception {
        //if(clientSocket==null || !clientSocket.isConnected() || clientSocket.isClosed()) startConnection(ip,port);
        try {
            Thread.sleep(SEND_DELAY);
            out.print(msg);
            out.flush();
            String resp = readResponse();
            if (debug) {
                return (lp++) + " " + msg + "->" + resp + " " + clientSocket.hashCode();
            } else {
                return resp;
            }
        } catch (Exception e) {
            error("hhc#118 "+e.getMessage());
            restart();
            return null;
        }

    }

    private boolean respIsOK(String r) {
        boolean retOn = r.matches("on\\d{1}");
        boolean retOff = r.matches("off\\d{1}");
        boolean retInput = r.matches("input\\d{8}");
        boolean retRead = r.matches("relay\\d{8}");
        boolean retName = r.matches("name=\\\"(.*)\\\"");
        boolean ret = retOn || retOff || retInput || retRead || retName;
        debug("r=" + r + " ret=" + ret + " on=" + retOn + " off=" + retOff + " input=" + retInput + " switch=" + retRead +
                " retName=" + retName +
                " closed/out/in:" +
                clientSocket.isClosed() + "/" + clientSocket.isOutputShutdown() + "/" + clientSocket.isInputShutdown());
        return ret;
    }

    private String readResponse() throws Exception {
        Thread.sleep(RESPONSE_DELAY);
        String s = "";
        int readCount = 0;
        while (in.ready() || !respIsOK(s)) {
            try {
                s += "" + (char) in.read();
                if (readCount++ > 20) throw new SocketException();
            } catch (SocketException | SocketTimeoutException se) {
                restart();
                return null;
            }
            if (respIsOK(s)) break;
            Thread.sleep(1); // prevent killing CPU
        }
        return s;
    }


    private void close() {

        try {
            in.close();
        } catch (Exception e) {
            //e.printStackTrace();
        }
        try {
            out.close();
        } catch (Exception e) {
            //e.printStackTrace();
        }
        try {
            clientSocket.close();
        } catch (Exception e) {
            //e.printStackTrace();
        }
        clientSocket = null;
        debug("connection stopped");
    }

    public void testCommands() throws Exception {
        restart();

        while (Math.random() < 10) {
            debug(sendMessage("name"));
            Thread.sleep(1000);
        }
        if (true) return;
        //cc.startConnection("192.168.102.4",4999);
        while (true) {
            debug(Thread.currentThread().getId() + " " + clientSocket.isConnected() + " " + clientSocket.isInputShutdown() + " " + clientSocket.isOutputShutdown());

            if (Math.random() > 0.5) {
                debug(Thread.currentThread().getId() + " " + sendMessage("on1"));
                debug(Thread.currentThread().getId() + " " + sendMessage("on2"));
                debug(Thread.currentThread().getId() + " " + sendMessage("on3"));
                debug(Thread.currentThread().getId() + " " + sendMessage("on4"));
                debug(Thread.currentThread().getId() + " " + sendMessage("on5"));
                debug(Thread.currentThread().getId() + " " + sendMessage("on6"));
                debug(Thread.currentThread().getId() + " " + sendMessage("on7"));
                debug(Thread.currentThread().getId() + " " + sendMessage("on8"));
                debug(Thread.currentThread().getId() + " " + sendMessage("read"));
                debug(Thread.currentThread().getId() + " " + sendMessage("off1"));
            } else {
                debug(Thread.currentThread().getId() + " " + sendMessage("off2"));
                debug(Thread.currentThread().getId() + " " + sendMessage("off3"));
                debug(Thread.currentThread().getId() + " " + sendMessage("off4"));
                debug(Thread.currentThread().getId() + " " + sendMessage("off5"));
                debug(Thread.currentThread().getId() + " " + sendMessage("off6"));
                debug(Thread.currentThread().getId() + " " + sendMessage("off7"));
                debug(Thread.currentThread().getId() + " " + sendMessage("off8"));
                debug(Thread.currentThread().getId() + " " + sendMessage("input"));
                debug(Thread.currentThread().getId() + " " + sendMessage("read"));
            }
        }
    }

    public static void debug(String s) {
        logger.debug(s);
    }

    public static void error(String s) {
        logger.error(s);
    }

    public static void main(String[] args) {
        for (int i = 0; i < 1; i++) {
            Thread t = new Thread(() -> {
                try {
                    new HHCClient().testCommands();
                } catch (Exception e) {
                    error(e.getMessage());
                }
            });
            t.start();

        }
    }
}
