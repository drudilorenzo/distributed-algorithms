package cs451.packet;

import cs451.message.Message;

import java.util.List;

/**
 * Interface for UDP Packet (composed of more {@link Message}).
 */
public interface Packet {

    int INT_SIZE = 4; // size in byte of an int

    /**
     * Get the id of the packet.
     *
     * @return The id of the packet.
     */
    int getId();

    /**
     * Get the id of the sender.
     *
     * @return The id of the sender.
     */
    int getSenderId();

    /**
     * Get the id of the receiver.
     *
     * @return The id of the receiver.
     */
    int getReceiverId();

    /**
     * Get the (byte) length of the packet.
     *
     * @return The length of the packet.
     */
    int getLength();

    /**
     * Get the original sender id of the packet.
     *
     * @return The original sender id of the packet.
     */
    int getOriginalSenderId();

    /**
     * Get the messages of the packet.
     *
     * @return The messages of the packet.
     */
    List<Message> getMessages();

    /**
     * Return true if the packet is an ack packet, false otherwise.
     *
     * @return True if the packet is an ack packet, false otherwise.
     */
    boolean isAck();

    /**
     * Add a message to the packet.
     *
     * @param message: the message to add.
     */
    void addMessage(Message message);

    /**
     * Check if the packet is full.
     *
     * @return True if the packet is full, false otherwise.
     */
    boolean canContainMessage(int messageLength) ;

    /**
     * Check if the packet can be transmitted.
     *
     * @return True if the packet can be transmitted, false otherwise.
     */
    boolean canTransmit();

    /**
     * Set the packet as transmitted.
     *
     * @param transmit: true if the packet can be transmitted, false otherwise.
     */
    void setTransmit(boolean transmit);

    /**
     * Convert the packet to an ack packet.
     *
     * @return The ack packet.
     */
    Packet toAck();

    /**
     * Get the byte representation of the packet.
     *
     * @return The byte representation of the packet.
     */
    byte[] serialize();

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

}
