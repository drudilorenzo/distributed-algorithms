package cs451.broadcast;

import cs451.message.Message;

import java.util.Set;

/**
 * Interface for broadcast.
 */
public interface Broadcast {

    /**
    * Broadcast a message.
    *
    * @param payload: the payload of the message.
    * @param messageId: the id of the message.
    */
    void broadcast(byte[] payload, int messageId);

    /**
     * Send a message to a single destination.
     * To avoid the usage of multiple perfect links references,
     * beb exposes also a single send method.
     *
     * @param payload:     the payload of the message.
     * @param messageId:   the id of the message.
     * @param destination: the destination to send the message to.
     */
    void singleSend(byte[] payload, int messageId,int destination);

    /**
     * Close the broadcast.
     */
    void close();

}
