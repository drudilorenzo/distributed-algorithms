package cs451.lattice.messageTypes;

import java.util.Set;

/**
 * Implementation of {@link LatticeMessage} for nack messages.
 */
public class NackMessageImpl implements LatticeMessage {

    private final int shotNumber;
    private final int proposalNumber;
    private final Set<Integer> proposalNumbers;

    /**
     * Constructor of {@link NackMessageImpl}.
     *
     * @param shotNumber:      the shot number of the message (lattice instance).
     * @param proposalNumber:  the proposal number of the message.
     * @param proposalNumbers: the new proposal set.
     */
    public NackMessageImpl(final int shotNumber, final int proposalNumber, final Set<Integer> proposalNumbers) {
        this.shotNumber = shotNumber;
        this.proposalNumber = proposalNumber;
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
     * Get the new proposal set.
     *
     * @return the new proposal set.
     */
    public Set<Integer> getProposalNumbers() {
        return this.proposalNumbers;
    }

    @Override
    public String toString() {
        return "NackMessageImpl{" +
            "shotNumber=" + shotNumber +
            ", proposalNumber=" + proposalNumber +
            ", proposalNumbers=" + proposalNumbers.toString() +
            "}";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NackMessageImpl)) {
            return false;
        }
        final NackMessageImpl that = (NackMessageImpl) o;
        return getShotNumber() == that.getShotNumber() &&
            getProposalNumber() == that.getProposalNumber() &&
            getProposalNumbers().equals(that.getProposalNumbers());
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getShotNumber())
                + Integer.hashCode(getProposalNumber()) * 31
                + getProposalNumbers().hashCode() * 31 * 31;
    }

}
