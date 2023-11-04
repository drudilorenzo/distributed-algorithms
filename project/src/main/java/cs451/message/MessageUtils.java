package cs451.message;

/**
 * Utility class for {@link Message}.
 */
public class MessageUtils {

    public static Message deserialize(final byte[] data, final int receiverId) {
        // The first 4 bytes are the id of the message
        final int messageId = (0xFF & data[0]) << 24 | (0xFF & data[1]) << 16  | (0xFF & data[2]) << 8 | (0xFF & data[3]);
        // The 5th byte is the senderId
        final int senderId = data[4] + 1;
        // The last 4 bytes are the length of the payload
        final int payloadLength = (0xFF & data[5]) << 24 | (0xFF & data[6]) << 16 | (0xFF & data[7]) << 8 | (0xFF & data[7]);
        final var payload = new byte[payloadLength];
        System.arraycopy(data, PayloadMessageImpl.HEADER_SIZE, payload, 0, payloadLength);
        return new PayloadMessageImpl(payload, messageId, senderId, receiverId);
   }

}
