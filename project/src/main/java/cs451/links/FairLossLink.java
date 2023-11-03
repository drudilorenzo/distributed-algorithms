package cs451.links;

import cs451.Host;
import cs451.packet.Packet;
import cs451.message.Message;
import cs451.packet.PacketImpl;
import cs451.packet.PacketUtils;

import java.net.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * FairLossLink implementation. It is the weakest variant of the link abstractions.
 * It is characterized by the following properties:
 * - Fair-loss: If a correct process p infinitely often sends a message m to a
 *      correct process q, then q delivers m an infinite number of times.
 * - Finite duplication: If a correct process p sends a message m a finite number
 *      of times to process q, then m cannot be delivered an infinite number of times by q.
 * - No creation: If some process q delivers a message m with sender p, then m
 *      was previously sent to q by process p.
 * It implements the {@link Link} interface.
 */
public class FairLossLink implements Link {

    private final static int MAX_CAPACITY = 976;           // maximum send buffer capacity (for memory constraints)
    private final static int NUM_THREADS = 2;
    private static final int NUM_ATTEMPTS = 3;             // number of attempts to add more messages to a packet
    private final static int NUM_PACKETS_LOWER_BOUND = 3;  // lower bound on the number of packets to send (after num attempts tries it sends the packet anyway)
    private final static int SOCKET_TERMINATION_TIME = 50; // time to wait for the socket to close

    private final Host[] hosts;
    private DatagramSocket socket;
    private final ExecutorService executor;
    private final Consumer<Message> deliverer;
    private final BlockingDeque<Message> sendBuffer;

    /**
     * Constructor of {@link FairLossLink}.
     *
     * @param port:      the port to listen to.
     * @param hosts:     the list of hosts.
     * @param deliverer: consumer of packets called every time a packet is received.
     */
    public FairLossLink(final int port, final Host[] hosts, final Consumer<Message> deliverer) {
        try {
            this.socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println("FairLossLink: Could not open socket on port " + port);
        }
        this.deliverer = deliverer;
        this.hosts = new Host[hosts.length];
        this.sendBuffer = new LinkedBlockingDeque<>(FairLossLink.MAX_CAPACITY);
        this.executor = Executors.newFixedThreadPool(FairLossLink.NUM_THREADS);
        for (final var host : hosts) {
            this.hosts[host.getId() - 1] = host;
        }
        executor.submit(this::deliver);
        executor.submit(this::sendBuffer);
    }

    @Override
    public void send(final Message message) {
        try {
            this.sendBuffer.put(message);
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
        DatagramPacket datagram;
        while (!Thread.currentThread().isInterrupted()) {
            buf = new byte[Packet.MAX_PAYLOAD_SIZE];
            datagram = new DatagramPacket(buf, buf.length);
            try {
                this.socket.receive(datagram);
            } catch (IOException e) {
                // filter out the exception that is thrown when the socket is closed.
                // they are thrown when the close method is called.
                if (e instanceof SocketException && e.getMessage().equals("Socket closed")) {
                    Thread.currentThread().interrupt();
                    return;
                }
                System.err.println("FairLossLink: Could not deliver packet ");
                Thread.currentThread().interrupt();
                System.exit(1);
            }
            final var packet = PacketUtils.deserialize(datagram.getData());
            for (final var message : packet.getMessages()) {
                this.deliverer.accept(message);
            }
        }
    }

    private void sendBuffer() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                final var message = this.sendBuffer.poll();
                if (message == null) {
                    continue;
                }
                final var receiverId = message.getReceiverId();
                final var receiver = this.hosts[receiverId - 1];
                final byte[] buf = this.createPacket(message).serialize();
                final var datagram = new DatagramPacket(
                        buf,
                        buf.length,
                        InetAddress.getByName(receiver.getIp()),
                        receiver.getPort()
                );
                this.socket.send(datagram);
            } catch (IOException e) {
                // filter out the exception that is thrown when the socket is closed
                // they are thrown when the close method is called
                if (e instanceof SocketException && e.getMessage().equals("Socket closed")) {
                    Thread.currentThread().interrupt();
                    return;
                }
                System.err.println("FairLossLink: send problem ");
                Thread.currentThread().interrupt();
                System.exit(1);
            } catch (Exception e) {
                System.err.println("FairLossLink: unknown problem");
                Thread.currentThread().interrupt();
                System.exit(1);
            }
        }
    }

    /*
     * The packet is created only inside FairLossLink.
     */
    private Packet createPacket(final Message message) {
        final var packet = new PacketImpl();
        final int receiverId = message.getReceiverId();
        packet.addMessage(message);
        var iterator = this.sendBuffer.iterator();
        // if the packet is too small, try to add more messages to it.
        // this avoids sending packets that are too empty.
        var attempts = 0;
        while (iterator.hasNext() && attempts < FairLossLink.NUM_ATTEMPTS) {
            final var msg = iterator.next();
            if (msg.getReceiverId() == receiverId && packet.canContainMessage(msg.getLength())) {
                packet.addMessage(msg);
                iterator.remove();
            }
            if (packet.getLength() < FairLossLink.NUM_PACKETS_LOWER_BOUND && !iterator.hasNext()) {
                iterator = this.sendBuffer.iterator();
                attempts++;
            }
        }
        return packet;
    }

}
