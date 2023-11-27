package cs451.dataStructures.pair;

/**
 * Interface for a pair.
 */
public interface Pair {

    /**
     * Get the left value of the pair.
     *
     * @return The left value of the pair.
     */
    int getLeft();

    /**
     * Get the right value of the pair.
     *
     * @return The right value of the pair.
     */
    int getRight();

    /**
     * Set the left value of the pair.
     * 
     * @param left: the new left value.
     */
    void setLeft(int left);

    /**
     * Set the right value of the pair.
     * 
     * @param right: the new right value.
     */ 
    void setRight(int right);

}
