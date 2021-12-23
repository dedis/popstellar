package com.github.dedis.popstellar.model.network.method.message.data;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.error.UnhandledDataTypeException;
import com.github.dedis.popstellar.utility.handler.DataHandler;
import com.github.dedis.popstellar.utility.handler.HandlerContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** A registry of Data classes and handlers */
public final class DataRegistry {

  /** A mapping of (object, action) -> (class, handler) */
  private final Map<EntryPair, Entry<? extends Data>> mapping;

  private DataRegistry(Map<EntryPair, Entry<? extends Data>> mapping) {
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
    return Optional.ofNullable(mapping.get(pair(obj, action))).map(Entry::getDataClass);
  }

  public void handle(HandlerContext context, Data data, Objects obj, Action action)
      throws DataHandlingException {
    Optional.ofNullable(mapping.get(pair(obj, action)))
        .orElseThrow(() -> new UnhandledDataTypeException(data, obj + "#" + action))
        .handleData(context, data);
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

  public static final class Entry<T extends Data> {

    private final EntryPair key;
    private final Class<T> dataClass;
    private final DataHandler<T> dataHandler;

    private Entry(
        @NonNull EntryPair key, @NonNull Class<T> dataClass, @Nullable DataHandler<T> dataHandler) {
      this.key = key;
      this.dataClass = dataClass;
      this.dataHandler = dataHandler;
    }

    public EntryPair getKey() {
      return key;
    }

    public Class<T> getDataClass() {
      return dataClass;
    }

    @SuppressWarnings("unchecked")
    public void handleData(HandlerContext context, Data data) throws DataHandlingException {
      if (dataHandler == null) {
        throw new UnhandledDataTypeException(data, key.object + "#" + key.action);
      }
      dataHandler.accept(context, (T) data);
    }
  }

  public static final class Builder {

    private final Map<EntryPair, Entry<? extends Data>> mapping;

    public Builder() {
      this.mapping = new HashMap<>();
    }

    public <T extends Data> Builder add(
        @NonNull Objects obj,
        @NonNull Action action,
        @NonNull Class<T> dataClass,
        @Nullable DataHandler<T> dataHandler) {
      return add(new Entry<>(pair(obj, action), dataClass, dataHandler));
    }

    public <T extends Data> Builder add(@NonNull Entry<T> entry) {
      if (mapping.containsKey(entry.key)) {
        throw new IllegalArgumentException(String.format("They key %s already exists", entry.key));
      }
      mapping.put(entry.key, entry);
      return this;
    }

    @NonNull
    public DataRegistry build() {
      return new DataRegistry(new HashMap<>(mapping));
    }
  }
}
