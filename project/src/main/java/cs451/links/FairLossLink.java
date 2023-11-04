package cs451.links;

import cs451.Host;
import cs451.packet.Packet;
import cs451.message.Message;
import cs451.packet.PacketUtils;
import cs451.packet.PayloadPacketImpl;

import java.net.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * FairLossLink implementation. It is the weakest variant of the link abstractions.
 * It is characterized by the following properties:
 * 1) Fair-loss:          If a correct process p infinitely often sends a message m to a correct
 *                        process q, then q delivers m an infinite number of times.
 * 2) Finite duplication: If a correct process p sends a message m a finite number of times to process
*                         q, then m cannot be delivered an infinite number of times by q.
 * 3) No creation:        If some process q delivers a message m with sender p, then m was previously sent
 *                        to q by process p.
 * It implements the {@link Link} interface.
 */
public class FairLossLink implements Link {

    private final static int NUM_THREADS = 2;
    private final static int MAX_CAPACITY = 32;            // maximum send buffer capacity.
    private final static int SOCKET_TERMINATION_TIME = 50; // time to wait for the socket to close.

    private final Host[] hosts;
    private DatagramSocket socket;
    private final ExecutorService executor;
    private final Consumer<Packet> sendCallback;    // callback to call when a packet is sent.
    private final Consumer<Packet> deliverCallback; // callback to call when a packet is received.
    private final BlockingQueue<Packet> sendBuffer; // buffer of packets to send.

    /**
     * Constructor of {@link FairLossLink}.
     *
     * @param port:            the port to listen to.
     * @param hosts:           the list of hosts.
     * @param deliverCallback: consumer of packets called every time a packet is received.
     * @param sendCallback:    consumer of packets called every time a packet is sent.
     */
    public FairLossLink(final int port, final Host[] hosts,
        final Consumer<Packet> deliverCallback, Consumer<Packet> sendCallback) {
        try {
            this.socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println("FairLossLink: Could not open socket on port " + port);
        }
        this.sendCallback = sendCallback;
        this.deliverCallback = deliverCallback;
        this.hosts = new Host[hosts.length];
        this.sendBuffer = new LinkedBlockingQueue<>(FairLossLink.MAX_CAPACITY);
        for (final var host : hosts) {
            this.hosts[host.getId() - 1] = host;
        }
        this.executor = Executors.newFixedThreadPool(FairLossLink.NUM_THREADS);
        executor.execute(this::deliver);    // one thread to deliver packets.
        executor.execute(this::sendBuffer); // one thread to send packets.
    }

    @Override
    public void send(final Message message) {
        throw new UnsupportedOperationException("FairLossLink sends packets.");
    }

    /**
     * Send a packet.
     * 
     * @param packet: the packet to send.
     */
    public void send(final Packet packet) {
        try {
            this.sendBuffer.put(packet);
        } catch (InterruptedException e) {
            System.err.println("FairLossLink: Could not send packet");
            Thread.currentThread().interrupt();
            System.exit(1);
        }
    }

    @Override
    public void close() {
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(FairLossLink.SOCKET_TERMINATION_TIME, TimeUnit.MILLISECONDS)) {
                this.executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.executor.shutdownNow();
        }
        this.socket.close();
    }

    private void deliver() {
        byte[] buf;
        Packet packet;
        DatagramPacket datagram;
        while (!Thread.currentThread().isInterrupted()) {
            buf = new byte[PayloadPacketImpl.MAX_PAYLOAD_SIZE];
            datagram = new DatagramPacket(buf, buf.length);
            try {
                this.socket.receive(datagram);
            } catch (IOException e) {
                // filter out the exception that is thrown when the socket is closed.
                // they are thrown when the close method is called.
                if (e instanceof SocketException && e.getMessage().equals("Socket closed")) {
                    return;
                }
                System.err.println("FairLossLink: Could not deliver packet ");
                Thread.currentThread().interrupt();
                System.exit(1);
            }
            packet = PacketUtils.deserialize(datagram.getData());
            this.deliverer.accept(packet);
        }
    }

    private void sendBuffer() {
        int j = 0;
        Packet packet;
        int receiverId;
        Host receiver;
        DatagramPacket datagram;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                packet = this.sendBuffer.take();
                receiverId = packet.getReceiverId();
                receiver = this.hosts[receiverId - 1];
                datagram = new DatagramPacket(
                        packet.serialize(),
                        packet.getLength(),
                        InetAddress.getByName(receiver.getIp()),
                        receiver.getPort()
                );
                this.socket.send(datagram);
                this.senderCallback.accept(packet);
            } catch (IOException e) {
                // filter out the exception that is thrown when the socket is closed
                // they are thrown when the close method is called
                if (e instanceof SocketException && e.getMessage().equals("Socket closed")) {
                    return;
                }
                System.err.println("FairLossLink: send problem.");
                Thread.currentThread().interrupt();
                System.exit(1);
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                System.err.println("FairLossLink: unknown problem.");
                Thread.currentThread().interrupt();
                System.exit(1);
            }
        }
    }

}
