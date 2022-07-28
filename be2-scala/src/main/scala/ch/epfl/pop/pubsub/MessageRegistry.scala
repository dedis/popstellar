package ch.epfl.pop.pubsub

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.election.{CastVoteElection, EndElection, KeyElection, OpenElection, ResultElection, SetupElection}
import ch.epfl.pop.model.network.method.message.data.coin.PostTransaction
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, GreetLao, StateLao, UpdateLao}
import ch.epfl.pop.model.network.method.message.data.meeting.{CreateMeeting, StateMeeting}
import ch.epfl.pop.model.network.method.message.data.rollCall.{CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall}
import ch.epfl.pop.model.network.method.message.data.socialMedia._
import ch.epfl.pop.model.network.method.message.data.witness.WitnessMessage
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.pubsub.MessageRegistry.RegisterEntry
import ch.epfl.pop.pubsub.graph.SchemaVerifier.createSchemaVerifier
import ch.epfl.pop.pubsub.graph.handlers._
import ch.epfl.pop.pubsub.graph.validators._
import ch.epfl.pop.pubsub.graph.{GraphMessage, JsonString}

import scala.util.Try

final class MessageRegistry(private val register: Map[(ObjectType, ActionType), RegisterEntry]) {
  def getSchemaVerifier(_object: ObjectType, action: ActionType): Option[JsonString => Try[Unit]] = register.get((_object, action)).map(_.schemaVerifier)
  def getBuilder(_object: ObjectType, action: ActionType): Option[JsonString => MessageData] = register.get((_object, action)).map(_.builder)
  def getValidator(_object: ObjectType, action: ActionType): Option[JsonRpcRequest => GraphMessage] = register.get((_object, action)).map(_.validator)
  def getHandler(_object: ObjectType, action: ActionType): Option[JsonRpcRequest => GraphMessage] = register.get((_object, action)).map(_.handler)
}

object MessageRegistry {
  final case class RegisterEntry(
      schemaVerifier: JsonString => Try[Unit],
      builder: JsonString => MessageData,
      validator: JsonRpcRequest => GraphMessage,
      handler: JsonRpcRequest => GraphMessage
  )

  private final class Register {
    private val elements = collection.immutable.Map.newBuilder[(ObjectType, ActionType), RegisterEntry]

    def add(
        key: (ObjectType, ActionType),
        schemaVerifier: JsonString => Try[Unit],
        builder: JsonString => MessageData,
        validator: JsonRpcRequest => GraphMessage,
        handler: JsonRpcRequest => GraphMessage
    ): Unit = {
      elements += (key -> RegisterEntry(schemaVerifier, builder, validator, handler))
    }

    def get: Map[(ObjectType, ActionType), RegisterEntry] = elements.result()
  }

  def apply(): MessageRegistry = {
    val register: Register = new Register()

    // data lao
    register.add(
      (ObjectType.LAO, ActionType.CREATE),
      createSchemaVerifier("dataCreateLao.json"),
      CreateLao.buildFromJson,
      LaoValidator.validateCreateLao,
      LaoHandler.handleCreateLao
    )
    register.add(
      (ObjectType.LAO, ActionType.STATE),
      createSchemaVerifier("dataStateLao.json"),
      StateLao.buildFromJson,
      LaoValidator.validateStateLao,
      LaoHandler.handleStateLao
    )
    register.add(
      (ObjectType.LAO, ActionType.UPDATE_PROPERTIES),
      createSchemaVerifier("dataUpdateLao.json"),
      UpdateLao.buildFromJson,
      LaoValidator.validateUpdateLao,
      LaoHandler.handleUpdateLao
    )
    register.add(
      (ObjectType.LAO, ActionType.GREET),
      createSchemaVerifier("dataGreetLao.json"),
      GreetLao.buildFromJson,
      LaoValidator.validateGreetLao,
      LaoHandler.handleGreetLao
    )
    // data meeting
    register.add(
      (ObjectType.MEETING, ActionType.CREATE),
      createSchemaVerifier("dataCreateMeeting.json"),
      CreateMeeting.buildFromJson,
      MeetingValidator.validateCreateMeeting,
      MeetingHandler.handleCreateMeeting
    )
    register.add(
      (ObjectType.MEETING, ActionType.STATE),
      createSchemaVerifier("dataStateMeeting.json"),
      StateMeeting.buildFromJson,
      MeetingValidator.validateStateMeeting,
      MeetingHandler.handleStateMeeting
    )

    // data roll call
    register.add(
      (ObjectType.ROLL_CALL, ActionType.CREATE),
      createSchemaVerifier("dataCreateRollCall.json"),
      CreateRollCall.buildFromJson,
      RollCallValidator.validateCreateRollCall,
      RollCallHandler.handleCreateRollCall
    )
    register.add(
      (ObjectType.ROLL_CALL, ActionType.OPEN),
      createSchemaVerifier("dataOpenRollCall.json"),
      OpenRollCall.buildFromJson,
      r => RollCallValidator.validateOpenRollCall(r),
      RollCallHandler.handleOpenRollCall
    )
    register.add(
      (ObjectType.ROLL_CALL, ActionType.REOPEN),
      createSchemaVerifier("dataOpenRollCall.json"),
      ReopenRollCall.buildFromJson,
      RollCallValidator.validateReopenRollCall,
      RollCallHandler.handleReopenRollCall
    )
    register.add(
      (ObjectType.ROLL_CALL, ActionType.CLOSE),
      createSchemaVerifier("dataCloseRollCall.json"),
      CloseRollCall.buildFromJson,
      RollCallValidator.validateCloseRollCall,
      RollCallHandler.handleCloseRollCall
    )

    // data election
    register.add(
      (ObjectType.ELECTION, ActionType.SETUP),
      createSchemaVerifier("dataSetupElection.json"),
      SetupElection.buildFromJson,
      ElectionValidator.validateSetupElection,
      ElectionHandler.handleSetupElection
    )
    register.add(
      (ObjectType.ELECTION, ActionType.OPEN),
      createSchemaVerifier("dataOpenElection.json"),
      OpenElection.buildFromJson,
      ElectionValidator.validateOpenElection,
      ElectionHandler.handleOpenElection
    )
    register.add(
      (ObjectType.ELECTION, ActionType.CAST_VOTE),
      createSchemaVerifier("dataCastVote.json"),
      CastVoteElection.buildFromJson,
      ElectionValidator.validateCastVoteElection,
      ElectionHandler.handleCastVoteElection
    )
    register.add(
      (ObjectType.ELECTION, ActionType.END),
      createSchemaVerifier("dataEndElection.json"),
      EndElection.buildFromJson,
      ElectionValidator.validateEndElection,
      ElectionHandler.handleEndElection
    )
    register.add(
      (ObjectType.ELECTION, ActionType.KEY),
      createSchemaVerifier("dataKeyElection.json"),
      KeyElection.buildFromJson,
      ElectionValidator.validateKeyElection,
      ElectionHandler.handleKeyElection
    )

    // data witness
    register.add(
      (ObjectType.MESSAGE, ActionType.WITNESS),
      createSchemaVerifier("dataWitnessMessage.json"),
      WitnessMessage.buildFromJson,
      WitnessValidator.validateWitnessMessage,
      WitnessHandler.handleWitnessMessage
    )

    // data social media
    register.add(
      (ObjectType.CHIRP, ActionType.ADD),
      createSchemaVerifier("dataAddChirp.json"),
      AddChirp.buildFromJson,
      SocialMediaValidator.validateAddChirp,
      SocialMediaHandler.handleAddChirp
    )
    register.add(
      (ObjectType.CHIRP, ActionType.DELETE),
      createSchemaVerifier("dataDeleteChirp.json"),
      DeleteChirp.buildFromJson,
      SocialMediaValidator.validateDeleteChirp,
      SocialMediaHandler.handleDeleteChirp
    )
    register.add(
      (ObjectType.CHIRP, ActionType.NOTIFY_ADD),
      createSchemaVerifier("dataNotifyAddChirp.json"),
      NotifyAddChirp.buildFromJson,
      SocialMediaValidator.validateNotifyAddChirp,
      SocialMediaHandler.handleNotifyAddChirp
    )
    register.add(
      (ObjectType.CHIRP, ActionType.NOTIFY_DELETE),
      createSchemaVerifier("dataNotifyDeleteChirp.json"),
      NotifyDeleteChirp.buildFromJson,
      SocialMediaValidator.validateNotifyDeleteChirp,
      SocialMediaHandler.handleNotifyDeleteChirp
    )

    register.add(
      (ObjectType.REACTION, ActionType.ADD),
      createSchemaVerifier("dataAddReaction.json"),
      AddReaction.buildFromJson,
      SocialMediaValidator.validateAddReaction,
      SocialMediaHandler.handleAddReaction
    )
    register.add(
      (ObjectType.REACTION, ActionType.DELETE),
      createSchemaVerifier("dataDeleteReaction.json"),
      DeleteReaction.buildFromJson,
      SocialMediaValidator.validateDeleteReaction,
      SocialMediaHandler.handleDeleteReaction
    )

    // data digital cash
    register.add(
      (ObjectType.COIN, ActionType.POST_TRANSACTION),
      createSchemaVerifier("dataPostTransactionCoin.json"),
      PostTransaction.buildFromJson,
      CoinValidator.validatePostTransaction,
      CoinHandler.handlePostTransaction
    )

    new MessageRegistry(register.get)
  }
}
