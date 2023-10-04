package cs451.links;

import cs451.Host;
import cs451.packet.Packet;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

public class FairLossLink implements Link {

    private DatagramSocket socket;
    private final BlockingDeque<DatagramPacket> sendBuffer;

    private static final int NUM_THREADS = 2;

    public FairLossLink(final int port) {
        try {
            this.socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        this.sendBuffer = new LinkedBlockingDeque<>();
        final Executor executor = Executors.newFixedThreadPool(this.NUM_THREADS);
        executor.execute(this::sendBuffer);
        executor.execute(this::deliver);
    }

    @Override
    public void send(final Packet packet, final Host host) {
        byte[] buf = packet.getData();
        try {
            final var datagram = new DatagramPacket(buf, buf.length, InetAddress.getByName(host.getIp()), host.getPort());
            this.sendBuffer.add(datagram);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendBuffer() {
            while (true) {
                try {
                    final DatagramPacket packet = this.sendBuffer.take();
                    this.socket.send(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
    }

    public void deliver() {
        byte[] buf;
        DatagramPacket datagram;
        while (true) {
            buf = new byte[Packet.MAX_PAYLOAD_SIZE];
            datagram = new DatagramPacket(buf, buf.length);
            try {
                this.socket.receive(datagram);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
