package cs451.links;

import cs451.Host;
import cs451.packet.Packet;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Stubborn link abstraction. The implementation doesn't retransmit packets forever but uses
 * an ack mechanism to retransmit only the packets that were not delivered.
 *
 * It is characterized by the following properties:
 * - Stubborn delivery: If a correct process p sends a message m once to a correct
 *      process q, then q delivers m an infinite number of times.
 * - No creation: If some process q delivers a message m with sender p, then m
 *      was previously sent to q by process p.
 *
 * It implements the {@link Link} interface and it uses the {@link FairLossLink} abstraction.
 */
public class StubbornLink implements Link {

    private static final int NUM_THREADS = 2;
    private static final long RETRANSMISSION_TIME = 100;

    private final FairLossLink fLink;
    private final Set<Packet> packetSent;           // packets sent (waiting for ack)
    private final Consumer<Packet> deliverer;
    private final BlockingDeque<Packet> sendBuffer; // packets to send
    private final ScheduledExecutorService executor;

    /**
     * Constructor of {@StubbornLink}.
     *
     * @param port:  the port to listen to.
     * @param hosts: the list of hosts.
     * @param deliverer: consumer of packets called every time a packet is received.
     */
    public StubbornLink(final int port, final List<Host> hosts, final Consumer<Packet> deliverer) {
        this.deliverer = deliverer;
        this.sendBuffer = new LinkedBlockingDeque<>();
        this.packetSent = ConcurrentHashMap.newKeySet();
        this.fLink = new FairLossLink(port, hosts, this::deliver);
        this.executor = new ScheduledThreadPoolExecutor(StubbornLink.NUM_THREADS);

        this.executor.submit(this::sendBuffer);
        this.executor.scheduleWithFixedDelay(
                this::retransmitPackets,
                StubbornLink.RETRANSMISSION_TIME,
                StubbornLink.RETRANSMISSION_TIME,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void send(final Packet packet) {
        try {
            this.sendBuffer.addFirst(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        this.executor.shutdown();
        this.fLink.close();
    }

    private void sendBuffer() {
        while (true) {
            try {
                final var packet = this.sendBuffer.take();
                this.fLink.send(packet);
                this.packetSent.add(packet);
            } catch (Exception e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void deliver(final Packet packet) {
        if (packet.isAck()) {
            this.packetSent.remove(packet);
        } else {
            this.deliverer.accept(packet);
            this.sendBuffer.addFirst(packet.toAck());
        }
    }

    // todo: now there is a common timer for all the packets, maybe we can have a timer for each packet
    // todo: multiple equal packets may be present in the buffer

    private void retransmitPackets() {
        try {
            if (packetSent.isEmpty()) {
                return;
            }
            final var iter = packetSent.iterator();
            while (iter.hasNext()) {
                final var packet = iter.next();
                this.sendBuffer.addLast(packet);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

}
