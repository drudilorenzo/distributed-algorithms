package cs451.dataStructures.pair;

/**
 * Implementation of {@link Pair}.
 */
public class PairImpl implements Pair {

    private int left;
    private int right;

    /**
     * Constructor.
     *
     * @param left:  the left value of the pair.
     * @param right: the right value of the pair.
     */
    public PairImpl(final int left, final int right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public int getLeft() {
        return this.left;
    }

    @Override
    public int getRight() {
        return this.right;
    }

    @Override
    public void setLeft(final int left) {
        this.left = left;
    }

    @Override
    public void setRight(final int right) {
        this.right = right;
    }

}
