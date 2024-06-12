package com.github.dedis.popstellar.model.network.method.message.data

import java.util.Collections

/** Enumerates all possible messages actions */
enum class Action
/**
 * Constructor for a message Action
 *
 * @param action the name of the action
 */
(
    /** Returns the name of the Action. */
    val action: String
) {
  CREATE("create"),
  END("end"),
  RESULT("result"),
  SETUP("setup"),
  UPDATE("update_properties"),
  STATE("state"),
  GREET("greet"),
  WITNESS("witness"),
  OPEN("open"),
  REOPEN("reopen"),
  CLOSE("close"),
  CAST_VOTE("cast_vote"),
  ELECT("elect"),
  ELECT_ACCEPT("elect_accept"),
  PREPARE("prepare"),
  PROMISE("promise"),
  PROPOSE("propose"),
  ACCEPT("accept"),
  LEARN("learn"),
  FAILURE("failure"),
  ADD("add"),
  NOTIFY_ADD("notify_add"),
  DELETE("delete"),
  NOTIFY_DELETE("notify_delete"),
  KEY("key"),
  POST_TRANSACTION("post_transaction"),
  AUTH("authenticate"),
  CHALLENGE_REQUEST("challenge_request"),
  CHALLENGE("challenge"),
  INIT("init"),
  EXPECT("expect"),
  RUMOR("rumor");

  /**
   * Field to decide whether to store the message content. This is used to save memory, as some
   * messages don't need to be saved but rather only their ids is necessary to avoid reprocessing.
   *
   * @return true if the message content is useful for future retrieval and thus has to be stored,
   *   false otherwise.
   */
  val isStoreNeededByAction: Boolean
    get() = // So far only the cast vote message relies on a previous message retrieval
    action == CAST_VOTE.action

  companion object {
    private val ALL = values().toList().let(Collections::unmodifiableList)

    /**
     * Find a given Action
     *
     * @param searched the searched action
     * @return the corresponding enum action
     */
    fun find(searched: String): Action? {
      return ALL.stream()
          .filter { action: Action -> action.action == searched }
          .findFirst()
          .orElse(null)
    }
  }
}
