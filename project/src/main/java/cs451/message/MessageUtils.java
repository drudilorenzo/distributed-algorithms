package cs451.message;

/**
 * Utility class for {@link Message}.
 */
public class MessageUtils {

   public static Message deserialize(final byte[] data) {
       // The first 4 bytes are the id of the message
       final int messageId = (0xff & data[0]) << 24 | (0xff & data[1]) << 16  | (0xff & data[2]) << 8 | (0xff & data[3]);
       // The 5th byte is the senderId and the isAck boolean
       final int senderId = (data[4] & 0x7F) + 1;
       final boolean isAck = (data[4] & 0x80) != 0;
       // The 6th byte is the receiverId
       final int receiverId = data[5] + 1;
       if (isAck) {
           return new AckMessageImpl(messageId, senderId, receiverId);
       } else {
           // The last 4 bytes are the length of the payload
           final int payloadLength = (0xff & data[6]) << 24 | (0xff & data[7]) << 16 | (0xff & data[8]) << 8 | (0xff & data[9]);
           final var payload = new byte[payloadLength];
           System.arraycopy(data, PayloadMessageImpl.HEADER_SIZE, payload, 0, payloadLength);
           return new PayloadMessageImpl(payload, messageId, senderId, receiverId);
       }
   }

}
