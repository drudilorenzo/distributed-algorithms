package cs451.lattice.messageTypes;

/**
 * Implementation of {@link LatticeMessage} for ack messages.
 */
public class AckMessageImpl implements LatticeMessage {

    private final int shotNumber;
    private final int proposalNumber;

    /**
     *
     * @param shotNumber
     * @param proposalNumber
     */
    public AckMessageImpl(final int shotNumber, final int proposalNumber) {
        this.shotNumber = shotNumber;
        this.proposalNumber = proposalNumber;
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

    @Override
    public String toString() {
        return "AckMessageImpl{" +
            "shotNumber=" + shotNumber +
            ", proposalNumber=" + proposalNumber +
            "}";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AckMessageImpl)) {
            return false;
        }
        final AckMessageImpl that = (AckMessageImpl) o;
        return getShotNumber() == that.getShotNumber() &&
            getProposalNumber() == that.getProposalNumber();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getShotNumber())
                + Integer.hashCode(getProposalNumber()) * 31;
    }

}
