package cz.kinst.jakub.diploma.udpbroadcast;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class JavaUDPBroadcast extends UDPBroadcast {

    private Thread receivingThread;

    @Override
    public final InetAddress getBroadcastAddress() {
        try {
            Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback())
                    continue;    // Don't want to broadcast to the loopback interface
                for (InterfaceAddress interfaceAddress :
                        networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null)
                        continue;
                    return broadcast;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    @Override
    public final String getMyIpAddress() {
        try {
            return Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    protected final void logDebug(String message) {
        System.out.println("DEBUG: " + UDPConfig.TAG + ": " + message);
    }

    @Override
    protected final void logError(String message) {
        System.err.println("ERROR: " + UDPConfig.TAG + ": " + message);
    }

    @Override
    protected final void logInfo(String message) {
        System.out.println("INFO: " + UDPConfig.TAG + ": " + message);
    }

    @Override
    public void startReceivingInBackground() {
        receivingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                startReceiving();
            }
        });
        receivingThread.start();
    }

    @Override
    public void stopReceivingInBackground() {
        receivingThread.interrupt();
    }
}
