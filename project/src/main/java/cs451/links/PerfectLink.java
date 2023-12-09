package cs451.links;

import cs451.Host;
import cs451.packet.Packet;
import cs451.packet.PayloadPacketImpl;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Perfect link (or reliable link) abstraction. It is the strongest variant of the link abstractions, and it has
 * or detecting and suppressing message duplicates, in addition to mechanisms for message retransmission.
 * It is characterized by the following properties:
 * 1) Reliable delivery: If a correct process p sends a message m to a correct
 *                       process q, then q eventually delivers m.
 * 2) No duplication:    No message is delivered by a process more than once.
 * 3) No creation:       If some process q delivers a message m with sender p, then m
 *                       was previously sent to q by process p.
 * It implements the {@link Link} interface and it uses the {@link FairLossLink} abstraction.
 */
public class PerfectLink implements Link {

    private static final int NUM_THREADS = 2;
    private static final int MEMORY_BOUND = 50;
    private static final int SEND_BUFFER_CAPACITY = 8;
    private static final long RETRANSMISSION_TIME = 100;
    private final static int SOCKET_TERMINATION_TIME = 50;

    private final int myId;
    private final FairLossLink fLink;
    private final ExecutorService executor;
    private final Consumer<Packet> deliverCallback;
    private final BlockingQueue<Packet>[] packetsSent;
    private final BlockingQueue<Packet> packetSendBuffer; // packet to send to the fair loss link

    /**
     * Constructor of {@link PerfectLink}.
     *
     * @param myId:      the id of the current process.
     * @param port:      the port to listen to.
     * @param hosts:     the list of hosts.
     * @param deliverCallback: consumer of packets called every time a packet is received.
     */
    public PerfectLink(final int myId, final int port, final Host[] hosts, final Consumer<Packet> deliverCallback) {
        this.myId = myId;
        this.deliverCallback = deliverCallback;
        this.packetsSent = new LinkedBlockingQueue[hosts.length];
        for (var i = 0; i < hosts.length; i++) {
            if (i + 1 == myId) {
                continue;
            }
            this.packetsSent[i] = new LinkedBlockingQueue<>(Integer.MAX_VALUE);
        }

        this.packetSendBuffer = new LinkedBlockingQueue<>(PerfectLink.SEND_BUFFER_CAPACITY);
        this.fLink = new FairLossLink(port, hosts, this::deliver);
        this.executor = Executors.newFixedThreadPool(PerfectLink.NUM_THREADS);
        this.executor.execute(this::retransmitPackets); // one thread to retransmit packets.
        this.executor.execute(this::sendPackets);       // one thread to send packets to fair-loss.
    }

    @Override
    public void send(final Packet packet) {
        try {
            packet.setTransmit(false);
            this.packetSendBuffer.put(packet);
            if (!packet.isAck()) {
                this.packetsSent[packet.getReceiverId() - 1].put(packet);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(PerfectLink.SOCKET_TERMINATION_TIME, TimeUnit.MILLISECONDS)) {
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
            var dummyPacket = new PayloadPacketImpl(packet.getId(), packet.getReceiverId(), packet.getSenderId(), packet.getOriginalSenderId());
            this.packetsSent[packet.getSenderId() - 1].remove(dummyPacket);
        } else {
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
                if (now - timers[i] >= PerfectLink.RETRANSMISSION_TIME) {
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
              
    private void sendPackets() {
        Packet packet;
        long MegaBytes = 1024 * 1024;
        long memoryInUse, totalMemory, freeMemory;
        while (!Thread.currentThread().isInterrupted()) {
            Runtime runtime = Runtime.getRuntime();
            // To convert from Bytes to MegaBytes:
            // 1 MB = 1024 KB and 1 KB = 1024 Bytes.
            // Therefore, 1 MB = 1024 * 1024 Bytes.
            totalMemory = runtime.totalMemory() / MegaBytes;       
            freeMemory = runtime.freeMemory() / MegaBytes;    
            memoryInUse = totalMemory - freeMemory;
            if (memoryInUse > PerfectLink.MEMORY_BOUND) {
                System.gc();
            }
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
