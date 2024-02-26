package ch.epfl.pop.model.network.method.message.data

trait MessageData {
  val _object: ObjectType
  val action: ActionType
}
