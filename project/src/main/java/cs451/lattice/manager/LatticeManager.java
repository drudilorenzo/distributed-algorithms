package cs451.lattice.manager;

import java.util.Set;

/**
 * Interface for multi-shot lattice agreement manager.
 */
public interface LatticeManager {

    /**
     * Propose a new shot.
     *
     * @param proposal: the proposal to propose.
     */
    void propose(final Set<Integer> proposal);

    /**
     * Decide a shot.
     */
    void decide(final int shotNumber, final Set<Integer> decision);

    /**
     * Broadcast a message.
     *
     * @param payload:   the payload of the message.
     * @param messageId: the id of the message.
     */
    void broadcast(byte[] payload, int messageId);

    /**
     * Send a message to a single destination.
     *
     * @param payload:     the payload of the message.
     * @param messageId:   the id of the message.
     * @param destination: the destination to send the message to.
     */
    void singleSend(byte[] payload, int messageId, int destination);

    /**
     * Close the manager.
     */
    void close();


}
