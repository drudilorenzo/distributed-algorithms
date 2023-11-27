package cs451.broadcast;

import cs451.Host;
import cs451.message.Message;
import cs451.packet.Packet;
import cs451.dataStructures.intRanges.IntRanges;
import cs451.dataStructures.intRanges.IntRangesImpl;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Implementation of FIFO {@link Broadcast}.
 * Its implementation is based on URB Broadcast adding the following property:
 * - FIFO delivery: messages from the same sender are delivered in the same sequence as they were
 *                  broadcast by the sender.
 */
public class FIFOBroadcast implements Broadcast {

    private final Broadcast urbBroadcast;
    private final BiConsumer<Integer, Integer> deliverCallback;
    private final int[] currentSeq;    // current sequence number for each sender.
    private final IntRanges[] waiting; // messages waiting to be delivered for each sender.

    /**
     * Constructor of {@link FIFOBroadcast}.
     *
     * @param myId:            the id of the current process.
     * @param port:            the port to listen to.
     * @param hosts:           the list of hosts.
     * @param deliverCallback: consumer of packets called every time a packet is urb-delivered.
     */
    public FIFOBroadcast(final int myId, final int port, List<Host> hosts, final BiConsumer<Integer, Integer> deliverCallback) {
        this.deliverCallback = deliverCallback;
        this.urbBroadcast = new URBBroadcast(myId, port, hosts, this::deliver);
        this.currentSeq = new int[hosts.size()];
        for (var i = 0; i < hosts.size(); i++) {
            this.currentSeq[i] = 1; // waiting for message with id 1 from each sender.
        }
        this.waiting = new IntRanges[hosts.size()];
        for (var i = 0; i < hosts.size(); i++) {
            this.waiting[i] = new IntRangesImpl();
        }
    }

    @Override
    public void broadcast(final int msgId, final byte[] payload, final int senderId) {
        this.urbBroadcast.broadcast(msgId, payload, senderId);
    }

    @Override
    public void close() {
        this.urbBroadcast.close();
    }

    private void deliver(Message message) {

        System.out.println("FIFO start");

        var sender = message.getOriginalSenderId();
        this.waiting[sender - 1].addValue(message.getId());

        var start = this.waiting[sender - 1].getFirstRangeStart();

        // accept the messages only if the first message in the range is the one we are waiting for.
        if (start == this.currentSeq[sender - 1]) {
            System.out.println("FIFO deliver");
            // deliver all messages in the range.
            var end = this.waiting[sender - 1].getFirstRangeEnd();
            for (var i = start; i <= end; i++) {
                this.deliverCallback.accept(i, sender);
                this.currentSeq[sender - 1]++;
            }

            this.waiting[sender - 1].removeFirstRange();
        }

    }
}
