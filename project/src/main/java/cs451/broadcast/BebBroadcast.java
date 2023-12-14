package cs451.broadcast;

import cs451.Host;
import cs451.message.Message;
import cs451.links.PerfectLink;
import cs451.message.PayloadMessageImpl;
import cs451.lattice.messageTypes.LatticeMessage;
import cs451.lattice.messageTypes.LatticeMessageSerializationUtils;

import java.util.List;
import java.util.function.Consumer;

/**
 * Interface for BEB broadcast.
 *
 * It is characterized by the following properties:
 *  - Validity: if pi and pj are correct, then every message broadcast by
 *              pi is eventually delivered by pj.
 *  - No duplication: no message is delivered more than once.
 *  - No creation: no message is delivered unless it was broadcast.
 */
public class BebBroadcast implements Broadcast {

    private final int myId;
    private final int numHosts;
    private final PerfectLink perfectLink;
    private final Consumer<LatticeMessage> deliverCallback;

    /**
     * Constructor of {@link BebBroadcast}.
     *
     * @param hosts: the list of hosts.
     * @param myId:  the id of the current host.
     * @param port:  the port to use for the perfect link.
     */
    public BebBroadcast(final List<Host> hosts, final int myId, final int port, final Consumer<LatticeMessage> deliverCallback) {
        this.myId = myId;
        this.numHosts = hosts.size();
        this.deliverCallback = deliverCallback;
        this.perfectLink = new PerfectLink(myId, port, hosts, this::deliver);
    }

    @Override
    public void broadcast(final byte[] payload, final int messageId) {
        for (int i = 1; i <= this.numHosts; i++) {
            if (i != this.myId) {
                this.perfectLink.send(new PayloadMessageImpl(payload, messageId, this.myId, i));
            }
        }
    }

    private void deliver(final byte[] payload) {
        LatticeMessage latticeMessage = LatticeMessageSerializationUtils.deserializeLatticeMessage(payload);
        this.deliverCallback.accept(latticeMessage);
    }

    @Override
    public void singleSend(final byte[] payload, final int messageId, final int destination) {
        this.perfectLink.send(new PayloadMessageImpl(payload, messageId, this.myId, destination));
    }

    @Override
    public void close() {
        this.perfectLink.close();
    }

}
