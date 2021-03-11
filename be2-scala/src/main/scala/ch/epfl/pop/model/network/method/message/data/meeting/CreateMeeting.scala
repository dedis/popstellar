package ch.epfl.pop.model.network.method.message.data.meeting

import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType, Parsable}
import ch.epfl.pop.model.objects.{Hash, Timestamp}

case class CreateMeeting(
                          id: Hash,
                          name: String,
                          creation: Timestamp,
                          location: Option[String],
                          start: Timestamp,
                          end: Option[Timestamp],
                          extra: Option[Any]
                        ) {
  private final val _object = ObjectType.MEETING
  private final val action = ActionType.CREATE
}

object CreateMeeting extends Parsable {
  def apply(
             id: Hash,
             name: String,
             creation: Timestamp,
             location: Option[String],
             start: Timestamp,
             end: Option[Timestamp],
             extra: Option[Any]
           ): CreateMeeting = {
    // FIXME add checks
    new CreateMeeting(id, name, creation, location, start, end, extra)
  }

  override def buildFromJson(messageData: MessageData, payload: String): Any = ???
}
