package com.github.dedis.popstellar.model.network.method.message.data

import com.github.dedis.popstellar.utility.error.DataHandlingException
import com.github.dedis.popstellar.utility.error.UnhandledDataTypeException
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.error.UnknownRollCallException
import com.github.dedis.popstellar.utility.error.UnknownWitnessMessageException
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import com.github.dedis.popstellar.utility.handler.data.DataHandler
import com.github.dedis.popstellar.utility.handler.data.HandlerContext
import java.util.Collections
import java.util.Optional
import timber.log.Timber

/** A registry of Data classes and handlers */
class DataRegistry private constructor(mapping: Map<EntryPair, Entry<out Data>>) {
  /** A mapping of (object, action) -> (class, handler) */
  private val mapping: Map<EntryPair, Entry<out Data>> = Collections.unmodifiableMap(mapping)

  /**
   * Return the class assigned to the pair (obj, action)
   *
   * @param obj of the entry
   * @param action of the entry
   * @return the class assigned to the pair or empty if none are defined
   */
  fun getType(obj: Objects, action: Action): Optional<Class<out Data?>> {
    Timber.tag(TAG).d("getting data type")
    return Optional.ofNullable(mapping[pair(obj, action)]).map { it.dataClass }
  }

  /**
   * Found the DataHandler corresponding to the given (obj, action) and execute it with the given
   * data and context.
   *
   * @param context the HandlerContext of the message
   * @param data the Data to be handle
   * @param obj of the entry
   * @param action of the entry
   * @throws DataHandlingException if an error occurs or if there was no handler
   */
  @Throws(
      DataHandlingException::class,
      UnknownLaoException::class,
      UnknownRollCallException::class,
      UnknownElectionException::class,
      NoRollCallException::class,
      UnknownWitnessMessageException::class)
  fun handle(context: HandlerContext, data: Data, obj: Objects, action: Action) {
    Optional.ofNullable(mapping[pair(obj, action)])
        .orElseThrow { UnhandledDataTypeException(data, "$obj#$action") }
        .handleData(context, data)
  }

  /** Entry of the messages map. A pair of (Objects, Action) */
  class EntryPair
  /**
   * Constructor for the EntryPair
   *
   * @param object of the pair
   * @param action of the pair
   */
  (val `object`: Objects, val action: Action) {
    override fun equals(other: Any?): Boolean {
      if (this === other) {
        return true
      }
      if (other == null || javaClass != other.javaClass) {
        return false
      }
      val entryPair = other as EntryPair
      return `object` === entryPair.`object` && action === entryPair.action
    }

    override fun hashCode(): Int {
      return java.util.Objects.hash(`object`, action)
    }
  }

  /**
   * Entry of the DataRegistry.
   *
   * @param <T> generic type of class that extends Data </T>
   */
  class Entry<T : Data>(
      val key: EntryPair,
      val dataClass: Class<T>,
      private val dataHandler: DataHandler<T>?
  ) {
    /**
     * Handle the given data using the given context
     *
     * @param context the HandlerContext of the message
     * @param data the Data to be handle
     * @throws DataHandlingException if an error occurs or if the dataHandler is null
     */
    @Throws(
        DataHandlingException::class,
        UnknownLaoException::class,
        UnknownRollCallException::class,
        UnknownElectionException::class,
        NoRollCallException::class,
        UnknownWitnessMessageException::class)
    fun handleData(context: HandlerContext, data: Data) {
      if (dataHandler == null) {
        throw UnhandledDataTypeException(data, "${key.`object`}#${key.action}")
      }

      dataHandler.accept(context, data as T)
    }
  }

  /** Builder of the DataRegistry */
  class Builder {
    private val mapping: MutableMap<EntryPair, Entry<out Data>>

    init {
      mapping = HashMap()
    }

    /**
     * Add an entry to the builder and check if it was already present.
     *
     * @param obj of the entry to add
     * @param action of the entry to add
     * @param dataClass of the entry to add
     * @param dataHandler of the entry to add
     * @param <T> generic type of class that extends Data
     * @return the builder
     * @throws IllegalArgumentException if the key of the entry is already present </T>
     */
    fun <T : Data> add(
        obj: Objects,
        action: Action,
        dataClass: Class<T>,
        dataHandler: DataHandler<T>?
    ): Builder {
      return add(Entry(pair(obj, action), dataClass, dataHandler))
    }

    /**
     * Add an entry to the builder and check if it was already present.
     *
     * @param entry the new entry to add
     * @param <T> generic type of class that extends Data
     * @return the builder
     * @throws IllegalArgumentException if the key of the entry is already present </T>
     */
    fun <T : Data> add(entry: Entry<T>): Builder {
      require(!mapping.containsKey(entry.key)) { "They key ${entry.key} already exists" }

      mapping[entry.key] = entry
      return this
    }

    fun build(): DataRegistry {
      return DataRegistry(HashMap(mapping))
    }
  }

  companion object {
    private val TAG = DataRegistry::class.java.simpleName

    /**
     * Create an entry pair given obj and action
     *
     * @param obj of the pair
     * @param action of the pair
     * @return the pair
     */
    private fun pair(obj: Objects, action: Action): EntryPair {
      return EntryPair(obj, action)
    }
  }
}
