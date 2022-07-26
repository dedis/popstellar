package ch.epfl.pop.model.network.method.message.data.rollCall

import ch.epfl.pop.model.objects.{Hash, Timestamp}

/** Provides a useful trait for [[OpenRollCall]] and [[ReopenRollCall]]
  */
trait IOpenRollCall {
  val update_id: Hash
  val opens: Hash
  val opened_at: Timestamp
}
