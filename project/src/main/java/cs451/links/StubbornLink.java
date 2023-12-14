package cs451.links;

import cs451.Host;
import cs451.utils.Utils;
import cs451.packet.Packet;
import cs451.message.Message;
import cs451.packet.PayloadPacketImpl;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Stubborn link abstraction. The implementation doesn't retransmit packets forever but uses
 * an ack mechanism to retransmit only the packets that were not delivered.
 * It is characterized by the following properties:
 * 1) Stubborn delivery: If a correct process p sends a message m once to a correct
 *                       process q, then q delivers m an infinite number of times.
 * 2) No creation:       If some process q delivers a message m with sender p, then m
 *                       was previously sent to q by process p.
 * It implements the {@link Link} interface and it uses the {@link FairLossLink} abstraction.
 */
public class StubbornLink implements Link {

    private static final int NUM_THREADS = 3;
    private static final int NUM_ATTEMPTS = 3;
    private static final int PACKET_LOWER_BOUND = 3;
    private static final int SEND_BUFFER_CAPACITY = 8;
    private static final long RETRANSMISSION_TIME = 100;
    private final static int SOCKET_TERMINATION_TIME = 50;

    private final int myId;
    /* Number of messages sent to each receiver.
     * It is used as id for the packets sent to the receiver.
     * The final and unique id of a packet is given by the pair (id, senderId).
     */
    private final int[] counters;
    private final FairLossLink fLink;
    private final ExecutorService executor;
    // Packets sent that wait for the ack.
    // Key: the packet.
    // Value: true if the packet can be retransmitted, false otherwise.
    private final BlockingQueue<Packet>[] packetsSent;
    private final Consumer<Packet> deliverCallback;
    private final BlockingQueue<Packet> packetSendBuffer;           // packet to send to the fair loss link
    private final BlockingQueue<Message>[] messageSendBuffer;       // messages that have to be added to a packet

    /**
     * Constructor of {@link StubbornLink}.
     *
     * @param myId:      the id of the current process.
     * @param port:      the port to listen to.
     * @param hosts:     the list of hosts.
     * @param deliverCallback: consumer of packets called every time a packet is received.
     */
    public StubbornLink(final int myId, final int port, final Host[] hosts, final Consumer<Packet> deliverCallback) {
        this.myId = myId;
        this.deliverCallback = deliverCallback;
        this.counters = new int[hosts.length];
        Arrays.fill(this.counters, 0);
        this.packetsSent = new LinkedBlockingQueue[hosts.length];
        for (var i = 0; i < hosts.length; i++) {
            if (i + 1 == myId) {
                continue;
            }
            this.packetsSent[i] = new LinkedBlockingQueue<>(StubbornLink.SEND_BUFFER_CAPACITY);
        }
        this.messageSendBuffer = new LinkedBlockingQueue[hosts.length];
        for (int i = 0; i < hosts.length; i++) {
            if (i + 1 == myId) {
                continue;
            }
            this.messageSendBuffer[i] = new LinkedBlockingQueue<>(StubbornLink.SEND_BUFFER_CAPACITY);
        }
        this.packetSendBuffer = new LinkedBlockingQueue<>(StubbornLink.SEND_BUFFER_CAPACITY);
        this.fLink = new FairLossLink(port, hosts, this::deliver);
        this.executor = Executors.newFixedThreadPool(StubbornLink.NUM_THREADS);
        this.executor.execute(this::createPackets);     // one thread to create packets.
        this.executor.execute(this::retransmitPackets); // one thread to retransmit packets.
        this.executor.execute(this::sendPackets);       // one thread to send packets to fair-loss.
    }

    @Override
    public void send(final Message message) {
        try {
            this.messageSendBuffer[message.getReceiverId() - 1].put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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

    private void deliver(final Packet packet) {
        if (packet.isAck()) {
            // need to create a dummy payload packet to remove it from the set
            var dummyPacket = new PayloadPacketImpl(packet.getId(), packet.getReceiverId(), packet.getSenderId());
            this.packetsSent[packet.getSenderId() - 1].remove(dummyPacket);
        } else {
            try {
                this.packetSendBuffer.put(packet.toAck());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            this.deliverCallback.accept(packet);
        }
    }

    private void retransmitPackets() {
        // Use a timer for each receiver.
        final var timers = new long[this.packetsSent.length];
        Arrays.fill(timers, System.currentTimeMillis());
        while (!Thread.currentThread().isInterrupted()) {
            for (var i = 0; i < this.packetsSent.length; i++) {
                if (this.myId == i + 1) {
                    continue;
                }
                final var now = System.currentTimeMillis();
                // Retransmit the messages sent to the receiver i if the timer is expired.
                if (now - timers[i] >= StubbornLink.RETRANSMISSION_TIME) {
                    for (var packet : this.packetsSent[i]) {
                        try {
                            if (packet.canTransmit()) {
                                packet.setTransmit(false);
                                this.packetSendBuffer.put(packet);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    timers[i] = now;
                }
            }
        }
    }

    /*
     * The packets are created only one time.
     */
    private void createPackets() {
        // Continuously try to create packets.
        int id;
        Packet packet;
        int numAttempts;
        while (!Thread.currentThread().isInterrupted()) {
            for (var i = 0; i < this.messageSendBuffer.length; i++) {
                if (this.myId == i + 1) {
                    continue;
                }
                // Peek without removing from the queue.
                Message message = this.messageSendBuffer[i].peek();
                if (message != null) {
                    // Try NUM ATTEMPTS times to fill a packet to avoid
                    // sending too empty packets.
                    numAttempts = 0;
                    this.counters[i]++;
                    id = this.counters[i];
                    packet = new PayloadPacketImpl(id, this.myId);
                    while (numAttempts < StubbornLink.NUM_ATTEMPTS && packet.canContainMessage(message.getLength())) {
                        try {
                            var messageToAdd = this.messageSendBuffer[i].take();
                            packet.addMessage(messageToAdd);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        message = this.messageSendBuffer[i].peek();
                        // If there are yet at least PACKET_LOWER_BOUND messages in the packet,
                        // no need to wait for more messages.
                        if (message == null && packet.getMessages().size() >= StubbornLink.PACKET_LOWER_BOUND) {
                            break;
                        }
                        // Else wait for more messages.
                        while (message == null && numAttempts < StubbornLink.NUM_ATTEMPTS) {
                            numAttempts++;
                        }
                    }
                    try {
                        this.packetSendBuffer.put(packet);
                        this.packetsSent[i].put(packet);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    private void sendPackets() {
        Packet packet;
        while (!Thread.currentThread().isInterrupted()) {
            Utils.checkGc();
            try {
                packet = this.packetSendBuffer.take();
                this.fLink.send(packet);
           } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
               return;
           }
        }
    }
    
}
