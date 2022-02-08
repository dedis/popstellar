package ch.epfl.pop.json

import ch.epfl.pop.json.ObjectProtocol._
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.election._
import ch.epfl.pop.model.network.method.message.data.lao._
import ch.epfl.pop.model.network.method.message.data.meeting._
import ch.epfl.pop.model.network.method.message.data.rollCall._
import ch.epfl.pop.model.network.method.message.data.socialMedia._
import ch.epfl.pop.model.network.method.message.data.witness._
import ch.epfl.pop.model.network.method.message.data.{ActionType, ObjectType}
import ch.epfl.pop.model.objects._
import spray.json._

import scala.collection.immutable.ListMap

object MessageDataProtocol extends DefaultJsonProtocol {

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


  // ------------------------------- DATA FORMATTERS UTILITY ------------------------------- //

  implicit object VoteElectionFormat extends RootJsonFormat[VoteElection] {
    final private val PARAM_ID: String = "id"
    final private val PARAM_QUESTION: String = "question"
    final private val PARAM_VOTE: String = "vote"
    final private val PARAM_WRITE_IN: String = "write_in"

    override def read(json: JsValue): VoteElection = json.asJsObject.getFields(PARAM_ID, PARAM_QUESTION) match {
      case Seq(id@JsString(_), question@JsString(_)) =>

        val voteOpt: Option[List[Int]] = json.asJsObject.getFields(PARAM_VOTE) match {
          case Seq(JsArray(vote)) => Some(vote.map(_.convertTo[Int]).toList)
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

      if (obj.isWriteIn) {
        jsObjectContent += (PARAM_WRITE_IN -> obj.write_in.get.toJson)
      } else {
        jsObjectContent += (PARAM_VOTE -> obj.vote.get.toJson)
      }

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

  implicit val createLaoFormat: JsonFormat[CreateLao] = jsonFormat[Hash, String, Timestamp, PublicKey, List[PublicKey], CreateLao](CreateLao.apply, "id", "name", "creation", "organizer", "witnesses")
  implicit val stateLaoFormat: JsonFormat[StateLao] = jsonFormat[Hash, String, Timestamp, Timestamp, PublicKey, List[PublicKey], Hash, List[WitnessSignaturePair], StateLao](StateLao.apply, "id", "name", "creation", "last_modified", "organizer", "witnesses", "modification_id", "modification_signatures")
  implicit val updateLaoFormat: JsonFormat[UpdateLao] = jsonFormat[Hash, String, Timestamp, List[PublicKey], UpdateLao](UpdateLao.apply, "id", "name", "last_modified", "witnesses")

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
        "object" -> JsString(obj._object.toString),
        "action" -> JsString(obj.action.toString),
        PARAM_ID -> obj.id.toJson,
        PARAM_NAME -> obj.name.toJson,
        PARAM_CREATION -> obj.creation.toJson,
        PARAM_START -> obj.start.toJson
      )

      if (obj.location.isDefined) jsObjectContent += (PARAM_LOCATION -> obj.location.get.toJson)
      if (obj.end.isDefined) jsObjectContent += (PARAM_END -> obj.end.get.toJson)
      if (obj.extra.isDefined) jsObjectContent += (PARAM_EXTRA -> ???) // FIXME extra

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
        "object" -> JsString(obj._object.toString),
        "action" -> JsString(obj.action.toString),
        PARAM_ID -> obj.id.toJson,
        PARAM_NAME -> obj.name.toJson,
        PARAM_CREATION -> obj.creation.toJson,
        PARAM_LAST_MODIFIED -> obj.last_modified.toJson,
        PARAM_START -> obj.start.toJson,
        PARAM_MOD_ID -> obj.modification_id.toJson,
        PARAM_MOD_SIGNATURES -> obj.modification_signatures.toJson
      )

      if (obj.location.isDefined) jsObjectContent += (PARAM_LOCATION -> obj.location.get.toJson)
      if (obj.end.isDefined) jsObjectContent += (PARAM_END -> obj.end.get.toJson)
      if (obj.extra.isDefined) jsObjectContent += (PARAM_EXTRA -> ???) // FIXME extra

      JsObject(jsObjectContent)
    }
  }

  implicit val closeRollCallFormat: JsonFormat[CloseRollCall] = jsonFormat[Hash, Hash, Timestamp, List[PublicKey], CloseRollCall](CloseRollCall.apply, "update_id", "closes", "closed_at", "attendees")
  implicit val createRollCallFormat: JsonFormat[CreateRollCall] = jsonFormat[Hash, String, Timestamp, Timestamp, Timestamp, String, Option[String], CreateRollCall](CreateRollCall.apply, "id", "name", "creation", "proposed_start", "proposed_end", "location", "roll_call_description")
  implicit val openRollCallFormat: JsonFormat[OpenRollCall] = jsonFormat[Hash, Hash, Timestamp, OpenRollCall](OpenRollCall.apply, "update_id", "opens", "opened_at")
  implicit val reopenRollCallFormat: JsonFormat[ReopenRollCall] = jsonFormat[Hash, Hash, Timestamp, ReopenRollCall](ReopenRollCall.apply, "update_id", "opens", "opened_at")

  implicit val witnessMessageFormat: JsonFormat[WitnessMessage] = jsonFormat[Hash, Signature, WitnessMessage](WitnessMessage.apply, "message_id", "signature")

  implicit val castVoteElectionFormat: JsonFormat[CastVoteElection] = jsonFormat[Hash, Hash, Timestamp, List[VoteElection], CastVoteElection](CastVoteElection.apply, "lao", "election", "created_at", "votes")
  implicit val setupElectionFormat: JsonFormat[SetupElection] = jsonFormat[Hash, Hash, String, String, Timestamp, Timestamp, Timestamp, List[ElectionQuestion], SetupElection](SetupElection.apply, "id", "lao", "name", "version", "created_at", "start_time", "end_time", "questions")
  implicit val resultElectionFormat: JsonFormat[ResultElection] = jsonFormat[List[ElectionQuestionResult], List[Signature], ResultElection](ResultElection.apply, "questions", "witness_signatures")
  implicit val endElectionFormat: JsonFormat[EndElection] = jsonFormat[Hash, Hash, Timestamp, Hash, EndElection](EndElection.apply, "lao", "election", "created_at", "registered_votes")

  implicit val addChirpFormat: JsonFormat[AddChirp] = jsonFormat[String, Option[String], Timestamp, AddChirp](AddChirp.apply, "text", "parent_id", "timestamp")
  implicit val notifyAddChirpFormat: JsonFormat[NotifyAddChirp] = jsonFormat[Hash, Channel, Timestamp, NotifyAddChirp](NotifyAddChirp.apply, "chirp_id", "channel", "timestamp")
  implicit val deleteChirpFormat: JsonFormat[DeleteChirp] = jsonFormat[Hash, Timestamp, DeleteChirp](DeleteChirp.apply, "chirp_id", "timestamp")
  implicit val notifyDeleteChirpFormat: JsonFormat[NotifyDeleteChirp] = jsonFormat[Hash, Channel, Timestamp, NotifyDeleteChirp](NotifyDeleteChirp.apply, "chirp_id", "channel", "timestamp")

  implicit val addReactionFormat: JsonFormat[AddReaction] = jsonFormat[String, Hash, Timestamp, AddReaction](AddReaction.apply, "reaction_codepoint", "chirp_id", "timestamp")
  implicit val deleteReactionFormat: JsonFormat[DeleteReaction] = jsonFormat[Hash, Timestamp, DeleteReaction](DeleteReaction.apply, "reaction_id", "timestamp")

  implicit object ChannelDataFormat extends JsonFormat[ChannelData] {
    final private val PARAM_CHANNELTYPE: String = "channelType"
    final private val PARAM_MESSAGES: String = "messages"

    override def read(json: JsValue): ChannelData = json.asJsObject().getFields(PARAM_CHANNELTYPE, PARAM_MESSAGES) match {
      case Seq(channelType@JsString(_), JsArray(messages)) => ChannelData(
        channelType.convertTo[ObjectType],
        messages.map(_.convertTo[Hash]).toList
      )
      case _ => throw new IllegalArgumentException(s"Can't parse json value $json to a ChannelData object")
    }

    override def write(obj: ChannelData): JsValue = JsObject(
      PARAM_CHANNELTYPE -> obj.channelType.toJson,
      PARAM_MESSAGES -> obj.messages.toJson
    )

  }

  implicit object LaoDataFormat extends JsonFormat[LaoData] {
    final private val PARAM_OWNER: String = "owner"
    final private val PARAM_ATTENDEES: String = "attendees"
    final private val PARAM_PRIVATEKEY: String = "privateKey"
    final private val PARAM_PUBLICKEY: String = "publicKey"
    final private val PARAM_WITNESSES: String = "witnesses"

    override def read(json: JsValue): LaoData = json.asJsObject().getFields(PARAM_OWNER, PARAM_ATTENDEES, PARAM_PRIVATEKEY, PARAM_PUBLICKEY, PARAM_WITNESSES) match {
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
      PARAM_PRIVATEKEY -> obj.privateKey.toJson,
      PARAM_PUBLICKEY -> obj.publicKey.toJson,
      PARAM_WITNESSES -> obj.witnesses.toJson
    )

  }


}
