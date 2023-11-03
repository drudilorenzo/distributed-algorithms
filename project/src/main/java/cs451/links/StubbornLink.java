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

    private static final int NUM_THREADS = 3;
    private static final long RETRANSMISSION_TIME = 100;
    private final static int SOCKET_TERMINATION_TIME = 50;

    private final FairLossLink fLink;
    private final Consumer<Message> deliverer;
    private final ScheduledExecutorService executor;
    private final Set<Message>[] messagesSent; // array of messages sent divided by receiver (waiting for ack)

    /**
     * Constructor of {@link StubbornLink}.
     *
     * @param port:  the port to listen to.
     * @param hosts: the list of hosts.
     * @param deliverer: consumer of packets called every time a packet is received.
     */
    public StubbornLink(final int myId, final int port, final Host[] hosts, final Consumer<Message> deliverer) {
        this.deliverer = deliverer;
        this.messagesSent = new Set[hosts.length];
        for (var i = 0; i < hosts.length; i++) {
            if (i + 1 == myId) {
                continue;
            }
            this.messagesSent[i] = ConcurrentHashMap.newKeySet();
        }
        this.fLink = new FairLossLink(port, hosts, this::deliver);
        this.executor = new ScheduledThreadPoolExecutor(StubbornLink.NUM_THREADS);
        // creating one task for each receiver to retransmit the packets that were not delivered
        for (var i = 0; i < hosts.length; i++) {
            final var receiverId = i + 1;
            if (receiverId != myId) {
                this.executor.scheduleWithFixedDelay(
                        () -> this.retransmitPacketsOfReceiver(receiverId),
                        StubbornLink.RETRANSMISSION_TIME,
                        StubbornLink.RETRANSMISSION_TIME,
                        TimeUnit.MILLISECONDS
                );
            }
        }
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
                    message.getMessageId(),
                    message.getReceiverId(),
                    message.getSenderId()
            );
            this.messagesSent[message.getSenderId() - 1].remove(m);
        } else {
            this.send(message.toAck());
            this.deliverer.accept(message);
        }
    }

    private void retransmitPacketsOfReceiver(final int receiverId) {
        final var messages = this.messagesSent[receiverId - 1];
        // early return if there are no messages to retransmit
        if (messages.isEmpty()) {
            return;
        }
        messages.iterator().forEachRemaining(this.fLink::send);
    }
    
}
