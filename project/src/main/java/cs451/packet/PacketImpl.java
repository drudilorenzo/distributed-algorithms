package cs451.packet;

import java.nio.ByteBuffer;

/**
 * Implementation of {@Packet}.
 */
public class PacketImpl implements Packet {

    private static final int BYTE_SIZE = 17; // size in byte of the packet header

    // Using a byte array to be able to send every type of data
    private final int id;         // id of the packet
    private boolean isAck;        // true if the packet is an ack packet, false otherwise
    private int senderId;         // id of the sender
    private int receiverId;       // id of the receiver
    private byte[] byteRappr;     // byte representation of the packet
    private final byte[] payload; // payload of the packet (using a byte array to be able to send every type of data)

    /**
     * Constructor of {@PacketImpl}.
     *
     * @param payload:    the payload of the packet.
     * @param id:         the id of the packet.
     * @param isAck:      true if the packet is an ack packet, false otherwise.
     * @param senderId:   the id of the sender.
     * @param receiverId: the id of the receiver.
     */
    public PacketImpl(final byte[] payload, final int id, final boolean isAck, final int senderId, final int receiverId) {
        this.id = id;
        this.isAck = isAck;
        this.payload = payload;
        this.senderId = senderId;
        this.receiverId = receiverId;

        this.serializePacket();
    }

    private void serializePacket() {
        // The total size of the packet is the size of the header + the size of the payload
        final ByteBuffer bb = ByteBuffer.allocate(PacketImpl.BYTE_SIZE + this.payload.length);
        bb.putInt(this.payload.length);
        bb.put(this.payload);
        bb.put(this.isAck() ? (byte) 1 : (byte) 0);
        bb.putInt(this.id);
        bb.putInt(this.senderId);
        bb.putInt(this.receiverId);
        this.byteRappr = bb.array();
    }

    @Override
    public int getPacketId() {
        return this.id;
    }

    @Override
    public int getLength() {
        return Packet.MAX_PAYLOAD_SIZE;
    }

    @Override
    public boolean isAck() {
        return this.isAck;
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
    public byte[] getPacketInBytes() {
        return this.byteRappr;
    }

    @Override
    public byte[] getPayload() {
        return this.payload;
    }

    @Override
    public Packet toAck() {
        return new PacketImpl(this.byteRappr, this.id, true, this.receiverId, this.senderId);
    }

    /*
     * To compare two packets, we compare their id, isAck, senderId and receiverId.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        return (this.getPacketId() == ((PacketImpl) obj).getPacketId())
                && (this.isAck() == ((PacketImpl) obj).isAck())
                && (this.getSenderId() == ((PacketImpl) obj).getSenderId())
                && (this.getReceiverId() == ((PacketImpl) obj).getReceiverId());
    }

    @Override
    public int hashCode() {
        return Integer.toString(this.getPacketId()).hashCode()
                + Boolean.toString(this.isAck()).hashCode()
                + Integer.toString(this.getSenderId()).hashCode()
                + Integer.toString(this.getReceiverId()).hashCode();
    }

}
