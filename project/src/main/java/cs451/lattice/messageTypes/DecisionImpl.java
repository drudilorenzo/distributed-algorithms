package cs451.lattice.messageTypes;

import java.util.Set;

/**
 * Implementation of {@link LatticeMessage} for decision messages.
 */
public class DecisionImpl implements LatticeMessage {

    private final int shotNumber;
    private final Set<Integer> decision;

    /**
     * Constructor of {@link DecisionImpl}.
     *
     * @param shotNumber: the shot number of the message (lattice instance).
     * @param decision:   the decision.
     */
    public DecisionImpl(final int shotNumber, final Set<Integer> decision) {
        this.shotNumber = shotNumber;
        this.decision = Set.copyOf(decision);
    }

    @Override
    public int getShotNumber() {
        return this.shotNumber;
    }

    /**
     * Get the decision.
     *
     * @return the decision.
     */
    public Set<Integer> getDecision() {
        return this.decision;
    }

    @Override
    public String toString() {
        return "DecisionImpl{" +
            "shotNumber=" + shotNumber +
            ", decision=" + decision.toString() +
            "}";
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this){
            return true;
        }
        if (!(o instanceof DecisionImpl)) {
            return false;
        }
        final DecisionImpl other = (DecisionImpl) o;
        if (this.shotNumber != other.shotNumber) {
            return false;
        }
        if (!this.decision.equals(other.decision)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + this.shotNumber;
        result = 31 * result + this.decision.hashCode();
        return result;
    }

}
