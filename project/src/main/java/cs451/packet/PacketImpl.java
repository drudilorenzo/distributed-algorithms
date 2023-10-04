package cs451.packet;

public class PacketImpl implements Packet {

    private final byte[] datagram;
    private final int id;
    private final int numMessages;
    private final boolean ack;
    private final int timestamp;

    public PacketImpl(final byte[] datagram, final int id,
            final int numMessages, final boolean ack,final int timestamp) {
        this.id = id;
        this.ack = ack;
        this.datagram = datagram;
        this.timestamp = timestamp;
        this.numMessages = numMessages;
    }

    @Override
    public int getPacketId() {
        return this.id;
    }

    public byte[] getData() {
        return this.datagram;
    }

    @Override
    public boolean isAck() {
        return this.ack;
    }

    @Override
    public int getTimestamp() {
        return this.timestamp;
    }
}
