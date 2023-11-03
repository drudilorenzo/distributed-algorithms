package cs451.message;

/**
 * Implementation of {@link PayloadMessageImpl} for the payload message.
 */
public class PayloadMessageImpl implements Message {

    public static final int HEADER_SIZE = 10; // size in byte of the message header without the payload

    private final int id;               // id of the message
    private final byte[] msgToByte;     // byte representation of the packet
    private final byte senderId;        // id of the sender (max value: 128)
    private final byte receiverId;      // id of the receiver (max value: 128)
    private final byte[] payload;       // payload of the packet (using a byte array to be able to send every type of data)

    /**
     * Constructor of {@link PayloadMessageImpl}.
     *
     * @param payload:    the payload of the message.
     * @param id:         the id of the message.
     * @param senderId:   the id of the sender.
     * @param receiverId: the id of the receiver.
     */
    public PayloadMessageImpl(final byte[] payload, final int id, final int senderId, final int receiverId) {
        this.id = id;
        this.payload = payload;
        this.senderId =  (byte)((senderId - 1) & 0xFF);
        this.receiverId = (byte)((receiverId - 1) & 0xFF);
        this.msgToByte = new byte[HEADER_SIZE + payload.length];
        // 4 bytes for the id of the message
        this.msgToByte[0] = (byte)((this.id >> 24) & 0xff);
        this.msgToByte[1] = (byte)((this.id >> 16) & 0xff);
        this.msgToByte[2] = (byte)((this.id >> 8) & 0xff);
        this.msgToByte[3] = (byte)(this.id & 0xff);
        // Need to decrease the senderId by 1 to be able to represent it on 7 bits.
        // This permits to use the 8th bit to represent the isAck boolean.
        // 1 byte for the senderId and the isAck boolean
        this.msgToByte[4] = (byte)((senderId - 1) & 0xFF);
        this.msgToByte[4] |= (byte)(0);
        // 1 byte for the receiverId
        this.msgToByte[5] = (byte)((receiverId - 1) & 0xFF);
        // 4 bytes for the length of the payload
        this.msgToByte[6] = (byte)((payload.length >> 24) & 0xff);
        this.msgToByte[7] = (byte)((payload.length >> 16) & 0xff);
        this.msgToByte[8] = (byte)((payload.length >> 8) & 0xff);
        this.msgToByte[9] = (byte)(payload.length & 0xff);
        // n bytes for the payload
        System.arraycopy(payload, 0, this.msgToByte, PayloadMessageImpl.HEADER_SIZE, payload.length);
    }

    @Override
    public int getMessageId() {
        return this.id;
    }

    @Override
    public int getLength() {
        return PayloadMessageImpl.HEADER_SIZE + this.payload.length;
    }

    @Override
    public boolean isAck() {
        return false;
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
        return this.payload;
    }

    @Override
    public Message toAck() {
        return new AckMessageImpl(this.id, this.getReceiverId(), this.getSenderId());
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
