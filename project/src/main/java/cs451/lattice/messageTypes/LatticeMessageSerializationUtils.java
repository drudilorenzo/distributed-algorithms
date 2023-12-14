package cs451.lattice.messageTypes;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for serializing and deserializing {@link LatticeMessage}s.
 */
public class LatticeMessageSerializationUtils {

    private static final int INT_SIZE = 4;

    private static byte[] serializeAck(final AckMessageImpl ackMessage) {
        // need 9 bytes for the message type and the two ints
        final int size = 1 + 2 * INT_SIZE;
        final ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.put((byte) LatticeMessageEnum.ACK.getValue());
        buffer.putInt(ackMessage.getShotNumber());
        buffer.putInt(ackMessage.getProposalNumber());
        return buffer.array();
    }

    private static AckMessageImpl deserializeAck(final byte[] bytes, final ByteBuffer buffer) {
        final int shotNumber = buffer.getInt();
        final int proposalNumber = buffer.getInt();
        return new AckMessageImpl(shotNumber, proposalNumber);
    }

    private static byte[] serializeNack(final NackMessageImpl nackMessage) {
        // need to allocate 9 bytes for the message type and the two ints + the size of the set
        final int size = 1 + 3 * INT_SIZE + nackMessage.getProposalNumbers().size() * INT_SIZE;
        final ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.put((byte) LatticeMessageEnum.NACK.getValue());
        buffer.putInt(nackMessage.getShotNumber());
        buffer.putInt(nackMessage.getProposalNumber());
        buffer.putInt(nackMessage.getProposalNumbers().size());
        for (final int proposalNumber : nackMessage.getProposalNumbers()) {
            buffer.putInt(proposalNumber);
        }
        return buffer.array();
    }

    private static NackMessageImpl deserializeNack(final byte[] bytes, final ByteBuffer buffer) {
        final int shotNumber = buffer.getInt();
        final int proposalNumber = buffer.getInt();
        final int size = buffer.getInt();
        final Set<Integer> proposalNumbers = new HashSet<>();
        for (int i = 0; i < size; i++) {
            proposalNumbers.add(buffer.getInt());
        }
        return new NackMessageImpl(shotNumber, proposalNumber, proposalNumbers);
    }

    private static byte[] serializeProposal(final ProposalImpl proposal) {
        final int size = 1 + 4 * INT_SIZE + proposal.getProposalNumbers().size() * INT_SIZE;
        final ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.put((byte) LatticeMessageEnum.PROPOSAL.getValue());
        buffer.putInt(proposal.getShotNumber());
        buffer.putInt(proposal.getProposalNumber());
        buffer.putInt(proposal.getSenderId());
        buffer.putInt(proposal.getProposalNumbers().size());
        for (final int proposalNumber : proposal.getProposalNumbers()) {
            buffer.putInt(proposalNumber);
        }
        return buffer.array();
    }

    private static ProposalImpl deserializeProposal(final byte[] bytes, final ByteBuffer buffer) {
        final int shotNumber = buffer.getInt();
        final int proposalNumber = buffer.getInt();
        final int senderId = buffer.getInt();
        final int size = buffer.getInt();
        final Set<Integer> proposalNumbers = new HashSet<>();
        for (int i = 0; i < size; i++) {
            proposalNumbers.add(buffer.getInt());
        }
        return new ProposalImpl(shotNumber, proposalNumber, senderId, proposalNumbers);
    }

    private static byte[] serializeDecision(final DecisionImpl decision) {
        final int size = 1 + 2 * INT_SIZE + decision.getDecision().size() * INT_SIZE;
        final ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.put((byte) LatticeMessageEnum.DECISION.getValue());
        buffer.putInt(decision.getShotNumber());
        buffer.putInt(decision.getDecision().size());
        for (final int decisionNumber : decision.getDecision()) {
            buffer.putInt(decisionNumber);
        }
        return buffer.array();
    }

    private static DecisionImpl deserializeDecision(final byte[] bytes, final ByteBuffer buffer) {
        final int shotNumber = buffer.getInt();
        final int size = buffer.getInt();
        final Set<Integer> decision = new HashSet<>();
        for (int i = 0; i < size; i++) {
            decision.add(buffer.getInt());
        }
        return new DecisionImpl(shotNumber, decision);
    }

    /**
     * Serialize a {@link LatticeMessage} into a byte array.
     *
     * @param latticeMessage: the {@link LatticeMessage} to serialize.
     * @return the serialized {@link LatticeMessage}.
     */
    public static byte[] serializeLatticeMessage(final LatticeMessage latticeMessage) {
        if (latticeMessage instanceof AckMessageImpl) {
            return serializeAck((AckMessageImpl) latticeMessage);
        } else if (latticeMessage instanceof NackMessageImpl) {
            return serializeNack((NackMessageImpl) latticeMessage);
        } else if (latticeMessage instanceof ProposalImpl) {
            return serializeProposal((ProposalImpl) latticeMessage);
        } else if (latticeMessage instanceof DecisionImpl) {
            return serializeDecision((DecisionImpl) latticeMessage);
        } else {
            throw new IllegalArgumentException("Unknown lattice message type.");
        }
    }

    /**
     * Deserialize a {@link LatticeMessage} from a byte array.
     *
     * @param bytes: the byte array to deserialize.
     * @return the deserialized {@link LatticeMessage}.
     */
    public static LatticeMessage deserializeLatticeMessage(final byte[] bytes) {
        final ByteBuffer buffer = ByteBuffer.wrap(bytes);
        final byte messageType = buffer.get();
        if (messageType == LatticeMessageEnum.ACK.getValue()) {
            return deserializeAck(bytes, buffer);
        } else if (messageType == LatticeMessageEnum.NACK.getValue()) {
            return deserializeNack(bytes, buffer);
        } else if (messageType == LatticeMessageEnum.PROPOSAL.getValue()) {
            return deserializeProposal(bytes, buffer);
        } else if (messageType == LatticeMessageEnum.DECISION.getValue()) {
            return deserializeDecision(bytes, buffer);
        } else {
            throw new IllegalArgumentException("Unknown lattice message type.");
        }
    }

}
