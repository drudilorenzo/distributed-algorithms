package cs451.packet;

import java.io.Serializable;

/**
 * Interface for UDP Packets.
 */
public interface Packet extends Serializable {


    public static final int MAX_PAYLOAD_SIZE = 65507;

    /**
     * Get the packet id.
     *
     * @return the id of the packet.
     */
    int getPacketId();

    /**
     * Get the length of the payload.
     *
     * @return The length of the payload.
     */
    int getLength();

    /**
     * Check if a packet is an ack packet.
     *
     * @return True if it is an packet, false otherwise.
     */
    boolean isAck();

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
     * Get the packet data as a byte array.
     *
     * @return A byte array containing the packet data.
     */
    byte[] getPacketInBytes();

    /**
     * Get the payload of the packet.
     *
     * @return A byte array containing the payload.
     */
    byte[] getPayload();

    /**
     * Get the ack packet of the current one.
     *
     * @return The ack packet.
     */
    Packet toAck();

}
