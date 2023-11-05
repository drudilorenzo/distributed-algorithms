package cs451.message;

/**
 * Implementation of {@link PayloadMessageImpl} for the payload message.
 */
public class PayloadMessageImpl implements Message {

    public static final int HEADER_SIZE = 8; // size in byte of the message header without the payload

    private final int id;               // id of the message
    private final byte senderId;        // id of the sender (max value: 128)
    private final byte[] payload;       // payload of the packet (using a byte array to be able to send every type of data)
    private final byte receiverId;      // id of the receiver (max value: 128)

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
    }

    @Override
    public int getId() {
        return this.id;
    }

    public int getSenderId() {
        return this.senderId + 1;
    }

    @Override
    public int getReceiverId() {
        return this.receiverId + 1;
    }

    @Override
    public int getLength() {
        return PayloadMessageImpl.HEADER_SIZE + this.payload.length;
    }

    @Override
    public byte[] getPayload() {
        return this.payload;
    }

    @Override
    public byte[] serialize() {
        var msgToByte = new byte[HEADER_SIZE + payload.length];
        // 4 bytes for the id of the message
        msgToByte[0] = (byte)((this.id >> 24) & 0xFF);
        msgToByte[1] = (byte)((this.id >> 16) & 0xFF);
        msgToByte[2] = (byte)((this.id >> 8) & 0xFF);
        msgToByte[3] = (byte)(this.id & 0xff);
        // 4 bytes for the length of the payload
        msgToByte[4] = (byte)((this.payload.length >> 24) & 0xFF);
        msgToByte[5] = (byte)((this.payload.length >> 16) & 0xFF);
        msgToByte[6] = (byte)((this.payload.length >> 8) & 0xFF);
        msgToByte[7] = (byte)(this.payload.length & 0xFF);
        // n bytes for the payload
        System.arraycopy(this.payload, 0, msgToByte, PayloadMessageImpl.HEADER_SIZE, this.payload.length);
        return msgToByte;
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
        return (this.getId() == ((Message) obj).getId())
                && (this.getSenderId() == ((Message) obj).getSenderId())
                && (this.getReceiverId() == ((Message) obj).getReceiverId());
    }

    @Override
    public int hashCode() {
        return Integer.toString(this.getId()).hashCode()
                + Integer.toString(this.getSenderId()).hashCode()
                + Integer.toString(this.getReceiverId()).hashCode();
    }

}
