package cs451.packet;

import java.util.List;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import cs451.message.Message;

/**
 * Implementation of {@link Packet}.
 */
public class PacketImpl implements Packet {

    private static final int INT_SIZE = 4;         // size in byte of an int
    private static final int MAX_NUM_MESSAGES = 8; // maximum number of messages in a packet

    private int length;
    private final List<Message> messages;

    public PacketImpl() {
        this.length = INT_SIZE; // 4 bytes for the number of messages
        this.messages = new ArrayList<>();
    }

    @Override
    public void addMessage(final Message message) {
        // Need to consider the size of the header (4 bytes for the length of the message)
        this.length += PacketImpl.INT_SIZE + message.getLength();
        this.messages.add(message);
    }

    @Override
    public boolean canContainMessage(final int messageLength) {
        return (this.length + PacketImpl.INT_SIZE + messageLength <= Packet.MAX_PAYLOAD_SIZE)
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
        final ByteBuffer bb = ByteBuffer.allocate(this.length);
        bb.putInt(this.messages.size());
        for (final var message : this.messages) {
            bb.putInt(message.getLength());
            bb.put(message.getMessageInBytes());
        }
        return bb.array();
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
