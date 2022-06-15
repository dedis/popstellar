package util.examples

import ch.epfl.pop.json.MessageDataProtocol._
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.lao.CreateLao
import ch.epfl.pop.model.network.method.message.data.rollCall.CloseRollCall
import ch.epfl.pop.model.network.method.message.data.socialMedia.AddChirp
import ch.epfl.pop.model.network.method.message.data.meeting.CreateMeeting
import ch.epfl.pop.model.objects._
import spray.json._
import java.sql.Time
import ch.epfl.pop.model.network.method.message.data.meeting.StateMeeting
import java.{util => ju}

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
    Base64Data.encode(createLaoWrongId.toJson.toString),
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

  final val creationMeeting: Timestamp = Timestamp(1633098331)
  final val nameMeeting : String = "Meeting"
  final val laoIdMEeeting: String = "p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="
  final val HASH_MEETING_OBJECT: Hash = Hash.fromStrings("M", laoIdMEeeting, ""+creationMeeting, nameMeeting)
  final val mettingCreate: CreateMeeting = CreateMeeting(HASH_MEETING_OBJECT, nameMeeting, creationMeeting, Some("EPFL"),  Timestamp(1633098900), Some(Timestamp(1633102500)), None)
  final val MESSAGE_CREATE_MEETING: Message = new Message(
    Base64Data.encode(mettingCreate.toJson.toString()),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(mettingCreate)
  )

  final val laoIdMeetingWrongChannel: String = "wrongMeetingChannel/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="
  final val HASH_MEETING_OBJECT_WRONG_CHANNEL: Hash = Hash.fromStrings("M", laoIdMeetingWrongChannel, ""+creationMeeting, nameMeeting)
  final val mettingCreateWrongChannel: CreateMeeting = CreateMeeting(HASH_MEETING_OBJECT, nameMeeting, creationMeeting, Some("EPFL"),  Timestamp(1633098900), Some(Timestamp(1633102500)), None)
  final val MESSAGE_CREATE_MEETING_WRONG_CHANNEL: Message = new Message(
    Base64Data.encode(mettingCreateWrongChannel.toJson.toString()),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(mettingCreateWrongChannel)
  )

  final val HASH_MEETING_OBJECT_WRONG_DATA: Hash = Hash.fromStrings("M", laoIdMeetingWrongChannel, ""+creationMeeting, nameMeeting, "wrongElement")
  final val meetingCreateWrongData: CreateMeeting = CreateMeeting(HASH_MEETING_OBJECT_WRONG_DATA, nameMeeting, creationMeeting, Some("EPFL"),  Timestamp(1633098900), Some(Timestamp(1633102500)), None)
  final val MESSAGE_CREATE_MEETING_WRONG_DATA: Message = new Message(
    Base64Data.encode(meetingCreateWrongData.toJson.toString()),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(meetingCreateWrongData)
  )

  final val creationMeetingSmall: Timestamp = Timestamp(123)
  final val HASH_MEETING_OBJECT_STALE_CREATION: Hash = Hash.fromStrings("M", laoIdMEeeting, ""+creationMeetingSmall, nameMeeting)
  final val meetingCreateSmallCreation: CreateMeeting = CreateMeeting(HASH_MEETING_OBJECT_STALE_CREATION, nameMeeting, creationMeetingSmall, Some("EPFL"),  Timestamp(1633098900), Some(Timestamp(1633102500)), None)
  final val MESSAGE_CREATE_MEETING_SMALL_CREATION: Message = new Message(
    Base64Data.encode(meetingCreateSmallCreation.toJson.toString()),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(meetingCreateSmallCreation)
  )

  final val meetingCreateSmallStart: CreateMeeting = CreateMeeting(HASH_MEETING_OBJECT, nameMeeting, creationMeeting, Some("EPFL"),  Timestamp(170), Some(Timestamp(1633102500)), None)
  final val MESSAGE_CREATE_MEETING_SMALL_START: Message = new Message(
    Base64Data.encode(meetingCreateSmallStart.toJson.toString()),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(meetingCreateSmallStart)
  )

final val meetingCreateInvalidEnd: CreateMeeting = CreateMeeting(HASH_MEETING_OBJECT, nameMeeting, creationMeeting, Some("EPFL"),  Timestamp(1633098400), Some(Timestamp(1633098200)), None)
final val MESSAGE_CREATE_MEETING_SMALL_END: Message = new Message(
    Base64Data.encode(meetingCreateInvalidEnd.toJson.toString()),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(meetingCreateInvalidEnd)
  )

  final val meetingCreateSmallEnd: CreateMeeting = CreateMeeting(HASH_MEETING_OBJECT, nameMeeting, creationMeeting, Some("EPFL"),  Timestamp(1633102500), Some(Timestamp(1633102000)), None)
  final val MESSAGE_CREATE_MEETING_START_BIGGER_THAN_END: Message = new Message(
    Base64Data.encode(meetingCreateSmallEnd.toJson.toString()),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(meetingCreateSmallEnd)
  )

  final val modificationId: Hash = HASH_MEETING_OBJECT
  final val lastModified: Timestamp = Timestamp(1633098340)
  final val witnessSignatures: List[WitnessSignaturePair] = WitnessSignaturePair.apply(PublicKey(Base64Data("M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=")), Signature(Base64Data(""))) :: List.empty
  final val validStateMeeting: StateMeeting = StateMeeting(HASH_MEETING_OBJECT, nameMeeting, creationMeeting, lastModified, Some("EPFL"),  Timestamp(1633098900), Some(Timestamp(1633102500)), None, modificationId, witnessSignatures)
  final val MESSAGE_STATE_MEETING: Message = new Message(
    Base64Data.encode(validStateMeeting.toJson.toString()),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(validStateMeeting)
  )

  final val invalidStateMeetingWrongData: StateMeeting = StateMeeting(HASH_MEETING_OBJECT_WRONG_DATA, nameMeeting, creationMeeting, lastModified, Some("EPFL"),  Timestamp(1633098900), Some(Timestamp(1633102500)), None, modificationId, witnessSignatures)
  final val MESSAGE_STATE_MEETING_INVALID_DATA: Message = new Message(
    Base64Data.encode(invalidStateMeetingWrongData.toJson.toString()),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(invalidStateMeetingWrongData)
  )

  final val invalidStateMeetingStaleCreationTime: StateMeeting = StateMeeting(HASH_MEETING_OBJECT_STALE_CREATION, nameMeeting, Timestamp(123), lastModified, Some("EPFL"),  Timestamp(1633098900), Some(Timestamp(1633102500)), None, modificationId, witnessSignatures)
  final val MESSAGE_STATE_MEETING_INVALID_CREATION: Message = new Message(
    Base64Data.encode(invalidStateMeetingStaleCreationTime.toJson.toString()),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(invalidStateMeetingStaleCreationTime)
  )

  final val invalidStateMeetingStaleStartTime: StateMeeting = StateMeeting(HASH_MEETING_OBJECT, nameMeeting, creationMeeting, lastModified, Some("EPFL"),  Timestamp(163), Some(Timestamp(1633102500)), None, modificationId, witnessSignatures)
  final val MESSAGE_STATE_MEETING_INVALID_START: Message = new Message(
    Base64Data.encode(invalidStateMeetingStaleStartTime.toJson.toString()),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(invalidStateMeetingStaleStartTime)
  )

  final val invalidStateMeetingSmallStartTime: StateMeeting = StateMeeting(HASH_MEETING_OBJECT, nameMeeting, creationMeeting, lastModified, Some("EPFL"),  Timestamp(1633098300), Some(Timestamp(1633102500)), None, modificationId, witnessSignatures)
  final val MESSAGE_STATE_MEETING_SMALL_START: Message = new Message(
    Base64Data.encode(invalidStateMeetingSmallStartTime.toJson.toString()),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(invalidStateMeetingSmallStartTime)
  )

  final val invalidStateMeetingSmallEndTime: StateMeeting = StateMeeting(HASH_MEETING_OBJECT, nameMeeting, creationMeeting, lastModified, Some("EPFL"),  Timestamp(1633098400), Some(Timestamp(1633098380)), None, modificationId, witnessSignatures)
  final val MESSAGE_STATE_MEETING_SMALL_END: Message = new Message(
    Base64Data.encode(invalidStateMeetingSmallEndTime.toJson.toString()),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(invalidStateMeetingSmallEndTime)
  )

  
  final val invalidStateMeetingBigStartTime: StateMeeting = StateMeeting(HASH_MEETING_OBJECT, nameMeeting, creationMeeting, lastModified, Some("EPFL"),  Timestamp(1633102500), Some(Timestamp(1633098900)), None, modificationId, witnessSignatures)
  final val  MESSAGE_STATE_MEETING_BIG_START: Message = new Message(
    Base64Data.encode(invalidStateMeetingBigStartTime.toJson.toString()),
    PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")),
    Signature(Base64Data("")),
    Hash(Base64Data("")),
    List.empty,
    Some(invalidStateMeetingBigStartTime)
  )

}
