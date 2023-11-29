package cs451.broadcast;

import cs451.Host;
import cs451.links.Link;
import cs451.links.PerfectLink;
import cs451.packet.Packet;
import cs451.message.Message;
import cs451.packet.PayloadPacketImpl;
import cs451.dataStructures.pair.Pair;
import cs451.message.PayloadMessageImpl;
import cs451.dataStructures.pair.PairImpl;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.NoSuchElementException;

/**
 * Implementation of Uniform Reliable Broadcast based on {@link Broadcast} following
 * the algorithm Majority-Ack Uniform Reliable Broadcast.
 * The algorithm is characterized by the following properties:
 * - Validity:          if pi and pj are correct, then every message broadcast by
 *                      pi is eventually delivered by pj.
 * - No duplication:    no message is delivered more than once.
 * - No creation:       no message is delivered unless it was broadcast.
 * - Uniform Agreement: for any message m, if any process delivers m, then every
 *                      correct process delivers m.
 */
public class URBBroadcast implements Broadcast {

    // num of packets handled by the algorithm concurrently.
    private static final int NUM_PACKETS = 8;
    private static final int NUM_THREADS = 3;
    private static final int NUM_ATTEMPTS = 3;
    private static final int DELTA_BOUND = 200;
    private static final int PACKET_LOWER_BOUND = 3;
    private final static int SOCKET_TERMINATION_TIME = 50;

    // messages that have been beb-delivered but not yet urb-delivered.
    // pair of (messageId, number of ack received)
    private final BlockingQueue<Pair>[] pending;

    // Packet sent - packet delivered
    private final AtomicInteger delta = new AtomicInteger(0);
    private final int myId;
    private final Link link;
    private final int numHosts;
    private int packetIdCounter; // id of the next packet to send
    private final ExecutorService executor;
    private final Consumer<Packet> deliverCallback;
    private final BlockingQueue<Packet> deliverBuffer;        // buffer used to send messages to beb
    private final BlockingQueue<Packet> packetSendBuffer;        // buffer used to send messages to beb
    private final ConcurrentSkipListSet<Integer>[] delivered;
    private final BlockingQueue<BroadcastMessageDto> sendBuffer; // buffer used to pack messages into packets

    /**
     * Constructor of {@link URBBroadcast}.
     *
     * @param myId:            the id of the current process.
     * @param port:            the port to listen to.
     * @param hosts:           the list of hosts.
     * @param deliverCallback: consumer of packets called every time a packet is urb-delivered.
     */
    public URBBroadcast(final int myId, final int port, List<Host> hosts, final Consumer<Packet> deliverCallback) {
        this.myId = myId;
        this.packetIdCounter = 0;
        this.numHosts = hosts.size();
        this.deliverCallback = deliverCallback;

        this.delivered = new ConcurrentSkipListSet[hosts.size()];
        for (var i = 0; i < hosts.size(); i++) {
            this.delivered[i] = new ConcurrentSkipListSet<>();
        }

        this.pending = new BlockingQueue[hosts.size()];
        for (var i = 0; i < hosts.size(); i++) {
            this.pending[i] = new LinkedBlockingQueue<>(URBBroadcast.NUM_PACKETS);
        }

        this.sendBuffer = new LinkedBlockingQueue<>(URBBroadcast.NUM_PACKETS);
        this.deliverBuffer = new LinkedBlockingQueue<>(URBBroadcast.NUM_PACKETS);
        this.packetSendBuffer = new LinkedBlockingQueue<>(URBBroadcast.NUM_PACKETS);
        this.executor = Executors.newFixedThreadPool(URBBroadcast.NUM_THREADS);
        this.executor.execute(this::handleSendBuffer);
        this.executor.execute(this::createPackets);
        this.executor.execute(this::handleDeliverBuffer);
        this.link = new PerfectLink(myId, port, hosts, this::deliver);
    }

    @Override
    public void broadcast(final int msgId, final byte[] payload) {
        try {
            this.sendBuffer.put(new BroadcastMessageDto(msgId, payload));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(URBBroadcast.SOCKET_TERMINATION_TIME, TimeUnit.MILLISECONDS)) {
                this.executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.executor.shutdownNow();
        }
        this.link.close();
    }

    private void deliver(final Packet packet) {
        try {
            System.out.println("URB(D): Putting packet in deliver buffer");
            this.deliverBuffer.put(packet);
            System.out.println("URB(D): Packet put in deliver buffer");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleDeliverBuffer() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                System.out.println("URB(HDB): Current deliver buffer size: " + this.deliverBuffer.size());
                Packet packet = this.deliverBuffer.take();
                if (!this.delivered[packet.getSenderId() - 1].contains(packet.getId())
                        && !(this.pending[packet.getSenderId() - 1].size() == URBBroadcast.NUM_PACKETS
                        && this.pending[packet.getSenderId() - 1].stream().noneMatch(pair -> pair.getLeft() == packet.getId()))
                ) {
                    System.out.println("UBR(HDB): Packet id: " + packet.getId() + " sender id: " + packet.getSenderId() + " receiver id: " + packet.getReceiverId());
                    if (this.pending[packet.getSenderId() - 1].stream().noneMatch(pair -> pair.getLeft() == packet.getId())) {
                        try {
                            System.out.println("URB: first time seen");
                            this.pending[packet.getSenderId() - 1].put(new PairImpl(packet.getId(), 0));
                            this.packetSendBuffer.put(packet);
                            System.out.println("URB: Sender id: " + packet.getSenderId() + " queue dim: " + this.pending[packet.getSenderId() - 1].size());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    System.out.println("URB(HDB): Incrementing ack received");
                    // increment number of ack received
                    this.pending[packet.getSenderId() - 1] = this.pending[packet.getSenderId() - 1].stream()
                            .map(pair -> pair.getLeft() == packet.getId() ? new PairImpl(pair.getLeft(), pair.getRight() + 1) : pair)
                            .collect(Collectors.toCollection(() -> new LinkedBlockingQueue<>(URBBroadcast.NUM_PACKETS)));

                    try {
                        System.out.println("URB(HDB): Number of ack received: " + this.pending[packet.getSenderId() - 1].stream()
                                .filter(pair -> pair.getLeft() == packet.getId()).findFirst().get().getRight());
                    } catch (NoSuchElementException e) {
                        Thread.currentThread().interrupt();
                        System.exit(1);
                    }

                    // check if message can be urb delivered
                    if (this.canDeliver(packet)) {
                        System.out.println("URB(HDB): FIFO ready");
                        this.delivered[packet.getSenderId() - 1].add(packet.getId());
                        this.pending[packet.getSenderId() - 1].removeIf(pair -> pair.getLeft() == packet.getId());
                        this.deliverCallback.accept(packet);
                        this.delta.decrementAndGet();
                        System.out.println("URB(HDB): Sender id: " + packet.getSenderId() + " queue dim: " + this.pending[packet.getSenderId() - 1].size());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            System.out.println("URB(HDB): Next iteration");
        }
    }

    private boolean canDeliver(final Packet packet) {
        // check if there is a pair with key message id
        return this.pending[packet.getSenderId() - 1].stream().anyMatch(pair -> pair.getLeft() == packet.getId())
            && this.pending[packet.getSenderId() - 1].stream().filter(pair -> pair.getLeft() == packet.getId())
                .findFirst().get().getRight() > ((this.numHosts-1) / 2);
    }

    /*
     * The packets are created only one time.
     */

    private void createPackets() {

        // Continuously try to create packets.
        int id;
        Packet packet;
        int numAttempts;
        Message paylodMessage;
        BroadcastMessageDto message;
        List<BroadcastMessageDto> messages;

        while (!Thread.currentThread().isInterrupted()) {

            if (this.delta.get() < URBBroadcast.DELTA_BOUND) {
                // Peek without removing from the queue.
                message = this.sendBuffer.peek();
                if (message != null) {
                    // Try NUM ATTEMPTS times to fill a packet to avoid
                    // sending too empty packets.
                    numAttempts = 0;
                    this.packetIdCounter++;
                    id = this.packetIdCounter;
                    messages = new ArrayList<>();
                    while (numAttempts < URBBroadcast.NUM_ATTEMPTS
                            && this.canContainMessage(PayloadMessageImpl.HEADER_SIZE + message.getPayload().length, messages.size())
                    ) {
                        try {
                            var messageToAdd = this.sendBuffer.take();
                            messages.add(messageToAdd);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        message = this.sendBuffer.peek();
                        // If there are yet at least PACKET_LOWER_BOUND messages in the packet,
                        // no need to wait for more messages.
                        if (message == null && messages.size() >= URBBroadcast.PACKET_LOWER_BOUND) {
                            break;
                        }
                        // Else wait for more messages.
                        while (message == null && numAttempts < URBBroadcast.NUM_ATTEMPTS) {
                            numAttempts++;
                        }
                    }

                    try {
                        // doing that I can ensure that the packets sent to the hosts are the same
                        System.out.println("URB(CP):Creating packet id: " + id);
                        for (int hostId = 1; hostId <= this.numHosts; hostId++) {
                            if (hostId != this.myId) {
                                packet = new PayloadPacketImpl(id, this.myId);
                                for (int msg = 0; msg < messages.size(); msg++) {
                                    message = messages.get(msg);
                                    paylodMessage = new PayloadMessageImpl(message.getPayload(), message.getMsgId(), this.myId, hostId);
                                    packet.addMessage(paylodMessage);
                                }
                                System.out.println("URB(CP) Putting packet in packet send buffer");
                                this.packetSendBuffer.put(packet);
                                System.out.println("URB(CP): Packet put in packet send buffer");
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    private void handleSendBuffer() {
        Packet packet;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                System.out.println("URB(HSB):Sending packet");
                packet = this.packetSendBuffer.take();
                System.out.println("URB(HSB):Sending received packet from: " + packet.getSenderId());
                this.link.send(packet);
                this.delta.incrementAndGet();
                System.out.println("URB(HSB):Packet sent");
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private boolean canContainMessage(final int messageLength, final int numMessages) {
        return (PayloadPacketImpl.PAYLOAD_HEADER_SIZE + Packet.INT_SIZE + messageLength <= PayloadPacketImpl.MAX_PAYLOAD_SIZE)
            && (numMessages + 1 <= PayloadPacketImpl.MAX_NUM_MESSAGES);
    }

    private class BroadcastMessageDto {
        private final int msgId;
        private final byte[] payload;

        public BroadcastMessageDto(final int msgId, final byte[] payload) {
            this.msgId = msgId;
            this.payload = payload;
        }

        public int getMsgId() {
            return msgId;
        }

        public byte[] getPayload() {
            return payload;
        }

    }

}
