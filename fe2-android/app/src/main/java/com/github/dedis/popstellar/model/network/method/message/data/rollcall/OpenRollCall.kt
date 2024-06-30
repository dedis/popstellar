package com.github.dedis.popstellar.model.network.method.message.data.rollcall

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.utility.MessageValidator
import com.google.gson.annotations.SerializedName

/** Data sent to open a roll call */
@Immutable
class OpenRollCall : Data {
  @SerializedName("update_id") val updateId: String
  val opens: String

  @SerializedName("opened_at") val openedAt: Long

  override val action: String

  /**
   * Constructor of a data Open Roll-Call
   *
   * @param laoId id of lao
   * @param opens The 'update_id' of the latest roll call close, or in its absence, the 'id' field
   *   of the roll call creation
   * @param openedAt timestamp corresponding to roll call open
   * @param state the state in which the roll call is when this instance is created
   */
  constructor(laoId: String, opens: String, openedAt: Long, state: EventState) {
    validate(laoId, "laoId", opens, openedAt)

    this.updateId = RollCall.generateOpenRollCallId(laoId, opens, openedAt)
    this.opens = opens
    this.openedAt = openedAt

    this.action =
        if (state == EventState.CLOSED) {
          Action.REOPEN.action
        } else {
          Action.OPEN.action
        }
  }

  /**
   * Constructor of a data Open Roll-Call
   *
   * @param updateId id of the update
   * @param opens The 'update_id' of the latest roll call close, or in its absence, the 'id' field
   *   of the roll call creation
   *     @param openedAt timestamp corresponding to roll call open. Must be one of
   *       ["open", "reopen"]
   */
  constructor(updateId: String, opens: String, openedAt: Long, action: String) {
    validate(updateId, "updateId", opens, openedAt)
        .elementIsOneOf(action, "action", Action.OPEN.action, Action.REOPEN.action)

    this.updateId = updateId
    this.opens = opens
    this.openedAt = openedAt
    this.action = action
  }

  override val `object`: String
    get() = Objects.ROLL_CALL.`object`

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as OpenRollCall
    return openedAt == that.openedAt &&
        updateId == that.updateId &&
        opens == that.opens &&
        action == that.action
  }

  override fun hashCode(): Int {
    return java.util.Objects.hash(updateId, opens, openedAt, action)
  }

  override fun toString(): String {
    return "OpenRollCall{updateId='$updateId', opens='$opens', openedAt=$openedAt, action='$action'}"
  }

  private fun validate(
      id: String,
      idLabel: String,
      opens: String,
      openedAt: Long
  ): MessageValidator.MessageValidatorBuilder {
    return MessageValidator.verify()
        .isNotEmptyBase64(id, idLabel)
        .isNotEmptyBase64(opens, "opens")
        .greaterOrEqualThan(openedAt, 0, "openedAt")
  }
}
