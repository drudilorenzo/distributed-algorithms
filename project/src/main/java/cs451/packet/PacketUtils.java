package cs451.packet;

import cs451.message.Message;
import cs451.message.MessageUtils;

/**
 * Utility class for {@link Packet}.
 */
public class PacketUtils {

    /**
     * Deserialize a byte array into a {@link Packet}.
     *
     * @param data: the byte array to deserialize.
     * @return The deserialized packet.
     */
    public static Packet deserialize(final byte[] data) {
        final int id = (0xFF & data[0]) << 24 | (0xFF & data[1]) << 16  | (0xFF & data[2]) << 8 | (0xFF & data[3]);
        final int receiverId = (data[4] & 0x7F) + 1;
        final boolean isAck = (data[4] & 0x80) != 0;
        final int senderId = data[5] + 1;
        if (isAck) {
            return new AckPacketImpl(id, senderId, receiverId);
        }
        final int numMessages = (0xFF & data[6]) << 24 | (0xFF & data[7]) << 16  | (0xFF & data[8]) << 8 | (0xFF & data[9]);
        int curPos = PayloadPacketImpl.PAYLOAD_HEADER_SIZE;
        final Packet packet = new PayloadPacketImpl(id);
        Message message;
        int messageLength;
        byte[] messageByte;
        for (int i = 0; i < numMessages; i++) {
            messageLength = (0xFF & data[curPos]) << 24 | (0xFF & data[curPos + 1]) << 16  | (0xFF & data[curPos + 2]) << 8 | (0xFF & data[curPos + 3]);
            curPos += Packet.INT_SIZE;
            messageByte = new byte[messageLength];
            System.arraycopy(data, curPos, messageByte, 0, messageLength);
            message = MessageUtils.deserialize(messageByte, senderId, receiverId);
            curPos += messageLength;
            packet.addMessage(message);
        }
        return packet;
    }

}
