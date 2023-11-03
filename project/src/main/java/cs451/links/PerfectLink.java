package cs451.links;

import cs451.Host;
import cs451.message.Message;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.function.Consumer;

/**
 * Perfect link (or reliable link) abstraction. It is the strongest variant of the link abstractions, and it has
 * or detecting and suppressing message duplicates, in addition to mechanisms for message retransmission.
 * It is characterized by the following properties:
 * - Reliable delivery: If a correct process p sends a message m to a correct
 *      process q, then q eventually delivers m.
 * - No duplication: No message is delivered by a process more than once.
 * - No creation: If some process q delivers a message m with sender p, then m
 *      was previously sent to q by process p.
 * It implements the {@link Link} interface and it uses the {@link StubbornLink} abstraction.
 */
public class PerfectLink implements Link {

    private final StubbornLink sLink;
    private final Set<Message> delivered;
    private final Consumer<Message> deliverer;

    /**
     * Constructor of {@link PerfectLink}.
     *
     * @param myId: the id of the host.
     * @param port: the port to listen to.
     * @param hosts: the list of hosts.
     * @param deliverer: consumer of packets called every time a packet is received.
     */
    public PerfectLink(final int myId, final int port, final List<Host> hosts, final Consumer<Message> deliverer) {
        this.delivered = new HashSet<>();
        this.deliverer = deliverer;
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
    private void deliver(final Message message) {
        if (!this.delivered.contains(message)) {
            this.delivered.add(message);
            this.deliverer.accept(message);
        }
    }

}
