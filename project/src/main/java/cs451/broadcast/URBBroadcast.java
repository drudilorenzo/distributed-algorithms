package cs451.broadcast;

import cs451.Host;
import cs451.message.Message;
import cs451.packet.Packet;
import cs451.dataStructures.pair.Pair;
import cs451.dataStructures.pair.PairImpl;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
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
    private static final int NUM_PACKETS = 8;
    private static final int NUM_THREADS = 1;

    // messages that have been beb-delivered but not yet urb-delivered.
    // pair of (messageId, number of ack received)
    private final BlockingQueue<Pair>[] pending;
    private final BlockingQueue<BroadcastMessageDto> sendingBuffer;

    private final int numHosts;
    private final Broadcast bebBroadcast;
    private final ExecutorService executor;
    private final Consumer<Message> deliverCallback;
    private final ConcurrentSkipListSet<Integer>[] delivered;

    /**
     * Constructor of {@link URBBroadcast}.
     *
     * @param myId:            the id of the current process.
     * @param port:            the port to listen to.
     * @param hosts:           the list of hosts.
     * @param deliverCallback: consumer of packets called every time a packet is urb-delivered.
     */
    public URBBroadcast(final int myId, final int port, List<Host> hosts, final Consumer<Message> deliverCallback) {
        this.numHosts = hosts.size();
        this.deliverCallback = deliverCallback;
        this.bebBroadcast = new BEBBroadcast(myId, port, hosts, this::deliver);

        this.delivered = new ConcurrentSkipListSet[hosts.size()];
        for (var i = 0; i < hosts.size(); i++) {
            this.delivered[i] = new ConcurrentSkipListSet<>();
        }

        this.pending = new BlockingQueue[hosts.size()];
        for (var i = 0; i < hosts.size(); i++) {
            this.pending[i] = new LinkedBlockingQueue<>(URBBroadcast.NUM_PACKETS);
        }

        this.sendingBuffer = new LinkedBlockingQueue<>(URBBroadcast.NUM_PACKETS);

        this.executor = Executors.newFixedThreadPool(URBBroadcast.NUM_THREADS);
        this.executor.execute(this::handleBuffer);
    }

    @Override
    public void broadcast(final int msgId, final byte[] payload, final int senderId) {
        try {
            this.sendingBuffer.put(new BroadcastMessageDto(msgId, payload, senderId));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
    }

    @Override
    public void close() {
        this.bebBroadcast.close();
    }

    private void deliver(Packet packet) {

        // if a message is not inside pending queue, broadcast it
        for (var message : packet.getMessages()) {
            if (!this.delivered[message.getOriginalSenderId() - 1].contains(message.getId())
                    && !(this.pending[message.getOriginalSenderId() - 1].size() == URBBroadcast.NUM_PACKETS
                        && this.pending[message.getOriginalSenderId() - 1].stream().noneMatch(pair -> pair.getLeft() == message.getId()))
            ) {
                System.out.println("Received message " + message.getId() + " from " + message.getOriginalSenderId());
                // TODO: if not in pending, broadcast it
                if (this.pending[message.getOriginalSenderId() - 1].stream().noneMatch(pair -> pair.getLeft() == message.getId())) {
                    System.out.println("NOT IN PENDING");
                    try {
                        this.pending[message.getOriginalSenderId() - 1].put(new PairImpl(message.getId(), 0));
                        System.out.println("PUT IN PENDING");
                        this.sendingBuffer.put(new BroadcastMessageDto(message.getId(), message.getPayload(), message.getOriginalSenderId()));
                        System.out.println("PUT IN BUFFER");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                // increment number of ack received
                this.pending[message.getOriginalSenderId() - 1] = this.pending[message.getOriginalSenderId() - 1].stream()
                        .map(pair -> pair.getLeft() == message.getId() ? new PairImpl(pair.getLeft(), pair.getRight() + 1) : pair)
                        .collect(Collectors.toCollection(() -> new LinkedBlockingQueue<>(URBBroadcast.NUM_PACKETS)));

                try {
                    System.out.println("Number of ack received: " + this.pending[message.getOriginalSenderId() - 1].stream()
                            .filter(pair -> pair.getLeft() == message.getId()).findFirst().get().getRight());
                } catch (NoSuchElementException e) {
                    System.out.println("message " + message.getId() + " from " + message.getOriginalSenderId());
                    Thread.currentThread().interrupt();
                    System.exit(1);
                }

                // check if message can be urb delivered
                if (this.canDeliver(message)) {
                    System.out.println("CAN DELIVER");
                    this.delivered[message.getOriginalSenderId() - 1].add(message.getId());
                    this.pending[message.getOriginalSenderId() - 1].removeIf(pair -> pair.getLeft() == message.getId());
                    this.deliverCallback.accept(message);
                }
            } else {
                System.out.println("Cannot contain");
            }
        }
    }

    private boolean canDeliver(Message message) {
        // check if there is a pair with key message id
        return this.pending[message.getOriginalSenderId() - 1].stream().anyMatch(pair -> pair.getLeft() == message.getId())
            && this.pending[message.getOriginalSenderId() - 1].stream().filter(pair -> pair.getLeft() == message.getId())
                .findFirst().get().getRight() > (this.numHosts / 2);
    }

    private void handleBuffer() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                var messageDto = this.sendingBuffer.take();
                this.bebBroadcast.broadcast(
                        messageDto.getMsgId(),
                        messageDto.getPayload(),
                        messageDto.getSenderId()
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private class BroadcastMessageDto {
        private final int msgId;
        private final byte[] payload;
        private final int senderId;

        public BroadcastMessageDto(final int msgId, final byte[] payload, final int senderId) {
            this.msgId = msgId;
            this.payload = payload;
            this.senderId = senderId;
        }

        public int getMsgId() {
            return msgId;
        }

        public byte[] getPayload() {
            return payload;
        }

        public int getSenderId() {
            return senderId;
        }
    }

}
