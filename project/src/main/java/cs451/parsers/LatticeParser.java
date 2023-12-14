package cs451.parsers;

import java.util.Set;

/**
 * Interface for lattice parsers.
 */
public interface LatticeParser {

    /**
     * Get the next proposal.
     *
     * @return the next proposal.
     */
    Set<Integer> getNextProposal();

    /**
     * Get the number of shots.
     *
     * @return the number of shots.
     */
    int getP();

    /**
     * Get the maximum number of elements in a proposal.
     *
     * @return the maximum number of elements in a proposal.
     */
    int getVs();

    /**
     * Get the maximum number of distinct elements in all proposals.
     *
     * @return the maximum number of distinct elements in all proposals.
     */
    int getDs();

}
