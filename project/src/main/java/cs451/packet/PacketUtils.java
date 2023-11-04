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
        final int numMessages = (0xff & data[0]) << 24 | (0xff & data[1]) << 16  | (0xff & data[2]) << 8 | (0xff & data[3]);
        System.out.println("NumMessages: " + numMessages);
        final int receiverId = data[4] + 1;
        System.out.println("ReceiverId: " + receiverId);
        int curPos = Packet.HEADER_SIZE;
        final Packet packet = new PacketImpl();
        Message message;
        int messageLength;
        byte[] messageByte;
        for (int i = 0; i < numMessages; i++) {
            messageLength = (0xff & data[curPos]) << 24 | (0xff & data[curPos + 1]) << 16  | (0xff & data[curPos + 2]) << 8 | (0xff & data[curPos + 3]);
            System.out.println("MessageLength: " + messageLength);
            curPos += Packet.INT_SIZE;
            messageByte = new byte[messageLength];
            System.arraycopy(data, curPos, messageByte, 0, messageLength);
            message = MessageUtils.deserialize(messageByte, receiverId);
            System.out.println("MessageId: " + message.getId());
            curPos += messageLength;
            packet.addMessage(message);
        }
        return packet;
    }

}
