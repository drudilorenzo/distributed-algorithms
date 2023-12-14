package cs451.lattice.messageTypes;

import java.util.Set;

/**
 * Implementation of {@link LatticeMessage} for proposal messages.
 */
public class ProposalImpl implements LatticeMessage {

    private final int shotNumber;
    private final int senderId;
    private final int proposalNumber;
    private final Set<Integer> proposalNumbers;

    /**
     * Constructor of {@link ProposalImpl}.
     *
     * @param shotNumber:      the shot number of the message (lattice instance).
     * @param proposalNumber:  the proposal number of the message.
     * @param senderId:        the sender id of the message.
     * @param proposalNumbers: the new proposal set.
     */
    public ProposalImpl(final int shotNumber, final int proposalNumber,
                        final int senderId, final Set<Integer> proposalNumbers) {
        this.shotNumber = shotNumber;
        this.proposalNumber = proposalNumber;
        this.senderId = senderId;
        this.proposalNumbers = proposalNumbers;
    }

    @Override
    public int getShotNumber() {
        return this.shotNumber;
    }

    /**
     * Get the proposal number of the message.
     *
     * @return the proposal number of the message.
     */
    public int getProposalNumber() {
        return this.proposalNumber;
    }

    /**
     * Get the sender id of the message.
     *
     * @return the sender id of the message.
     */
    public int getSenderId() {
        return this.senderId;
    }

    /**
     * Get the new proposal set.
     *
     * @return the new proposal set.
     */
    public Set<Integer> getProposalNumbers() {
        return this.proposalNumbers;
    }

    @Override
    public String toString() {
        return "ProposalImpl{" +
            "shotNumber=" + shotNumber +
            ", proposalNumber=" + proposalNumber +
            ", senderId=" + senderId +
            ", proposalNumbers=" + proposalNumbers.toString() +
            "}";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProposalImpl)) {
            return false;
        }
        final ProposalImpl that = (ProposalImpl) o;
        return getShotNumber() == that.getShotNumber() &&
            getProposalNumber() == that.getProposalNumber() &&
            getSenderId() == that.getSenderId() &&
            getProposalNumbers().equals(that.getProposalNumbers());
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getShotNumber())
                + Integer.hashCode(getProposalNumber()) * 31
                + Integer.hashCode(getSenderId()) * 31 * 31
                + getProposalNumbers().hashCode() * 31 * 31 * 31;
    }

}
