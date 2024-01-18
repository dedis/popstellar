package com.github.dedis.popstellar.model

import java.util.stream.Collectors

/**
 * Simple interface describing objects that can be copied
 *
 * It also store utility functions that can be used to create deep copy of collections
 *
 * @param <T> of the copied object
 */
interface Copyable<T> {

  /**
   * Create a deep copy of the object
   *
   * @return The copy
   */
  fun copy(): T

  companion object {

    /**
     * Create a copy of a map
     *
     * @param source map
     * @param <T> type of key
     * @param <C> type of value, must by a Copyable
     * @return the copy
     */
    fun <T, C : Copyable<C>> copy(source: Map<T, C>): Map<T, C> {
      return source.entries.stream().collect(Collectors.toMap({ it.key }, { it.value.copy() }))
    }

    /**
     * Create a copy of a map containing lists
     *
     * @param source map
     * @param <E> type of key
     * @param <T> type of value inside the list (assumed to be immutable)
     * @return the copy
     */
    fun <E, T> copyMapOfList(source: Map<E, List<T>>): Map<E, List<T>> {
      return source.entries.stream().collect(Collectors.toMap({ it.key }, { ArrayList(it.value) }))
    }

    /**
     * Create a copy of a map containing sets
     *
     * @param source map
     * @param <E> type of key
     * @param <T> type of value inside the set (assumed to be immutable)
     * @return the copy
     */
    fun <E, T> copyMapOfSet(source: Map<E, Set<T>>): Map<E, Set<T>> {
      return source.entries.stream().collect(Collectors.toMap({ it.key }, { HashSet(it.value) }))
    }

    /**
     * Create a copy of a map preserving it's order
     *
     * @param source map
     * @param <T> type of key
     * @param <C> type of value, must by a Copyable
     * @return the copy </C></T>
     */
    fun <T, C : Copyable<C>> copyMapPreservingOrder(source: Map<T, C>): LinkedHashMap<T, C> {
      return source.entries
        .stream()
        .collect(
          Collectors.toMap(
            { it.key },
            { it.value.copy() },
            { _, newValue -> newValue },
            { LinkedHashMap() }
          )
        )
    }
  }
}
