package com.github.dedis.popstellar.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple interface describing objects that can be copied
 *
 * <p>It also store utility functions that can be used to create deep copy of collections
 *
 * @param <T> of the copied object
 */
public interface Copyable<T> {

  /**
   * Create a deep copy of the object
   *
   * @return The copy
   */
  T copy();

  /**
   * Create a copy of a map
   *
   * @param source map
   * @param <T> type of key
   * @param <C> type of value, must by a Copyable
   * @return the copy
   */
  static <T, C extends Copyable<C>> Map<T, C> copy(Map<T, C> source) {
    return source.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().copy()));
  }

  /**
   * Create a copy of a map containing lists
   *
   * @param source map
   * @param <E> type of key
   * @param <T> type of value inside the list (assumed to be immutable)
   * @return the copy
   */
  static <E, T> Map<E, List<T>> copyMapOfList(Map<E, List<T>> source) {
    return source.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> new ArrayList<>(entry.getValue())));
  }

  /**
   * Create a copy of a map containing sets
   *
   * @param source map
   * @param <E> type of key
   * @param <T> type of value inside the set (assumed to be immutable)
   * @return the copy
   */
  static <E, T> Map<E, Set<T>> copyMapOfSet(Map<E, Set<T>> source) {
    return source.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> new HashSet<>(entry.getValue())));
  }

  /**
   * Create a copy of a map preserving it's order
   *
   * @param source map
   * @param <T> type of key
   * @param <C> type of value, must by a Copyable
   * @return the copy
   */
  static <T, C extends Copyable<C>> LinkedHashMap<T, C> copyMapPreservingOrder(Map<T, C> source) {
    return source.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().copy(),
                (oldValue, newValue) -> newValue,
                LinkedHashMap::new));
  }
}
