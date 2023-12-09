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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
    private static final int NUM_PACKETS = 10;
    private static final int NUM_THREADS = 2;
    private static final int NUM_ATTEMPTS = 3;
    private static final int PACKET_LOWER_BOUND = 3;
    private static final int DELTA_BOUND = NUM_PACKETS;
    private final static int SOCKET_TERMINATION_TIME = 50;

    // messages that have been beb-delivered but not yet urb-delivered.
    // pair of (messageId, number of ack received)
    private final BlockingQueue<Pair>[] pending;

    // Packet sent - packet delivered
    private final int myId;
    private final AtomicInteger delta;
    private final TreeSet<Integer>[] windows; // next sequence number for each sender.
    private final Link link;
    private final int numHosts;
    private int packetIdCounter; // id of the next packet to send
    private final ExecutorService executor;
    private final Consumer<Packet> deliverCallback;
    private final BlockingQueue<Packet> deliverBuffer;           // buffer used to send messages to beb
    private final ConcurrentSkipListSet<Pair>[] delivered;       // beb delivered
    private final ConcurrentSkipListSet<Integer>[] deliveredURB;    // urb delivered
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
        this.delta = new AtomicInteger(0);

        this.windows = new TreeSet[this.numHosts];
        for (var i = 0; i < this.numHosts; i++) {
            this.windows[i] = new TreeSet<>();
            for (var j = 1; j <= URBBroadcast.NUM_PACKETS; j++) {
                this.windows[i].add(j);
            }
        }

        this.delivered = new ConcurrentSkipListSet[hosts.size()];
        for (var i = 0; i < hosts.size(); i++) {
            this.delivered[i] = new ConcurrentSkipListSet<>();
        }

        this.deliveredURB = new ConcurrentSkipListSet[hosts.size()];
        for (var i = 0; i < hosts.size(); i++) {
            this.deliveredURB[i] = new ConcurrentSkipListSet<>();
        }

        this.pending = new BlockingQueue[hosts.size()];
        for (var i = 0; i < hosts.size(); i++) {
            this.pending[i] = new LinkedBlockingQueue<>(Integer.MAX_VALUE);
        }

        this.sendBuffer = new LinkedBlockingQueue<>(URBBroadcast.NUM_PACKETS);
        this.deliverBuffer = new LinkedBlockingQueue<>(Integer.MAX_VALUE);
        this.executor = Executors.newFixedThreadPool(URBBroadcast.NUM_THREADS);
        this.executor.execute(this::createPackets);
        this.executor.execute(this::handleDeliverBuffer);
        this.link = new PerfectLink(myId, port, hosts.toArray(new Host[hosts.size()]), this::deliver);
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
            this.deliverBuffer.put(packet);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleDeliverBuffer() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Packet packet = this.deliverBuffer.take();

                this.link.send(packet.toAck());

                if (!this.delivered[packet.getOriginalSenderId() - 1].contains(new PairImpl(packet.getId(), packet.getSenderId()))
                    && !this.deliveredURB[packet.getOriginalSenderId() - 1].contains(packet.getId())
                ) {
                    if (this.pending[packet.getOriginalSenderId() - 1].stream().noneMatch(pair -> pair.getLeft() == packet.getId())) {
                        try {
                            this.pending[packet.getOriginalSenderId() - 1].put(new PairImpl(packet.getId(), 1));

                            // broadcast the packet to all the hosts
                            var messagesList = packet.getMessages();
                            for (int hostId = 1; hostId <= this.numHosts; hostId++) {
                                if (hostId != this.myId && hostId != packet.getSenderId() && hostId != packet.getOriginalSenderId()) {
                                    var bPacket = new PayloadPacketImpl(packet.getId(), this.myId, hostId, packet.getOriginalSenderId());
                                    for (int i = 0; i < messagesList.size(); i++) {
                                        var message = messagesList.get(i);
                                        bPacket.addMessage(new PayloadMessageImpl(message.getPayload(), message.getId(), packet.getOriginalSenderId(), hostId));
                                    }
                                    this.link.send(bPacket);
                                }
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    // increment number of ack received

                    Iterator it = this.pending[packet.getOriginalSenderId() - 1].iterator();
                    while (it.hasNext()) {
                        Pair pair = (Pair) it.next();
                        if (pair.getLeft() == packet.getId()) {
                            pair.setRight(pair.getRight() + 1);
                            break;
                        }
                    }

                    this.delivered[packet.getOriginalSenderId() - 1].add(new PairImpl(packet.getId(), packet.getSenderId()));
                    // check if message can be urb delivered
                    if (this.canDeliver(packet)) {
                        this.deliveredURB[packet.getOriginalSenderId() - 1].add(packet.getId());
                        this.pending[packet.getOriginalSenderId() - 1].removeIf(pair -> pair.getLeft() == packet.getId());
                        this.deliverCallback.accept(packet);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private boolean canDeliver(final Packet packet) {
        // check if there is a pair with key message id
        return this.pending[packet.getOriginalSenderId() - 1].stream().anyMatch(pair -> pair.getLeft() == packet.getId())
            && this.pending[packet.getOriginalSenderId() - 1].stream().filter(pair -> pair.getLeft() == packet.getId())
                .findFirst().get().getRight() > (this.numHosts / 2);
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

            if (true) {
                // Peek without removing from the queue.
                message = this.sendBuffer.peek();
                if (message != null) {
                    // Try NUM ATTEMPTS times to fill a packet to avoid
                    // sending too empty packets.
                    numAttempts = 0;
                    this.packetIdCounter++;
                    id = this.packetIdCounter;
                    messages = new ArrayList<>();
                    var payloadLength = message == null || message.getPayload() == null ? 0 : message.getPayload().length;
                    while (numAttempts < URBBroadcast.NUM_ATTEMPTS
                            && this.canContainMessage(PayloadMessageImpl.HEADER_SIZE + payloadLength, messages.size())
                    ) {
                        if (message == null) {
                            numAttempts++;
                            continue;
                        }
                        try {
                            var messageToAdd = this.sendBuffer.take();
                            messages.add(messageToAdd);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        message = this.sendBuffer.peek();
                        payloadLength = message == null || message.getPayload() == null ? 0 : message.getPayload().length;
                        // If there are yet at least PACKET_LOWER_BOUND messages in the packet,
                        // no need to wait for more messages.
                        if (message == null && messages.size() >= URBBroadcast.PACKET_LOWER_BOUND) {
                            break;
                        }
                        numAttempts++;
                    }

                    this.delta.incrementAndGet();

                    try {
                        // doing that I can ensure that the packets sent to the hosts are the same
                        for (int hostId = 1; hostId <= this.numHosts; hostId++) {
                            if (hostId != this.myId) {
                                packet = new PayloadPacketImpl(id, this.myId, hostId, this.myId);
                                for (int msg = 0; msg < messages.size(); msg++) {
                                    message = messages.get(msg);
                                    paylodMessage = new PayloadMessageImpl(message.getPayload(), message.getMsgId(), this.myId, hostId);
                                    packet.addMessage(paylodMessage);
                                }
                                this.link.send(packet);
                            }
                        }
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
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
