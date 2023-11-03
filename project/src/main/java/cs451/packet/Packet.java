package cs451.packet;

import cs451.message.Message;

import java.util.List;

/**
 * Interface for UDP Packet (composed of more {@link Message}).
 */
public interface Packet {

    /**
     * Maximum size of a packet in byte.
     * Every packet, in the current implementation, can contain at most 8 messages.
     * Every message, in the current implementation, it is at most 14 bytes long.
     * To this we need to add the 4 bytes for the number of messages and 4 bytes for the length of each message.
     */
    int MAX_PAYLOAD_SIZE = 148;

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
     * Get the (byte) length of the packet.
     *
     * @return The length of the packet.
     */
    int getLength();

    /**
     * Get the messages of the packet.
     *
     * @return The messages of the packet.
     */
    List<Message> getMessages();

    /**
     * Get the byte representation of the packet.
     * The serialization is done as follows:
     * - the first 4 bytes represent the number of messages in the packet
     * - the next 4 bytes represent the length of the first message
     * - the next n bytes represent the first message
     * ...
     *
     * @return The byte representation of the packet.
     */
    byte[] serialize();

}
