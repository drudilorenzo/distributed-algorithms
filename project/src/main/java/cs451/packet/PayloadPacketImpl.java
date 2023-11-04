package cs451.packet;

import java.util.List;
import java.util.ArrayList;
import cs451.message.Message;

/**
 * Implementation of a payload {@link Packet}.
 */
public class PayloadPacketImpl implements Packet {

    /**
     * Size in byte of the header of a payload packet.
     * - 4 bytes for the id of the packet.
     * - 1 byte for the receiverId + isAck.
     * - 4 bytes for the number of messages.
     */
    public static final int PAYLOAD_HEADER_SIZE = 9;
    /**
     * Maximum size of a packet in byte.
     * Header + payload.
     * Every packet, in the current implementation, can contain at most 8 messages (with maximum size 13 bytes).
     */
    public static final int MAX_PAYLOAD_SIZE = 145;
    private static final int MAX_NUM_MESSAGES = 8; // maximum number of messages in a packet

    private int length;
    private int senderId;
    private int receiverId;
    private final int id;
    private final List<Message> messages;

    public PayloadPacketImpl(final int id) {
        this.id = id;
        this.receiverId = -1;
        this.senderId = -1;
        this.messages = new ArrayList<>();
        this.length = PayloadPacketImpl.PAYLOAD_HEADER_SIZE;
    }

    /**
     * Constructor for {@link PayloadPacketImpl}.
     *
     * @param senderId: the id of the sender.
     */
    public PayloadPacketImpl(final int id, final int senderId) {
        this(id);
        this.senderId = senderId;
        // -1 means that the receiverId is not set yet (it will be set when the first message is added)
        this.receiverId = -1;
    }

    /**
     * Constructor for {@link PayloadPacketImpl}.
     *
     * @param senderId:   the id of the sender.
     * @param receiverId: the id of the receiver.
     */
    public PayloadPacketImpl(final int id, final int senderId, final int receiverId) {
        this(id, senderId);
        this.receiverId = receiverId;
    }

    @Override
    public void addMessage(final Message message) {
        if (this.receiverId == -1) {
            this.receiverId = message.getReceiverId();
        }
        if (this.senderId == -1) {
            this.senderId = message.getSenderId();
        }
        // 4 bytes for the length of the message.
        this.length += Packet.INT_SIZE + message.getLength();
        this.messages.add(message);
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public int getSenderId() {
        if (this.senderId == -1) {
            throw new UnsupportedOperationException("Sender id not set.");
        }
        return this.senderId;
    }

    @Override
    public int getReceiverId() {
        // The receiver id is not set until the first message is added.
        // It is not possible to get the receiver id of an empty packet.
        if (this.receiverId == -1) {
            throw new UnsupportedOperationException("Receiver id not set.");
        }
        return this.receiverId;
    }

    @Override
    public boolean isAck() {
        return false;
    }

    public int getLength() {
        return this.length;
    }

    @Override
    public boolean canContainMessage(final int messageLength) {
        return (this.length + Packet.INT_SIZE + messageLength <= PayloadPacketImpl.MAX_PAYLOAD_SIZE)
                && (this.messages.size() + 1 <= PayloadPacketImpl.MAX_NUM_MESSAGES);
    }

    @Override
    public List<Message> getMessages() {
        return new ArrayList<>(this.messages); // shallow copy
    }

    @Override
    public byte[] serialize() {
        final byte[] buffer = new byte[this.length];
        buffer[0] = (byte)((this.id >> 24) & 0xFF);
        buffer[1] = (byte)((this.id >> 16) & 0xFF);
        buffer[2] = (byte)((this.id >> 8) & 0xFF);
        buffer[3] = (byte)(this.id & 0xFF);
        buffer[4] = (byte)((this.receiverId - 1) & 0xFF);
        buffer[4] |= (byte)(0);
        final var numMessages = this.messages.size();
        buffer[5] = (byte)((numMessages >> 24) & 0xFF);
        buffer[6] = (byte)((numMessages >> 16) & 0xFF);
        buffer[7] = (byte)((numMessages >> 8) & 0xFF);
        buffer[8] = (byte)(numMessages & 0xFF);
        var currentLength = PayloadPacketImpl.PAYLOAD_HEADER_SIZE;
        for (var m : this.messages) {
            final var messageLength = m.getLength();
            buffer[currentLength] = (byte)((messageLength >> 24) & 0xff);
            buffer[currentLength + 1] = (byte)((messageLength >> 16) & 0xff);
            buffer[currentLength + 2] = (byte)((messageLength >> 8) & 0xff);
            buffer[currentLength + 3] = (byte)(messageLength & 0xff);
            currentLength += Packet.INT_SIZE;
            System.arraycopy(m.serialize(), 0, buffer, currentLength, messageLength);
            currentLength += messageLength;
        }
        return buffer;
    }

    @Override
    public Packet toAck() {
        if (this.messages.isEmpty()) {
            throw new UnsupportedOperationException("Cannot convert an empty packet to an ack packet.");
        }
        if (this.senderId == -1) {
            return new AckPacketImpl(this.id, this.receiverId, senderId);
        } else {
            return new AckPacketImpl(this.id, this.receiverId, this.senderId);
        }
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
