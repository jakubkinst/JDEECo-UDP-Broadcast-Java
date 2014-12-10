package cz.kinst.jakub.diploma.udpbroadcast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * Created by jakubkinst on 03/12/14.
 */
public abstract class UDPBroadcast {

    public interface OnUdpPacketReceivedListener {
        void onUdpPacketReceived(DatagramPacket packet);
    }

    private OnUdpPacketReceivedListener mOnPacketReceivedListener;

    public final void sendPacket(byte[] packet) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, getBroadcastAddress(), UDPConfig.PORT);
            socket.send(sendPacket);
            logDebug("Broadcast packet sent to: " + getBroadcastAddress().getHostAddress());
        } catch (IOException e) {
            logError("IOException: " + e.getMessage());
        }
    }

    public final void startReceiving() {
        try {
            //Keep a socket open to listen to all the UDP trafic that is destined for this port
            DatagramSocket socket = new DatagramSocket(UDPConfig.PORT, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);

            while (true) {
                logDebug("Ready to receive broadcast packets!");

                //Receive a packet
                byte[] recvBuf = new byte[UDPConfig.PACKET_SIZE];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);
                // get actual length of data and trim the byte array accordingly
                int length = packet.getLength();
                byte[] data = Arrays.copyOfRange(packet.getData(), 0, length);
                packet.setData(data);

                //Packet received

                String sender = packet.getAddress().getHostAddress();

                // if received message is from myself, skip
                if (sender.equals(getMyIpAddress())) continue;

                logDebug("Packet received from: " + sender + "; Size: " + data.length);
                //String content = new String(data).trim();
                //logDebug("Content: " + content);
                onPacketReceived(packet);
                if (mOnPacketReceivedListener != null)
                    mOnPacketReceivedListener.onUdpPacketReceived(packet);
                else
                    logError("No listener for incoming UDP packets registered");
            }
        } catch (IOException ex) {
            logError("Oops" + ex.getMessage());
        }
    }

    protected abstract InetAddress getBroadcastAddress();


    public abstract String getMyIpAddress();

    protected void onPacketReceived(DatagramPacket packet) {

    }

    protected abstract void logDebug(String message);

    protected abstract void logError(String message);

    protected abstract void logInfo(String message);


    public void setOnPacketReceivedListener(OnUdpPacketReceivedListener listener) {
        this.mOnPacketReceivedListener = listener;
    }
}
