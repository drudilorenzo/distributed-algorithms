package cs451.links;

import cs451.Host;
import cs451.message.Message;
import cs451.packet.Packet;

import java.util.LinkedList;
import java.util.Set;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Perfect link (or reliable link) abstraction. It is the strongest variant of the link abstractions, and it has
 * or detecting and suppressing message duplicates, in addition to mechanisms for message retransmission.
 * It is characterized by the following properties:
 * 1) Reliable delivery: If a correct process p sends a message m to a correct
 *                       process q, then q eventually delivers m.
 * 2) No duplication:    No message is delivered by a process more than once.
 * 3) No creation:       If some process q delivers a message m with sender p, then m
 *                       was previously sent to q by process p.
 * It implements the {@link Link} interface and it uses the {@link StubbornLink} abstraction.
 */
public class PerfectLink implements Link {

    private final StubbornLink sLink;
    private final List<Integer>[] delivered;
    private final BiConsumer<Integer, Integer> deliverCallback;

    /**
     * Constructor of {@link PerfectLink}.
     *
     * @param myId:      the id of the host.
     * @param port:      the port to listen to.
     * @param hosts:     the list of hosts.
     * @param deliverCallback: consumer of packets called every time a packet is received.
     */
    public PerfectLink(final int myId, final int port,
        final List<Host> hosts, final BiConsumer<Integer, Integer> deliverCallback) {
        // Use a set of delivered messages for each sender host.
        this.delivered = new LinkedList[hosts.size()];
        for (var i = 0; i < hosts.size(); i++) {
            this.delivered[i] = new LinkedList<>();
        }
        this.deliverCallback = deliverCallback;
        var hostsArray = new Host[hosts.size()];
        hosts.toArray(hostsArray);
        this.sLink = new StubbornLink(myId, port, hostsArray, this::deliver);
    }

    @Override
    public void send(final Message message) {
        this.sLink.send(message);
    }

    @Override
    public void close() {
        this.sLink.close();
    }

    /*
     * Deliver a packet if it hasn't been delivered yet.
     */
    private void deliver(final Packet packet) {
        final var senderId = packet.getSenderId();
        final var packetId = packet.getId();
        if (!this.delivered[senderId - 1].contains(packetId)) {
            this.delivered[senderId - 1].add(packetId);
            for (final var message : packet.getMessages()) {
                this.deliverCallback.accept(message.getId(), senderId);
            }
        }
    }

}
