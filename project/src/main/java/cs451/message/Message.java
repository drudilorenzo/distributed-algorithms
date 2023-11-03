package cs451.message;

import java.io.Serializable;

/**
 * Interface for UDP Message.
 */
public interface Message extends Serializable {

    /**
     * Get the message id.
     *
     * @return the id of the message.
     */
    int getMessageId();

    /**
     * Get the length of the payload.
     *
     * @return The length of the payload.
     */
    int getLength();

    /**
     * Check if a packet is an ack message.
     *
     * @return True if it is a packet, false otherwise.
     */
    boolean isAck();

    /**
     * Get the id of the sender.
     *
     * @return The byte of the sender (since the maximum number of processes is 128 use a byte).
     */
    int getSenderId();

    /**
     * Get the id of the receiver.
     *
     * @return The id of the receiver (since the maximum number of processes is 128 use a byte).
     */
    int getReceiverId();

    /**
     * Get the message data as a byte array.
     *
     * @return A byte array containing the message data.
     */
    byte[] getMessageInBytes();

    /**
     * Get the payload of the message.
     *
     * @return A byte array containing the payload.
     */
    byte[] getPayload();

    /**
     * Get the ack message of the current one.
     *
     * @return The ack message.
     */
    Message toAck();

}
