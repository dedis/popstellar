package util.examples

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.lao.CreateLao
import ch.epfl.pop.model.network.method.message.data.rollCall.CloseRollCall
import ch.epfl.pop.model.network.method.message.data.socialMedia.AddChirp
import ch.epfl.pop.model.objects._
import spray.json._
import ch.epfl.pop.model.network.method.message.data.MessageData
import ch.epfl.pop.model.network.method.message.data.rollCall.CreateRollCall

object MessageExample {

  private final val PUBLICKEY: PublicKey = PublicKey(Base64Data("jsNj23IHALvppqV1xQfP71_3IyAHzivxiCz236_zzQc="))
  private final val PRIVATEKEY: PrivateKey = PrivateKey(Base64Data("qRfms3wzSLkxAeBz6UtwA-L1qP0h8D9XI1FSvY68t7Y="))

  final val NOT_STALE_TIMESTAMP = Timestamp(1577833201L)

  final val MESSAGE: Message = Message(
    Base64Data("eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="),
    PublicKey(Base64Data("to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=")),
    Signature(Base64Data("2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA==")),
    Hash(Base64Data("f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE=")),
    WitnessSignaturePair(PublicKey(Base64Data("d2l0MQ==")), Signature(Base64Data("c2lnMQ=="))) :: WitnessSignaturePair(PublicKey(Base64Data("d2l0Mg==")), Signature(Base64Data("c2lnMg=="))) :: Nil
  )

  final val MESSAGE_FAULTY_ID: Message = Message(
    Base64Data("eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="),
    PublicKey(Base64Data("to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=")),
    Signature(Base64Data("2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA==")),
    Hash(Base64Data("RkFVTFRZLUlE")),
    WitnessSignaturePair(PublicKey(Base64Data("d2l0MQ==")), Signature(Base64Data("c2lnMQ=="))) :: WitnessSignaturePair(PublicKey(Base64Data("d2l0Mg==")), Signature(Base64Data("c2lnMg=="))) :: Nil
  )

  //message with a valid Ed25519Sign WitnessSignaturePair
  final val MESSAGE_WORKING_WS_PAIR: Message = Message(
    Base64Data("eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="),
    PublicKey(Base64Data("to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=")),
    Signature(Base64Data("2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA==")),
    Hash(Base64Data("f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE=")),
    WitnessSignaturePair(PUBLICKEY, PRIVATEKEY.signData(Base64Data("f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE="))) :: Nil
  )

  //message with an invalid Ed25519Sign WitnessSignaturePair
  final val MESSAGE_FAULTY_WS_PAIR: Message = Message(
    Base64Data("eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="),
    PublicKey(Base64Data("to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=")),
    Signature(Base64Data("2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA==")),
    Hash(Base64Data.encode("invalid")),
    WitnessSignaturePair(PUBLICKEY, PRIVATEKEY.signData(Base64Data("f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE="))) :: Nil
  )

  //message with an invalid Signature
  final val MESSAGE_FAULTY_SIGNATURE: Message = Message(
    Base64Data("eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ=="),
    PublicKey(Base64Data("to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=")),
    Signature(Base64Data.encode("invalid")),
    Hash(Base64Data("f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE=")),
    WitnessSignaturePair(PUBLICKEY, PRIVATEKEY.signData(Base64Data("f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE="))) :: Nil
  )

  //CreateLao
  val organizer: PublicKey = PublicKey(Base64Data("to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic="))
  val organizerInvalid: PublicKey = PublicKey(Base64Data("to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLid="))
  val name: String = "LAO"
  val nameInvalid: String = "wrong"
  val creationWorking: Timestamp = NOT_STALE_TIMESTAMP
  val creationInvalid: Timestamp = Timestamp(0)
  val idWorking: Hash = Hash.fromStrings(organizer.base64Data.toString, creationWorking.toString, name)
  val idInvalid: Hash = Hash.fromStrings(organizer.base64Data.toString, creationWorking.toString, nameInvalid)
  val workingWitnessList: List[PublicKey] = PUBLICKEY :: Nil
  val invalidWitnessList: List[PublicKey] = PUBLICKEY :: PUBLICKEY :: Nil
  val workingWSPairList: List[WitnessSignaturePair] = WitnessSignaturePair(PUBLICKEY, PRIVATEKEY.signData(Base64Data("f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE="))) :: Nil


  private final val createLaoCorrect: CreateLao = CreateLao(idWorking, name, creationWorking, organizer, workingWitnessList)
  final val MESSAGE_CREATELAO_WORKING: Message = new Message(
    Base64Data.encode(createLaoCorrect.toJson.toString),
    organizer,
    Signature(Base64Data("2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA==")),
    Hash(Base64Data("f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE=")),
    workingWSPairList,
    Some(createLaoCorrect)
  )

  private final val createLaoWrongTimestamp: CreateLao = CreateLao(idWorking, name, creationInvalid, organizer, workingWitnessList)
  final val MESSAGE_CREATELAO_WRONG_TIMESTAMP: Message = new Message(
    Base64Data.encode(createLaoWrongTimestamp.toJson.toString),
    organizer,
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    workingWSPairList,
    Some(createLaoWrongTimestamp)
  )

  private final val createLaoWrongWitnesses: CreateLao = CreateLao(idWorking, name, creationWorking, organizer, invalidWitnessList)
  final val MESSAGE_CREATELAO_WRONG_WITNESSES: Message = new Message(
    Base64Data.encode(createLaoWrongWitnesses.toJson.toString),
    organizer,
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    workingWSPairList,
    Some(createLaoWrongWitnesses)
  )

  private final val createLaoWrongId: CreateLao = CreateLao(idInvalid, name, creationWorking, organizer, workingWitnessList)
  final val MESSAGE_CREATELAO_WRONG_ID: Message = new Message(
    Base64Data.encode(createLaoWrongWitnesses.toJson.toString),
    organizer,
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    workingWSPairList,
    Some(createLaoWrongId)
  )

  final val MESSAGE_CREATELAO_WRONG_SENDER: Message = new Message(
    Base64Data.encode(createLaoCorrect.toJson.toString),
    PublicKey(Base64Data.encode("wrong")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    workingWSPairList,
    Some(createLaoWrongId)
  )

  private final val createLaoEmptyName: CreateLao = createLaoCorrect.copy(name="")
  final val MESSAGE_CREATELAO_EMPTY_NAME: Message = new Message(
    Base64Data.encode(createLaoEmptyName.toJson.toString),
    organizer,
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    workingWSPairList,
    Some(createLaoEmptyName)
  )

  //we only care about the decoded data, the rest doesn't need to be right for current testing purposes
  final val MESSAGE_CREATELAO_SIMPLIFIED: Message = new Message(
    Base64Data.encode(CreateLao(Hash(Base64Data("aWQ=")), "LAO", NOT_STALE_TIMESTAMP, PublicKey(Base64Data("a2V5")), List.empty).toJson.toString),
    PublicKey(Base64Data("to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(CreateLao(Hash(Base64Data("aWQ=")), "LAO", NOT_STALE_TIMESTAMP, PublicKey(Base64Data("a2V5")), List.empty))
  )

  final val MESSAGE_CREATELAO2: Message = new Message(
    Base64Data.encode(CreateLao(Hash(Base64Data("aWQy")), "LAO2", Timestamp(0), PublicKey(Base64Data("a2V5Mg==")), List.empty).toJson.toString),
    PublicKey(Base64Data("to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(CreateLao(Hash(Base64Data("aWQy")), "LAO2", Timestamp(0), PublicKey(Base64Data("a2V5Mg==")), List.empty))
  )
   // Create Roll Call
  val createRollCallData: String = 
  """
        {
          "object": "roll_call",
          "action": "create",
          "id": "Slj7C1LBEXlRC8ItV2B0zWfUSD6YiGJt6N_I_m02uw4=",
          "name": "Roll Call ",
          "creation": 1633098853,
          "proposed_start": 1633099125,
          "proposed_end": 1633099140,
          "location": "EPFL",
          "description": "Food is welcome!"
        }
  """
  lazy val rollCallCreate: CreateRollCall = new CreateRollCall(Hash(Base64Data("Slj7C1LBEXlRC8ItV2B0zWfUSD6YiGJt6N_I_m02uw4=")), "Roll Call ", Timestamp(1633098853), Timestamp(1633099125), Timestamp(1633099140), "EPFL", Some("Food is welcome!"))
  lazy val rollCallCreateFromJson = CreateRollCall.buildFromJson(createRollCallData)
  final val MESSAGE_CREATEROLLCALL_VALID: Message =  new Message(
    Base64Data.encode(createRollCallData),
    organizer,
    Signature(Base64Data.encode(createRollCallData)),
    Hash(Base64Data("bgmzJEyaNoEQo3sA-Zky8pTZ9gMRjSW27ljm1vEPXMI=")),
    List.empty,
    Some(rollCallCreateFromJson)
  )

  final val MESSAGE_CLOSEROLLCALL: Message = new Message(
    Base64Data.encode(CloseRollCall(Hash(Base64Data("")), Hash(Base64Data("")), Timestamp(0), List(PublicKey(Base64Data("a2V5QXR0ZW5kZWU=")))).toJson.toString),
    PublicKey(Base64Data("to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(CloseRollCall(Hash(Base64Data("")), Hash(Base64Data("")), Timestamp(0), List(PublicKey(Base64Data("a2V5QXR0ZW5kZWU=")))))
  )

  final val MESSAGE_ADDCHIRP: Message = new Message(
    Base64Data.encode(AddChirp("abc", None, Timestamp(0)).toJson.toString),
    PublicKey(Base64Data("to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(AddChirp("abc", None, Timestamp(0)))
  )
}
