package cs451.links;

import cs451.Host;
import cs451.packet.Packet;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.function.Consumer;

/**
 * Perfect link (or reliable link) abstraction. It is the strongest variant of the link abstractions, and it has
 * or detecting and suppressing message duplicates, in addition to mechanisms for message retransmission.
 *
 * It is characterized by the following properties:
 * - Reliable delivery: If a correct process p sends a message m to a correct
 *      process q, then q eventually delivers m.
 * - No duplication: No message is delivered by a process more than once.
 * - No creation: If some process q delivers a message m with sender p, then m
 *      was previously sent to q by process p.
 *
 * It implements the {@link Link} interface and it uses the {@link StubbornLink} abstraction.
 */
public class PerfectLink implements Link {

    private final Consumer<Packet> deliverer;
    private final Set<Packet> delivered;
    private final StubbornLink sLink;

    public PerfectLink(final int port, final List<Host> hosts, final Consumer<Packet> deliverer) {
        this.delivered = new HashSet<>();
        this.deliverer = deliverer;
        this.sLink = new StubbornLink(port, hosts, this::deliver);
    }

    @Override
    public void send(final Packet packet) {
        this.sLink.send(packet);
    }

    @Override
    public void close() {
        this.sLink.close();
    }

    /*
     * Deliver a packet if it hasn't been delivered yet.
     */
    private void deliver(final Packet packet) {
        if (!this.delivered.contains(packet)) {
            this.delivered.add(packet);
            this.deliverer.accept(packet);
        }
    }
}
