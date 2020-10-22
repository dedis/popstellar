package json

import json.JsonMessages._
import spray.json._

object JsonCommunicationProtocol extends DefaultJsonProtocol {

  /*
   * ------------ Administration messages ------------
   */

  implicit val createLaoMessageClientFormat: RootJsonFormat[CreateLaoMessageClient] = jsonFormat5(CreateLaoMessageClient)
  implicit val joinLaoMessageClientFormat: RootJsonFormat[JoinLaoMessageClient] = jsonFormat3(JoinLaoMessageClient)
  implicit val createEventMessageClientFormat: RootJsonFormat[CreateEventMessageClient] = jsonFormat4(CreateEventMessageClient)
  implicit val createElectionMessageClientFormat: RootJsonFormat[CreateElectionMessageClient] = jsonFormat3(CreateElectionMessageClient)
  implicit val castVoteMessageClientFormat: RootJsonFormat[CastVoteMessageClient] = jsonFormat4(CastVoteMessageClient)

  implicit val answerMessageServerFormat: RootJsonFormat[AnswerMessageServer] = jsonFormat2(AnswerMessageServer)


  /*
   * ---------------- PubSub messages ----------------
   */

  implicit val createChannelClientFormat: RootJsonFormat[CreateChannelClient] = jsonFormat2(CreateChannelClient)
  implicit val publishChannelClientFormat: RootJsonFormat[PublishChannelClient] = jsonFormat2(PublishChannelClient)
  implicit val subscribeChannelClientFormat: RootJsonFormat[SubscribeChannelClient] = jsonFormat1(SubscribeChannelClient)
  implicit val fetchChannelClientFormat: RootJsonFormat[FetchChannelClient] = jsonFormat2(FetchChannelClient)

  implicit val notifyChannelServerFormat: RootJsonFormat[NotifyChannelServer] = jsonFormat2(NotifyChannelServer)
  implicit val fetchChannelServerFormat: RootJsonFormat[FetchChannelServer] = jsonFormat3(FetchChannelServer)


}
