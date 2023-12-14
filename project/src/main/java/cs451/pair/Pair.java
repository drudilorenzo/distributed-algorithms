package cs451.pair;

/**
 * Interface for pair.
 * @param <K>: the type of the first element of the pair.
 * @param <V>: the type of the second element of the pair.
 */
public interface Pair<K, V> {

    /**
     * Get the first element of the pair.
     *
     * @return the first element of the pair.
     */
    K getFirst();

    /**
     * Get the second element of the pair.
     *
     * @return the second element of the pair.
     */
    V getSecond();

}
