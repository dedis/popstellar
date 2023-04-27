package util.examples.Lao

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.lao.GreetLao
import ch.epfl.pop.model.objects._
import spray.json._

object GreetLaoExamples {

  final val LAO: Hash = Hash(Base64Data.encode("laoId"))
  final val SENDER: PublicKey = PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM="))
  final val ADDRESS: String = "ws://popdemo.dedis.ch"
  final val PEERS: List[String] = List.empty
  final val SIGNATURE: Signature = Signature(Base64Data(""))

  final val wrongSender = PublicKey(Base64Data.encode("wrong_sender"))
  final val wrongLao = Hash(Base64Data.encode("wrong_lao"))
  final val wrongAddress = "popdemo.dedis.ch"

  final val greetLao: GreetLao = GreetLao(LAO, SENDER, ADDRESS, PEERS)
  final val MESSAGE_GREET_LAO: Message = new Message(
    Base64Data.encode(GreetLaoFormat.write(greetLao).toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(greetLao)
  )

  final val greetLaoWrongFrontend: GreetLao = GreetLao(LAO, wrongSender, ADDRESS, PEERS)
  final val MESSAGE_GREET_LAO_WRONG_FRONTEND: Message = new Message(
    Base64Data.encode(GreetLaoFormat.write(greetLaoWrongFrontend).toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(greetLaoWrongFrontend)
  )

  final val wronLaoGreet: Hash = Hash(Base64Data.encode("laoId/election"))
  final val greetLaoWrongChannel: GreetLao = GreetLao(wronLaoGreet, SENDER, ADDRESS, PEERS)
  final val MESSAGE_GREET_LAO_WRONG_CHANNEL: Message = new Message(
    Base64Data.encode(GreetLaoFormat.write(greetLaoWrongChannel).toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(greetLaoWrongChannel)
  )

  final val greetLaoWrongLao: GreetLao = GreetLao(wrongLao, SENDER, ADDRESS, PEERS)
  final val MESSAGE_GREET_LAO_WRONG_LAO: Message = new Message(
    Base64Data.encode(greetLaoWrongLao.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(greetLaoWrongLao)
  )

  final val greetLaoWrongAddress: GreetLao = GreetLao(LAO, SENDER, wrongAddress, PEERS)
  final val MESSAGE_GREET_LAO_WRONG_ADDRESS: Message = new Message(
    Base64Data.encode(greetLaoWrongAddress.toJson.toString),
    SENDER,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(greetLaoWrongAddress)
  )

  final val MESSAGE_GREET_LAO_WRONG_OWNER: Message = new Message(
    Base64Data.encode(greetLaoWrongAddress.toJson.toString),
    wrongSender,
    SIGNATURE,
    Hash(Base64Data("")),
    List.empty,
    Some(greetLaoWrongAddress)
  )
}
