package com.github.dedis.popstellar.model.network.method.message.data;

import android.util.Log;

import androidx.core.util.Pair;

import com.github.dedis.popstellar.utility.handler.DataHandler;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/** A registry of Data classes and handlers */
public final class DataRegistry {

  /** A mapping of (object, action) -> (class, handler) */
  private final Map<EntryPair, Pair<Class<? extends Data>, DataHandler<? extends Data>>> mapping;

  public DataRegistry(
      Map<EntryPair, Pair<Class<? extends Data>, DataHandler<? extends Data>>> mapping) {
    this.mapping = Collections.unmodifiableMap(mapping);
  }

  /**
   * Return the class assigned to the pair (obj, action)
   *
   * @param obj of the entry
   * @param action of the entry
   * @return the class assigned to the pair or empty if none are defined
   */
  public Optional<Class<? extends Data>> getType(Objects obj, Action action) {
    Log.d("data", "getting data type");
    return Optional.ofNullable(mapping.get(pair(obj, action))).map(p -> p.first);
  }

  /**
   * Return the data handler assigned to the pair (obj, action)
   *
   * @param obj of the entry
   * @param action of the entry
   * @return the DataHandler assigned to the pair or empty if none are defined
   */
  @SuppressWarnings("rawtypes")
  public Optional<DataHandler> getDataHandler(Objects obj, Action action) {
    return Optional.ofNullable(mapping.get(pair(obj, action))).map(p -> p.second);
  }

  /**
   * Create an entry pair given obj and action
   *
   * @param obj of the pair
   * @param action of the pair
   * @return the pair
   */
  public static EntryPair pair(Objects obj, Action action) {
    return new EntryPair(obj, action);
  }

  /** Entry of the messages map. A pair of (Objects, Action) */
  public static final class EntryPair {

    private final Objects object;
    private final Action action;

    /**
     * Constructor for the EntryPair
     *
     * @param object of the pair
     * @param action of the pair
     */
    private EntryPair(Objects object, Action action) {
      this.object = object;
      this.action = action;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      EntryPair entryPair = (EntryPair) o;
      return object == entryPair.object && action == entryPair.action;
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(object, action);
    }
  }
}
