package cs451.links;

import cs451.Host;
import cs451.message.Message;
import cs451.message.PayloadMessageImpl;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Stubborn link abstraction. The implementation doesn't retransmit packets forever but uses
 * an ack mechanism to retransmit only the packets that were not delivered.
 * It is characterized by the following properties:
 * - Stubborn delivery: If a correct process p sends a message m once to a correct
 *      process q, then q delivers m an infinite number of times.
 * - No creation: If some process q delivers a message m with sender p, then m
 *      was previously sent to q by process p.
 * It implements the {@link Link} interface and it uses the {@link FairLossLink} abstraction.
 */
public class StubbornLink implements Link {

    private static final int NUM_THREADS = 1;
    private static final long RETRANSMISSION_TIME = 100;
    private final static int SOCKET_TERMINATION_TIME = 50;

    private final int myId;
    private final FairLossLink fLink;
    private final ExecutorService executor;
    private final Consumer<Message> deliverer;
    private final Set<Message>[] messagesSent; // array of messages sent divided by receiver (waiting for ack)

    /**
     * Constructor of {@link StubbornLink}.
     *
     * @param myId: the id of the current process.
     * @param port:  the port to listen to.
     * @param hosts: the list of hosts.
     * @param deliverer: consumer of packets called every time a packet is received.
     */
    public StubbornLink(final int myId, final int port, final Host[] hosts, final Consumer<Message> deliverer) {
        this.myId = myId;
        this.deliverer = deliverer;
        this.messagesSent = new Set[hosts.length];
        for (var i = 0; i < hosts.length; i++) {
            if (i + 1 == myId) {
                continue;
            }
            this.messagesSent[i] = ConcurrentHashMap.newKeySet();
        }
        this.fLink = new FairLossLink(port, hosts, this::deliver);
        this.executor = Executors.newFixedThreadPool(StubbornLink.NUM_THREADS);
        this.executor.execute(this::retransmitPackets);
    }

    @Override
    public void send(final Message message) {
        this.fLink.send(message);
        this.messagesSent[message.getReceiverId() - 1].add(message);
    }

    @Override
    public void close() {
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(StubbornLink.SOCKET_TERMINATION_TIME, TimeUnit.MILLISECONDS)) {
                this.executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.executor.shutdownNow();
        }
        this.fLink.close();
    }

    public void deliver(final Message message) {
        if (message.isAck()) {
            // need to create a dummy payload message to remove it from the set
            var m = new PayloadMessageImpl(
                    message.getPayload(),
                    message.getId(),
                    message.getReceiverId(),
                    message.getSenderId()
            );
            this.messagesSent[message.getSenderId() - 1].remove(m);
        } else {
            this.send(message.toAck());
            this.deliverer.accept(message);
        }
    }

    private void retransmitPackets() {
        // Use a timer for each receiver.
        final var timers = new long[this.messagesSent.length];
        Arrays.fill(timers, System.currentTimeMillis());
        while (!Thread.currentThread().isInterrupted()) {
            for (var i = 0; i < this.messagesSent.length; i++) {
                if (this.myId == i + 1) {
                    continue;
                }
                final var now = System.currentTimeMillis();
                // Retransmit the messages sent to the receiver i if the timer is expired.
                if (now - timers[i] >= StubbornLink.RETRANSMISSION_TIME) {
                    for (var m : this.messagesSent[i]) {
                        this.fLink.send(m);
                    }               
                    // final var messages = this.messagesSent[i];
                    // messages.iterator().forEachRemaining(this.fLink::send);
                    timers[i] = now;
                }
            }
        }
    }
    
}
