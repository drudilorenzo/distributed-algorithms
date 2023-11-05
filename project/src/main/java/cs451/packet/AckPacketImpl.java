package cs451.packet;

import cs451.message.Message;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of an ack {@link Packet}.
 */
public class AckPacketImpl implements Packet {

    // - 4 bytes for the id of the packet.
    // - 1 byte for the senderId + isAck.
    // - 1 byte for the receiverId.
    private static final int ACK_PACKET_SIZE = 6;

    private final int id;
    private final int senderId;
    private final int receiverId;
    private final AtomicBoolean canTransmit;

    public AckPacketImpl(final int id, final int senderId, final int receiverId) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.canTransmit = new AtomicBoolean(false);
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public int getSenderId() {
        return this.senderId;
    }

    @Override
    public int getReceiverId() {
        return this.receiverId;
    }

    @Override
    public boolean isAck() {
        return true;
    }

    @Override
    public int getLength() {
        return AckPacketImpl.ACK_PACKET_SIZE;
    }

    @Override
    public void addMessage(Message message) {
        throw new UnsupportedOperationException("No addMessage for ACK packets.");
    }

    @Override
    public boolean canContainMessage(int messageLength) {
        throw new UnsupportedOperationException("No canContainMessage for ACK packets.");
    }

    @Override
    public List<Message> getMessages() {
        throw new UnsupportedOperationException("No getMessages for ACK packets.");
    }

    @Override
    public boolean canTransmit() {
        return this.canTransmit.get();
    }

    @Override
    public void setTransmit(boolean transmit) {
        this.canTransmit.set(transmit);
    }

    @Override
    public Packet toAck() {
        throw new UnsupportedOperationException("No toAck for ACK packets.");
    }

    @Override
    public byte[] serialize() {
        final byte[] buffer = new byte[AckPacketImpl.ACK_PACKET_SIZE];
        // 4 bytes for the id of the packet.
        buffer[0] = (byte)((this.id >> 24) & 0xFF);
        buffer[1] = (byte)((this.id >> 16) & 0xFF);
        buffer[2] = (byte)((this.id >> 8) & 0xFF);
        buffer[3] = (byte)(this.id & 0xFF);
        // 5th byte for the receiverId + isAck.
        buffer[4] = (byte)((receiverId - 1) & 0xFF);
        buffer[4] |= (byte)(1 << 7);
        // 6th byte for the senderId.
        buffer[5] = (byte)((senderId - 1) & 0xFF);
        return buffer;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        return this.getId() == ((Packet)obj).getId()
                && this.getSenderId() == ((Packet)obj).getSenderId()
                && this.getReceiverId() == ((Packet)obj).getReceiverId()
                && this.isAck() == ((Packet)obj).isAck();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.id)
                + Integer.hashCode(this.senderId)
                + Integer.hashCode(this.receiverId)
                + Boolean.hashCode(this.isAck());
    }

}
