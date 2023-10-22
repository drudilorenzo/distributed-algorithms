package cs451.links;

import cs451.Host;
import cs451.packet.Packet;
import cs451.packet.PacketImpl;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.io.IOException;
import java.util.List;
import java.net.*;

/**
 * FairLossLink implementation. It is the weakest variant of the link abstractions.
 *
 * It is characterized by the following properties:
 * - Fair-loss: If a correct process p infinitely often sends a message m to a
 *      correct process q, then q delivers m an infinite number of times.
 * - Finite duplication: If a correct process p sends a message m a finite number
 *      of times to process q, then m cannot be delivered an infinite number of times by q.
 * - No creation: If some process q delivers a message m with sender p, then m
 *      was previously sent to q by process p.
 *
 * It implements the {@link Link} interface.
 */
public class FairLossLink implements Link {

    private final static int NUM_THREADS = 2;

    private final ExecutorService executor;
    private DatagramSocket socket;
    private final List<Host> hosts;
    private final Consumer<Packet> deliverer;
    private final BlockingDeque<DatagramPacket> sendBuffer;

    /**
     * Constructor of {@FairLossLink}.
     *
     * @param port:      the port to listen to.
     * @param hosts:     the list of hosts.
     * @param deliverer: consumer of packets called every time a packet is received.
     */
    public FairLossLink(final int port, final List<Host> hosts, final Consumer<Packet> deliverer) {
        try {
            this.socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println("Could not open socket on port " + port);
            e.printStackTrace();
        }

        this.deliverer = deliverer;
        this.hosts = new ArrayList<>(hosts.size());
        this.sendBuffer = new LinkedBlockingDeque<>();
        this.executor = Executors.newFixedThreadPool(FairLossLink.NUM_THREADS);

        for (final var host : hosts) {
            this.hosts.add(host.getId() - 1, host);
        }

        executor.submit(this::deliver);
        executor.submit(this::sendBuffer);
    }

    @Override
    public void send(final Packet packet) {
        final var receiverId = packet.getReceiverId();
        final var receiver = this.hosts.get(receiverId - 1);
        final byte[] buf = packet.getPacketInBytes();
        try {
            final var datagram = new DatagramPacket(buf, buf.length, InetAddress.getByName(receiver.getIp()), receiver.getPort());
            this.sendBuffer.put(datagram);
        } catch (Exception e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        this.executor.shutdown();
        this.socket.close();
    }

    private void deliver() {
        byte[] buf;
        DatagramPacket datagram;
        while (true) {
            // todo: check if this is the right size
            buf = new byte[Packet.MAX_PAYLOAD_SIZE];

            datagram = new DatagramPacket(buf, buf.length);
            try {
                this.socket.receive(datagram);
            } catch (IOException e) {
                System.err.println("Could not deliver packet ");
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            final var packet = this.deserializePacket(datagram.getData());
            this.deliverer.accept(packet);
        }
    }

    private void sendBuffer() {
        while (true) {
            try {
                final var datagram = this.sendBuffer.take();
                this.socket.send(datagram);
            } catch (Exception e) {
                System.err.println("Could not send packet ");
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    private Packet deserializePacket(final byte[] bytes){
        final ByteBuffer bb = ByteBuffer.wrap(bytes);

        final int payloadSize = bb.getInt();
        final byte[] payload = new byte[payloadSize];
        bb.get(payload);
        final boolean isAck =  bb.get() != 0;
        final int packetId = bb.getInt();
        final int senderId = bb.getInt();
        final int receiverId = bb.getInt();

        final Packet packet = new PacketImpl(bytes, packetId, isAck, senderId, receiverId);
        return packet;
    }

}
