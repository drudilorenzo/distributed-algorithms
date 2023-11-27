package cs451.broadcast;

import cs451.Host;
import cs451.links.Link;
import cs451.message.Message;
import cs451.packet.Packet;
import cs451.links.PerfectLink;
import cs451.message.PayloadMessageImpl;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Implementation of Best-Effort Broadcast based on {@link Broadcast}.
 * It is characterized by the following properties:
 * - Validity: if pi and pj are correct, then every message broadcast by
 *             pi is eventually delivered by pj.
 * - No duplication: no message is delivered more than once.
 * - No creation: no message is delivered unless it was broadcast.
 */
public class BEBBroadcast implements Broadcast {

    private static final int BUFFER_SIZE = 8;
    private static final int NUM_THREADS = 1;
    private static final int SOCKET_TERMINATION_TIME = 50;

    private final int myId;
    private final Link link;
    private final List<Host> hosts;
    private final ExecutorService executor;
    private final Consumer<Packet> deliverCallback;
    private final BlockingQueue<Packet> deliverBuffer;

    public BEBBroadcast(final int myId, final int port, final List<Host> hosts, final Consumer<Packet> deliverCallback ) {
        this.myId = myId;
        this.hosts = hosts;
        this.deliverCallback = deliverCallback;
        this.link = new PerfectLink(myId, port, hosts, this::deliver);
        this.deliverBuffer = new LinkedBlockingQueue<>(BEBBroadcast.BUFFER_SIZE);
        this.executor = Executors.newFixedThreadPool(BEBBroadcast.NUM_THREADS);
        this.executor.execute(this::handleBuffer);
    }

    @Override
    public void broadcast(final int msgId, final byte[] payload, final int senderId) {
        for (int hostId = 1; hostId <= this.hosts.size(); hostId++) {
            if (hostId != this.myId) {
                var message = new PayloadMessageImpl(payload, msgId, this.myId, hostId, senderId);
                this.link.send(message);
            }
        }
    }

    @Override
    public void close() {
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(BEBBroadcast.SOCKET_TERMINATION_TIME, TimeUnit.MILLISECONDS)) {
                this.executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.executor.shutdownNow();
        }
        this.link.close();
    }

    private void deliver(Packet packet) {
        try {
            this.deliverBuffer.put(packet);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleBuffer() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                var packet = this.deliverBuffer.take();
                this.deliverCallback.accept(packet);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

}
