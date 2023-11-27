package cs451.message;

/**
 * Utility class for {@link Message}.
 */
public class MessageUtils {

    public static Message deserialize(final byte[] data, final int senderId, final int receiverId) {
        // The first 4 bytes are the id of the message
        final int messageId = (0xFF & data[0]) << 24 | (0xFF & data[1]) << 16  | (0xFF & data[2]) << 8 | (0xFF & data[3]);
        // The last 4 bytes are the length of the payload
        final int payloadLength = (0xFF & data[4]) << 24 | (0xFF & data[5]) << 16 | (0xFF & data[6]) << 8 | (0xFF & data[7]);
        final var payload = new byte[payloadLength];
        System.arraycopy(data, PayloadMessageImpl.HEADER_SIZE, payload, 0, payloadLength);
        final var originalSenderId = data[8] + 1;
        return new PayloadMessageImpl(payload, messageId, senderId, receiverId, originalSenderId);
   }

}
