package cs451.dataStructures.intRanges;

/**
 * Interface for the abstraction of a list of ranges of integers.
 * The list is represented as a linked list of ranges.
 */
public interface IntRanges {

    /**
     * Initialize the list with a single range.
     *
     * @param start: the start of the range.
     * @param end:   the end of the range.
     */
    void setRange(int start, int end);

    /**
     * Add a range to the list.
     *
     * @param start: the start of the range.
     * @param end:   the end of the range.
     */
    void addRange(int start, int end);

    /**
     * Add a value to the list.
     *
     * @param value: the value to add.
     */
    void addValue(int value);

    /**
     * Get the start of the first range.
     *
     * @return the start of the first range (-1 if the list is empty).
     */
    int getFirstRangeStart();

    /**
     * Get the end of the first range.
     *
     * @return the end of the first range (-1 if the list is empty).
     */
    int getFirstRangeEnd();

    /**
     * Remove the first range.
     */
    void removeFirstRange();

}
