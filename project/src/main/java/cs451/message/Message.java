package cs451.message;

/**
 * Interface for UDP Message.
 */
public interface Message {

    /**
     * Get the message id.
     *
     * @return the id of the message.
     */
    int getId();

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
     * Get the length of the payload.
     *
     * @return The length of the payload.
     */
    int getLength();

    /**
     * Get the payload of the message.
     *
     * @return A byte array containing the payload.
     */
    byte[] getPayload();

    /**
     * Get the byte representation of the packet.
     * It is done as follows:
     * - the first 4 bytes represent the id of the message.
     * - the 5th byte represent the senderId and the isAck boolean.
     * - the next 4 bytes represent the length of the payload.
     * - the next n bytes represent the payload.
     * The receiverId is not considered in the serialization of the message
     * since it is present in the header of the packet.
     * 
     * @return The byte representation of the packet.
     */
    byte[] serialize();

}
