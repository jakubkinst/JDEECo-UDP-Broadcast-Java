package cz.kinst.jakub.diploma.udpbroadcast;

import cz.cuni.mff.d3s.deeco.network.*;

import java.net.DatagramPacket;

/**
 * Created by jakubkinst on 12/11/14.
 */
public class UDPBroadcastHost extends AbstractHost implements NetworkInterface {


    private final PacketReceiver packetReceiver;
    private final PacketSender packetSender;
    private final UDPBroadcast udpBroadcast;

    public UDPBroadcastHost(String ipAddress, UDPBroadcast udpBroadcast) {
        super(ipAddress, new DefaultCurrentTimeProvider());
        this.udpBroadcast = udpBroadcast;
        this.packetReceiver = new PacketReceiver(id, UDPConfig.PACKET_SIZE);
        this.packetSender = new PacketSender(this, UDPConfig.PACKET_SIZE, false, false);
        this.packetReceiver.setCurrentTimeProvider(this);
        this.udpBroadcast.setOnPacketReceivedListener(new UDPBroadcast.OnUdpPacketReceivedListener() {
            @Override
            public void onUdpPacketReceived(DatagramPacket packet) {
                packetReceived(packet.getData(), 1);
            }
        });
    }

    public void setKnowledgeDataReceiver(KnowledgeDataReceiver knowledgeDataReceiver) {
        packetReceiver.setKnowledgeDataReceiver(knowledgeDataReceiver);
    }

    public KnowledgeDataSender getKnowledgeDataSender() {
        return packetSender;
    }

    // CALL THIS when received a packet through UDP
    @Override
    public void packetReceived(byte[] packet, double rssi) {
        packetReceiver.packetReceived(packet, rssi);
    }


    @Override
    public void sendPacket(byte[] packet, String recipient) {
        // SEND UDP packet via UDP interface
        udpBroadcast.sendPacket(packet);
    }

    public void finalize() {
        packetReceiver.clearCachedMessages();
    }
}
