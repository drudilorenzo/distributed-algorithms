package cs451.pair;

/**
 * Implementation of {@link Pair}.
 * @param <K>: the type of the first element of the pair.
 * @param <V>: the type of the second element of the pair.
 */
public class PairImpl<K, V> implements Pair<K, V> {

    private final K first;
    private final V second;

    /**
     * Constructor of {@link PairImpl}.
     *
     * @param first:  the first element of the pair.
     * @param second: the second element of the pair.
     */
    public PairImpl(final K first, final V second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public K getFirst() {
        return this.first;
    }

    @Override
    public V getSecond() {
        return this.second;
    }

}
