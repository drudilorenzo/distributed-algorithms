package cs451.message;

/**
 * Implementation of {@link Message} for ACK messages.
 */
public class AckMessageImpl implements Message {

    private static final short MESSAGE_SIZE = 6;

    private final int id;
    private final byte senderId;
    private final byte receiverId;
    private final byte[] msgToByte;

    /**
     * Constructor for ACK messages.
     *
     * @param id: the id of the message.
     * @param senderId: the id of the sender.
     * @param receiverId: the id of the receiver.
     */
    public AckMessageImpl(final int id, final int senderId, final int receiverId) {
        this.id = id;
        // Need to decrease the senderId by 1 to be able to represent it on 7 bits.
        this.senderId = (byte)((senderId - 1) & 0xFF);
        this.receiverId = (byte)((receiverId - 1) & 0xFF);
        // Byte representation of the message (2 bytes).
        // No usage of a function to avoid the overhead.
        this.msgToByte = new byte[AckMessageImpl.MESSAGE_SIZE];
        // 4 bytes for the id of the message.
        this.msgToByte[0] = (byte)((this.id >> 24) & 0xff);
        this.msgToByte[1] = (byte)((this.id >> 16) & 0xff);
        this.msgToByte[2] = (byte)((this.id >> 8) & 0xff);
        this.msgToByte[3] = (byte)(this.id & 0xff);
        // Use the 8th bit to represent the isAck boolean.
        msgToByte[4] = this.senderId;
        msgToByte[4] |= (byte)(1 << 7);
        msgToByte[5] = this.receiverId;
    }

    @Override
    public int getMessageId() {
        return this.id;
    }

    @Override
    public int getLength() {
        return AckMessageImpl.MESSAGE_SIZE;
    }

    @Override
    public boolean isAck() {
        return true;
    }

    @Override
    public int getSenderId() {
        return this.senderId + 1;
    }

    @Override
    public int getReceiverId() {
        return this.receiverId + 1;
    }

    @Override
    public byte[] getMessageInBytes() {
        return this.msgToByte;
    }

    @Override
    public byte[] getPayload() {
        return new byte[0];
    }

    @Override
    public Message toAck() {
        return null;
    }

    /*
     * To compare two messages, we compare their id, isAck, senderId and receiverId.
     */
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
        return (this.getMessageId() == ((Message) obj).getMessageId())
                && (this.isAck() == ((Message) obj).isAck())
                && (this.getSenderId() == ((Message) obj).getSenderId())
                && (this.getReceiverId() == ((Message) obj).getReceiverId());
    }

    @Override
    public int hashCode() {
        return Integer.toString(this.getMessageId()).hashCode()
                + Boolean.toString(this.isAck()).hashCode()
                + Integer.toString(this.getSenderId()).hashCode()
                + Integer.toString(this.getReceiverId()).hashCode();
    }

}
