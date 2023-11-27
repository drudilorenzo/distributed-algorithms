package cs451.dataStructures.intRanges;

import cs451.dataStructures.pair.Pair;
import cs451.dataStructures.pair.PairImpl;

/**
 * Implementation of {@link IntRanges}.
 */
public class IntRangesImpl implements IntRanges {

    // since we assume all the values are positive, we use -1 to represent NaN.
    private static final int NaN_VALUE = -1;

    private IntRange head; // head of the linked list of ranges

    /**
     * Constructor of {@link IntRangesImpl}.
     */
    public IntRangesImpl() {
        this.head = null; // initially the list is empty
    }

    @Override
    public void setRange(final int start, final int end) {
        this.head = new IntRange(start, end);
    }

    @Override
    public void addRange(final int start, final int end) {
        IntRange prev = null;
        IntRange current = this.head;
        int lowerBound = IntRangesImpl.NaN_VALUE;

        // iterate over the list
        while (current != null) {
            if (lowerBound == IntRangesImpl.NaN_VALUE) {
                if (start < current.getStart()) {
                    lowerBound = start;
                } else if (current.contains(start) || start == current.getEnd() + 1) {
                    lowerBound = current.getStart();
                }
            }

            if (lowerBound != IntRangesImpl.NaN_VALUE) {
                IntRange newRange = null;
                if (current.contains(end) || end == current.getStart() - 1) {
                    newRange = new IntRange(lowerBound, current.getEnd(), current.getNext());
                } else if (end < current.getStart()) {
                    newRange = new IntRange(lowerBound, end, current);
                }
                if (prev != null) {
                    prev.setNext(newRange);
                } else {
                    this.head = newRange;
                }
                return; // range added, we can return
            } else {
                // if we have found the lower bound, we don't have to update
                // the previous range. That is because the new range will be
                // added exactly after the current previous.
                prev = current;
            }
            current = current.getNext();
        }

        // if we reach this point, it means that the new range is the last
        // range of the list.
        lowerBound = lowerBound == IntRangesImpl.NaN_VALUE ? start : lowerBound;
        if (prev != null) {
            prev.setNext(new IntRange(lowerBound, end));
        } else {
            this.head = new IntRange(lowerBound, end);
        }
    }

    @Override
    public void addValue(final int value) {
        this.addRange(value, value);
    }

    @Override
    public int getFirstRangeStart() {
        if (this.head == null) {
            return IntRangesImpl.NaN_VALUE;
        }
        return this.head.getStart();
    }

    @Override
    public int getFirstRangeEnd() {
        if (this.head == null) {
            return IntRangesImpl.NaN_VALUE;
        }
        return this.head.getEnd();
    }

    @Override
    public void removeFirstRange() {
        if (this.head == null) {
            return;
        }
        this.head = this.head.getNext();
    }

    private static class IntRange {

        private final Pair p;
        private IntRange next;

        public IntRange(final int start, final int end, final IntRange next) {
            this.p = new PairImpl(start, end);
            this.next = next;
        }

        public IntRange(final int start, final int end) {
            this(start, end, null);
        }

        public int getStart() {
            return this.p.getLeft();
        }

        public int getEnd() {
            return this.p.getRight();
        }

        public boolean contains(final int i) {
            return this.getStart() <= i && i <= this.getEnd();
        }

        public IntRange getNext() {
            return this.next;
        }

        public void setNext(final IntRange next) {
            this.next = next;
        }
    }

}
