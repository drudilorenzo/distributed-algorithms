package cs451.dataStructures.pair;

/**
 * Implementation of {@link Pair}.
 */
public class PairImpl implements Pair, Comparable<Pair> {

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
    public void setLeft(int left) {
        this.left = left;
    }

    @Override
    public void setRight(int right) {
        this.right = right;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        return this.getLeft() == ((PairImpl) obj).getLeft() && this.getRight() == ((PairImpl) obj).getRight();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.getLeft()) + Integer.hashCode(this.getRight());
    }

    @Override
    public int compareTo(Pair p) {
        int retVal = Integer.compare(this.getLeft(), p.getLeft());
        if (retVal != 0) {
            return retVal;
        }
        return Integer.compare(this.getRight(), p.getRight());
    }

    @Override
    public String toString() {
        return "(" + this.getLeft() + ", " + this.getRight() + ")";
    }
}
