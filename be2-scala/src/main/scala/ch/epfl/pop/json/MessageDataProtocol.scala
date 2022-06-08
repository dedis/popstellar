package ch.epfl.pop.json

import ch.epfl.pop.json.ObjectProtocol._
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.coin._
import ch.epfl.pop.model.network.method.message.data.election.VersionType.VersionType
import ch.epfl.pop.model.network.method.message.data.election._
import ch.epfl.pop.model.network.method.message.data.lao._
import ch.epfl.pop.model.network.method.message.data.meeting._
import ch.epfl.pop.model.network.method.message.data.rollCall._
import ch.epfl.pop.model.network.method.message.data.socialMedia._
import ch.epfl.pop.model.network.method.message.data.witness._
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.model.objects._
import spray.json._

import scala.collection.immutable.ListMap
import scala.util.Try

object MessageDataProtocol extends DefaultJsonProtocol {
  final private val PARAM_OBJECT = "object"
  final private val PARAM_ACTION = "action"

  // ----------------------------------- ENUM FORMATTERS ----------------------------------- //
  implicit object objectTypeFormat extends RootJsonFormat[ObjectType] {
    override def read(json: JsValue): ObjectType = json match {
      case JsString(method) => ObjectType.unapply(method).getOrElse(ObjectType.INVALID)
      case _ => throw new IllegalArgumentException(s"Can't parse json value $json to an ObjectType")
    }

    override def write(obj: ObjectType): JsValue = JsString(obj.toString)
  }

  implicit object actionTypeFormat extends RootJsonFormat[ActionType] {
    override def read(json: JsValue): ActionType = json match {
      case JsString(method) => ActionType.unapply(method).getOrElse(ActionType.INVALID)
      case _ => throw new IllegalArgumentException(s"Can't parse json value $json to an ActionType")
    }

    override def write(obj: ActionType): JsValue = JsString(obj.toString)
  }

  implicit object versionTypeFormat extends RootJsonFormat[VersionType] {
    override def read(json: JsValue): VersionType = json match {
      case JsString(version) => VersionType.unapply(version).getOrElse(VersionType.INVALID)
      case _ => throw new IllegalArgumentException(s"Can't parse json value $json to a VersionType")
    }

    override def write(obj: VersionType): JsValue = JsString(obj.toString)
  }


  // ------------------------------- METADATA UTILITY -------------------------------------- //
  // retrieve information from the header if it is correct.
  //
  // Succeeds if both 'object' and 'action' are present and are strings
  def parseHeader(data: String): Try[(ObjectType, ActionType)] =
    Try {
      data.parseJson.asJsObject.getFields(PARAM_OBJECT, PARAM_ACTION) match {
        case Seq(objectString@JsString(_), actionString@JsString(_)) =>
          (objectString.convertTo[ObjectType], actionString.convertTo[ActionType])
        case _ => throw new IllegalArgumentException("parseHeader: header fields not found")
      }
    }

  // ------------------------------- DATA FORMATTERS UTILITY ------------------------------- //

  // Decorate a JsonFormat to add the header fields when serializing.
  private def annotateHeader[T <: MessageData](fmt: RootJsonFormat[T]): RootJsonFormat[T] =
    new RootJsonFormat[T] {
      val inner = fmt

      override def read(json: JsValue): T = inner.read(json)

      override def write(obj: T): JsValue = inner.write(obj) match {
        case JsObject(fields) => JsObject(fields.updated(PARAM_OBJECT, obj._object.toJson).updated(PARAM_ACTION, obj.action.toJson))
        case _ => throw new IllegalArgumentException("annotateHeader: inner format must produce an object")
      }
    }

  implicit object VoteElectionFormat extends RootJsonFormat[VoteElection] {
    final private val PARAM_ID: String = "id"
    final private val PARAM_QUESTION: String = "question"
    final private val PARAM_VOTE: String = "vote"
    final private val PARAM_WRITE_IN: String = "write_in"

    override def read(json: JsValue): VoteElection = json.asJsObject.getFields(PARAM_ID, PARAM_QUESTION) match {
      case Seq(id@JsString(_), question@JsString(_)) =>

        val voteOpt: Option[Either[Int, Base64Data]] = json.asJsObject.getFields(PARAM_VOTE) match {
          case Seq(JsNumber(value)) => Some(Left(value.intValue))
          case Seq(JsString(s)) => Some(Right(Base64Data(s)))
          case _ => None
        }
        val writeInOpt: Option[String] = json.asJsObject.getFields(PARAM_WRITE_IN) match {
          case Seq(JsString(writeIn)) => Some(writeIn)
          case _ => None
        }

        if (voteOpt.isEmpty && writeInOpt.isEmpty) {
          throw new IllegalArgumentException(
            s"Unable to parse vote election $json to a VoteElection object: '$PARAM_VOTE' and '$PARAM_WRITE_IN' fields are missing or wrongly formatted"
          )
        } else {
          VoteElection(id.convertTo[Hash], question.convertTo[Hash], voteOpt, writeInOpt)
        }

      case _ => throw new IllegalArgumentException(
        s"Unable to parse vote election $json to a VoteElection object: '$PARAM_ID' or '$PARAM_QUESTION' field missing or wrongly formatted"
      )
    }

    override def write(obj: VoteElection): JsValue = {
      var jsObjectContent: ListMap[String, JsValue] = ListMap[String, JsValue](
        PARAM_ID -> obj.id.toJson,
        PARAM_QUESTION -> obj.question.toJson
      )

      // Adding either of the optional values depending on which is defined
      obj.write_in.foreach { w => jsObjectContent += (PARAM_WRITE_IN -> w.toJson) }
      jsObjectContent += (PARAM_VOTE -> obj.vote.toJson)


      JsObject(jsObjectContent)
    }
  }

  implicit val electionQuestionFormat: JsonFormat[ElectionQuestion] = jsonFormat5(ElectionQuestion.apply)
  implicit val electionBallotVotesFormat: JsonFormat[ElectionBallotVotes] = jsonFormat2(ElectionBallotVotes.apply)
  implicit val electionQuestionResultFormat: JsonFormat[ElectionQuestionResult] = jsonFormat2(ElectionQuestionResult.apply)


  // ----------------------------------- DATA FORMATTERS ----------------------------------- //
  /*
   * NOTE : I had to use 'jsonFormat' instead of 'jsonFormatN' (which directly infers both
   * parameter types and parameter names) because of the fact the MessageData subclasses
   * override two val from the MessageData trait. The solution is to explicitly state every
   * argument type and name
   */

  implicit val createLaoFormat: JsonFormat[CreateLao] = annotateHeader(jsonFormat[Hash, String, Timestamp, PublicKey, List[PublicKey], CreateLao](CreateLao.apply, "id", "name", "creation", "organizer", "witnesses"))
  implicit val stateLaoFormat: JsonFormat[StateLao] = annotateHeader(jsonFormat[Hash, String, Timestamp, Timestamp, PublicKey, List[PublicKey], Hash, List[WitnessSignaturePair], StateLao](StateLao.apply, "id", "name", "creation", "last_modified", "organizer", "witnesses", "modification_id", "modification_signatures"))
  implicit val updateLaoFormat: JsonFormat[UpdateLao] = annotateHeader(jsonFormat[Hash, String, Timestamp, List[PublicKey], UpdateLao](UpdateLao.apply, "id", "name", "last_modified", "witnesses"))

  implicit object GreetLaoFormat extends RootJsonFormat[GreetLao] {
    final private val PARAM_LAO: String = "lao"
    final private val PARAM_FRONTEND: String = "frontend"
    final private val PARAM_ADDRESS: String = "address"
    final private val PARAM_PEERS: String = "peers"

    override def read(json: JsValue): GreetLao = {
      val jsonObject: JsObject = json.asJsObject
      jsonObject.getFields(PARAM_LAO, PARAM_FRONTEND, PARAM_ADDRESS, PARAM_PEERS) match {
        case Seq(lao@JsString(_), frontend@JsString(_), JsString(address), JsArray(peers)) =>
          GreetLao(
            lao.convertTo[Hash],
            frontend.convertTo[PublicKey],
            address,
            peers.map(_.convertTo[String]).toList
          )
        case _ => throw new IllegalArgumentException(s"Can't parse json value $json to a GreetLao object")
      }
    }

    override def write(obj: GreetLao): JsValue = {
      var jsObjectContent: ListMap[String, JsValue] = ListMap[String, JsValue](
        PARAM_OBJECT -> JsString(obj._object.toString),
        PARAM_ACTION -> JsString(obj.action.toString),
        PARAM_LAO -> obj.lao.toJson,
        PARAM_FRONTEND -> obj.frontend.toJson,
        PARAM_ADDRESS -> obj.address.toJson,
        PARAM_PEERS -> obj.peers.toJson
      )
      JsObject(jsObjectContent)
    }
  }

  implicit object CreateMeetingFormat extends RootJsonFormat[CreateMeeting] {
    final private val PARAM_ID: String = "id"
    final private val PARAM_NAME: String = "name"
    final private val PARAM_CREATION: String = "creation"
    final private val PARAM_LOCATION: String = "location"
    final private val PARAM_START: String = "start"
    final private val PARAM_END: String = "end"
    final private val PARAM_EXTRA: String = "extra"

    override def read(json: JsValue): CreateMeeting = {
      val jsonObject: JsObject = json.asJsObject
      jsonObject.getFields(PARAM_ID, PARAM_NAME, PARAM_CREATION, PARAM_START) match {
        case Seq(id@JsString(_), JsString(name), creation@JsNumber(_), start@JsNumber(_)) =>
          CreateMeeting(
            id.convertTo[Hash],
            name,
            creation.convertTo[Timestamp],
            jsonObject.getFields(PARAM_LOCATION) match {
              case Seq(JsString(location)) => Some(location)
              case _ => None
            },
            start.convertTo[Timestamp],
            jsonObject.getFields(PARAM_END) match {
              case Seq(end@JsNumber(_)) => Some(end.convertTo[Timestamp])
              case _ => None
            },
            jsonObject.getFields(PARAM_EXTRA) match {
              case _ => None // FIXME todo extra
              //case _ => None
            }
          )
        case _ => throw new IllegalArgumentException(s"Can't parse json value $json to a CreateMeeting object")
      }
    }

    override def write(obj: CreateMeeting): JsValue = {
      var jsObjectContent: ListMap[String, JsValue] = ListMap[String, JsValue](
        PARAM_OBJECT -> JsString(obj._object.toString),
        PARAM_ACTION -> JsString(obj.action.toString),
        PARAM_ID -> obj.id.toJson,
        PARAM_NAME -> obj.name.toJson,
        PARAM_CREATION -> obj.creation.toJson,
        PARAM_START -> obj.start.toJson
      )

      obj.location.foreach { l => jsObjectContent += (PARAM_LOCATION -> l.toJson) }
      obj.end.foreach { e => jsObjectContent += (PARAM_END -> e.toJson) }
      obj.extra.foreach { _ => jsObjectContent += (PARAM_EXTRA -> ???) } // TODO extra

      JsObject(jsObjectContent)
    }
  }

  implicit object StateMeetingFormat extends RootJsonFormat[StateMeeting] {
    final private val PARAM_ID: String = "id"
    final private val PARAM_NAME: String = "name"
    final private val PARAM_CREATION: String = "creation"
    final private val PARAM_LAST_MODIFIED: String = "last_modified"
    final private val PARAM_LOCATION: String = "location"
    final private val PARAM_START: String = "start"
    final private val PARAM_END: String = "end"
    final private val PARAM_EXTRA: String = "extra"
    final private val PARAM_MOD_ID: String = "modification_id"
    final private val PARAM_MOD_SIGNATURES: String = "modification_signatures"

    override def read(json: JsValue): StateMeeting = {
      val jsonObject: JsObject = json.asJsObject
      jsonObject.getFields(PARAM_ID, PARAM_NAME, PARAM_CREATION, PARAM_LAST_MODIFIED, PARAM_START, PARAM_MOD_ID, PARAM_MOD_SIGNATURES) match {
        case Seq(id@JsString(_), JsString(name), creation@JsNumber(_), lastMod@JsNumber(_), start@JsNumber(_), modId@JsString(_), JsArray(modSig)) =>
          StateMeeting(
            id.convertTo[Hash],
            name,
            creation.convertTo[Timestamp],
            lastMod.convertTo[Timestamp],
            jsonObject.getFields(PARAM_LOCATION) match {
              case Seq(JsString(location)) => Some(location)
              case _ => None
            },
            start.convertTo[Timestamp],
            jsonObject.getFields(PARAM_END) match {
              case Seq(end@JsNumber(_)) => Some(end.convertTo[Timestamp])
              case _ => None
            },
            jsonObject.getFields(PARAM_EXTRA) match {
              case _ => None // FIXME todo extra
              //case _ => None
            },
            modId.convertTo[Hash],
            modSig.map(_.convertTo[WitnessSignaturePair]).toList
          )
        case _ => throw new IllegalArgumentException(s"Can't parse json value $json to a StateMeeting object")
      }
    }

    override def write(obj: StateMeeting): JsValue = {
      var jsObjectContent: ListMap[String, JsValue] = ListMap[String, JsValue](
        PARAM_OBJECT -> JsString(obj._object.toString),
        PARAM_ACTION -> JsString(obj.action.toString),
        PARAM_ID -> obj.id.toJson,
        PARAM_NAME -> obj.name.toJson,
        PARAM_CREATION -> obj.creation.toJson,
        PARAM_LAST_MODIFIED -> obj.last_modified.toJson,
        PARAM_START -> obj.start.toJson,
        PARAM_MOD_ID -> obj.modification_id.toJson,
        PARAM_MOD_SIGNATURES -> obj.modification_signatures.toJson
      )

      obj.location.foreach { l => jsObjectContent += (PARAM_LOCATION -> l.toJson) }
      obj.end.foreach { e => jsObjectContent += (PARAM_END -> e.toJson) }
      obj.extra.foreach { _ => jsObjectContent += (PARAM_EXTRA -> ???) } // TODO extra

      JsObject(jsObjectContent)
    }
  }

  implicit val closeRollCallFormat: JsonFormat[CloseRollCall] = annotateHeader(jsonFormat[Hash, Hash, Timestamp, List[PublicKey], CloseRollCall](CloseRollCall.apply, "update_id", "closes", "closed_at", "attendees"))
  implicit val createRollCallFormat: JsonFormat[CreateRollCall] = annotateHeader(jsonFormat[Hash, String, Timestamp, Timestamp, Timestamp, String, Option[String], CreateRollCall](CreateRollCall.apply, "id", "name", "creation", "proposed_start", "proposed_end", "location", "roll_call_description"))
  implicit val openRollCallFormat: JsonFormat[OpenRollCall] = annotateHeader(jsonFormat[Hash, Hash, Timestamp, OpenRollCall](OpenRollCall.apply, "update_id", "opens", "opened_at"))
  implicit val reopenRollCallFormat: JsonFormat[ReopenRollCall] = annotateHeader(jsonFormat[Hash, Hash, Timestamp, ReopenRollCall](ReopenRollCall.apply, "update_id", "opens", "opened_at"))

  implicit val witnessMessageFormat: JsonFormat[WitnessMessage] = annotateHeader(jsonFormat[Hash, Signature, WitnessMessage](WitnessMessage.apply, "message_id", "signature"))

  implicit val castVoteElectionFormat: JsonFormat[CastVoteElection] = annotateHeader(jsonFormat[Hash, Hash, Timestamp, List[VoteElection], CastVoteElection](CastVoteElection.apply, "lao", "election", "created_at", "votes"))
  implicit val setupElectionFormat: JsonFormat[SetupElection] = annotateHeader(jsonFormat[Hash, Hash, String, VersionType, Timestamp, Timestamp, Timestamp, List[ElectionQuestion], SetupElection](SetupElection.apply, "id", "lao", "name", "version", "created_at", "start_time", "end_time", "questions"))
  implicit val resultElectionFormat: JsonFormat[ResultElection] = annotateHeader(jsonFormat[List[ElectionQuestionResult], List[Signature], ResultElection](ResultElection.apply, "questions", "witness_signatures"))
  implicit val endElectionFormat: JsonFormat[EndElection] = annotateHeader(jsonFormat[Hash, Hash, Timestamp, Hash, EndElection](EndElection.apply, "lao", "election", "created_at", "registered_votes"))
  implicit val openElectionFormat: JsonFormat[OpenElection] = jsonFormat[Hash, Hash, Timestamp, OpenElection](OpenElection.apply, "lao", "election", "opened_at")

  implicit object KeyElectionFormat extends JsonFormat[KeyElection] {
    final private val PARAM_ELECTION_ID: String = "election"
    final private val PARAM_ELECTION_KEY: String = "election_key"

    override def read(json: JsValue): KeyElection = json.asJsObject().getFields(PARAM_ELECTION_ID, PARAM_ELECTION_KEY) match {
      case Seq(election@JsString(_), election_key@JsString(_)) => KeyElection(
        election.convertTo[Hash],
        election_key.convertTo[PublicKey]
      )
      case _ => throw new IllegalArgumentException(s"Can't parse json value $json to a KeyElection object")
    }

    override def write(obj: KeyElection): JsValue = {
      var jsObjectContent: ListMap[String, JsValue] = ListMap[String, JsValue](
        PARAM_OBJECT -> JsString(obj._object.toString),
        PARAM_ACTION -> JsString(obj.action.toString),
        PARAM_ELECTION_ID -> obj.election.toJson,
        PARAM_ELECTION_KEY -> obj.election_key.toJson,
      )
      JsObject(jsObjectContent)
    }
  }

  implicit val addChirpFormat: JsonFormat[AddChirp] = annotateHeader(jsonFormat[String, Option[String], Timestamp, AddChirp](AddChirp.apply, "text", "parent_id", "timestamp"))
  implicit val notifyAddChirpFormat: JsonFormat[NotifyAddChirp] = annotateHeader(jsonFormat[Hash, Channel, Timestamp, NotifyAddChirp](NotifyAddChirp.apply, "chirp_id", "channel", "timestamp"))
  implicit val deleteChirpFormat: JsonFormat[DeleteChirp] = annotateHeader(jsonFormat[Hash, Timestamp, DeleteChirp](DeleteChirp.apply, "chirp_id", "timestamp"))
  implicit val notifyDeleteChirpFormat: JsonFormat[NotifyDeleteChirp] = annotateHeader(jsonFormat[Hash, Channel, Timestamp, NotifyDeleteChirp](NotifyDeleteChirp.apply, "chirp_id", "channel", "timestamp"))

  implicit val addReactionFormat: JsonFormat[AddReaction] = annotateHeader(jsonFormat[String, Hash, Timestamp, AddReaction](AddReaction.apply, "reaction_codepoint", "chirp_id", "timestamp"))
  implicit val deleteReactionFormat: JsonFormat[DeleteReaction] = annotateHeader(jsonFormat[Hash, Timestamp, DeleteReaction](DeleteReaction.apply, "reaction_id", "timestamp"))

  implicit val postTransactionFormat: JsonFormat[PostTransaction] = jsonFormat[Transaction, Hash, PostTransaction](PostTransaction.apply, "transaction", "transaction_id")


  implicit object ChannelDataFormat extends JsonFormat[ChannelData] {
    final private val PARAM_CHANNEL_TYPE: String = "channelType"
    final private val PARAM_MESSAGES: String = "messages"

    override def read(json: JsValue): ChannelData = json.asJsObject().getFields(PARAM_CHANNEL_TYPE, PARAM_MESSAGES) match {
      case Seq(channelType@JsString(_), JsArray(messages)) => ChannelData(
        channelType.convertTo[ObjectType],
        messages.map(_.convertTo[Hash]).toList
      )
      case _ => throw new IllegalArgumentException(s"Can't parse json value $json to a ChannelData object")
    }

    override def write(obj: ChannelData): JsValue = JsObject(
      PARAM_CHANNEL_TYPE -> obj.channelType.toJson,
      PARAM_MESSAGES -> obj.messages.toJson
    )

  }

  implicit object RollcallDataFormat extends JsonFormat[RollCallData] {
    final private val PARAM_UPDATE_ID: String = "update_id"
    final private val PARAM_STATE: String = "state"

    override def read(json: JsValue): RollCallData = json.asJsObject().getFields(PARAM_UPDATE_ID, PARAM_STATE) match {
      case Seq(updateId@JsString(_), state@JsString(_)) => RollCallData(
        updateId.convertTo[Hash],
        state.convertTo[ActionType]
      )
      case _ => throw new IllegalArgumentException(s"Can't parse json value $json to a RollcallData object")
    }

    override def write(obj: RollCallData): JsValue = JsObject(
      PARAM_UPDATE_ID -> obj.updateId.toJson,
      PARAM_STATE -> obj.state.toJson
    )
  }

  implicit object ElectionDataFormat extends JsonFormat[ElectionData] {
    final private val PARAM_ELECTION_ID: String = "electionId"
    final private val PARAM_PRIVATE_KEY: String = "privateKey"
    final private val PARAM_PUBLIC_KEY: String = "publicKey"

    override def read(json: JsValue): ElectionData = json.asJsObject().getFields(PARAM_ELECTION_ID, PARAM_PRIVATE_KEY, PARAM_PUBLIC_KEY) match {
      case Seq(electionId@JsString(_), privateKey@JsString(_), publicKey@JsString(_)) => ElectionData(
        electionId.convertTo[Hash],
        KeyPair(privateKey.convertTo[PrivateKey], publicKey.convertTo[PublicKey])
      )
      case _ => throw new IllegalArgumentException(s"Can't parse json value $json to a ChannelData object")
    }

    override def write(obj: ElectionData): JsValue = JsObject(
      PARAM_ELECTION_ID -> obj.electionId.toJson,
      PARAM_PRIVATE_KEY -> obj.keyPair.privateKey.toJson,
      PARAM_PUBLIC_KEY -> obj.keyPair.publicKey.toJson
    )
  }

  implicit object LaoDataFormat extends JsonFormat[LaoData] {
    final private val PARAM_OWNER: String = "owner"
    final private val PARAM_ATTENDEES: String = "attendees"
    final private val PARAM_PRIVATE_KEY: String = "privateKey"
    final private val PARAM_PUBLIC_KEY: String = "publicKey"
    final private val PARAM_WITNESSES: String = "witnesses"

    override def read(json: JsValue): LaoData = json.asJsObject().getFields(PARAM_OWNER, PARAM_ATTENDEES, PARAM_PRIVATE_KEY, PARAM_PUBLIC_KEY, PARAM_WITNESSES) match {
      case Seq(owner@JsString(_), JsArray(attendees), privateKey@JsString(_), publicKey@JsString(_), JsArray(witnesses)) => LaoData(
        owner.convertTo[PublicKey],
        attendees.map(_.convertTo[PublicKey]).toList,
        privateKey.convertTo[PrivateKey],
        publicKey.convertTo[PublicKey],
        witnesses.map(_.convertTo[PublicKey]).toList
      )
      case _ => throw new IllegalArgumentException(s"Can't parse json value $json to a LaoData object")
    }

    override def write(obj: LaoData): JsValue = JsObject(
      PARAM_OWNER -> obj.owner.toJson,
      PARAM_ATTENDEES -> obj.attendees.toJson,
      PARAM_PRIVATE_KEY -> obj.privateKey.toJson,
      PARAM_PUBLIC_KEY -> obj.publicKey.toJson,
      PARAM_WITNESSES -> obj.witnesses.toJson
    )
  }
}
