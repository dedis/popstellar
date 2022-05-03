package util.examples.Lao

import ch.epfl.pop.json.MessageDataProtocol.GreetLaoFormat
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects._
import ch.epfl.pop.model.network.method.message.data.lao.GreetLao
import spray.json.enrichAny

object GreetLaoExamples {
  final val LAO_ID: Hash = Hash(Base64Data.encode("laoId"))
  final val FRONTEND: PublicKey = PublicKey(Base64Data("p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="))
  final val ADDRESS: String = "wss://popdemo.dedis.ch/demo"
  final val PEERS: List[String] = List("wss://popdemo.dedis.ch/second-organizer-demo", "wss://popdemo.dedis.ch/witness-demo")

  final val WRONG_FRONTEND: PublicKey = PublicKey(Base64Data.encode("wrong_frontend"))
  final val WRONG_ADDRESS: String = "popdemo.dedis.ch/demo"
  final val WRONG_PEERS: List[String] = List("popdemo.dedis.ch/second", "wss://popdemo.dedis.ch/witness-demo")

  val workingGreeting: GreetLao = GreetLao(LAO_ID, FRONTEND, ADDRESS, PEERS)
  final val MESSAGE_GREETING_WORKING: Message = new Message(
    Base64Data.encode(workingGreeting.toJson.toString),
    FRONTEND,
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(workingGreeting)
  )

  val wrongFrontendGreeting: GreetLao = GreetLao(LAO_ID, WRONG_FRONTEND, ADDRESS, PEERS)
  final val MESSAGE_GREETING_WRONG_FRONTEND: Message = new Message(
    Base64Data.encode(wrongFrontendGreeting.toJson.toString),
    FRONTEND,
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(wrongFrontendGreeting)
  )

  val wrongAddressGreeting: GreetLao = GreetLao(LAO_ID, FRONTEND, WRONG_ADDRESS, PEERS)
  final val MESSAGE_GREETING_WRONG_ADDRESS: Message = new Message(
    Base64Data.encode(wrongAddressGreeting.toJson.toString),
    FRONTEND,
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(wrongAddressGreeting)
  )

  val wrongPeersGreeting: GreetLao = GreetLao(LAO_ID, FRONTEND, ADDRESS, WRONG_PEERS)
  final val MESSAGE_GREETING_WRONG_PEERS: Message = new Message(
    Base64Data.encode(wrongPeersGreeting.toJson.toString),
    FRONTEND,
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(wrongPeersGreeting)
  )

  val emptyPeersGreeting: GreetLao = GreetLao(LAO_ID, FRONTEND, ADDRESS, List.empty)
  final val MESSAGE_GREETING_EMPTY_PEERS: Message = new Message(
    Base64Data.encode(emptyPeersGreeting.toJson.toString),
    FRONTEND,
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(emptyPeersGreeting)
  )
}


