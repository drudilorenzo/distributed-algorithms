package cs451.packet;

import java.util.List;
import java.util.ArrayList;
import cs451.message.Message;

/**
 * Implementation of {@link Packet}.
 */
public class PacketImpl implements Packet {

    private static final int MAX_NUM_MESSAGES = 8; // maximum number of messages in a packet

    private int length;
    private final List<Message> messages;

    /**
     * Constructor for {@link PacketImpl}.
     */
    public PacketImpl() {
        this.length = Packet.HEADER_SIZE;
        this.messages = new ArrayList<>();
    }

    @Override
    public void addMessage(final Message message) {
        // 4 bytes for the length of the message.
        this.length += Packet.INT_SIZE + message.getLength();
        this.messages.add(message);
    }

    @Override
    public boolean canContainMessage(final int messageLength) {
        return (this.length + Packet.INT_SIZE + messageLength <= Packet.MAX_PAYLOAD_SIZE)
                && (this.messages.size() + 1 <= PacketImpl.MAX_NUM_MESSAGES);
    }

    @Override
    public int getLength() {
        return this.length;
    }

    @Override
    public List<Message> getMessages() {
        return new ArrayList<>(this.messages); // shallow copy
    }

    @Override
    public byte[] serialize() {
        final byte[] buffer = new byte[this.length];
        final var numMessages = this.messages.size();
        buffer[0] = (byte)((numMessages >> 24) & 0xff);
        buffer[1] = (byte)((numMessages >> 16) & 0xff);
        buffer[2] = (byte)((numMessages >> 8) & 0xff);
        buffer[3] = (byte)(numMessages & 0xff);
        final var receiverId = this.messages.get(0).getReceiverId();
        buffer[4] = (byte)((receiverId - 1) & 0xFF);
        var currentLength = Packet.HEADER_SIZE;
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
        return this.messages.equals(((PacketImpl) obj).messages);
    }

    @Override
    public int hashCode() {
        return this.messages.hashCode();
    }

}
