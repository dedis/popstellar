package ch.epfl.pop.pubsub

import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.message.data.coin.PostTransaction
import ch.epfl.pop.model.network.method.message.data.election.*
import ch.epfl.pop.model.network.method.message.data.federation.{FederationChallenge, FederationExpect, FederationInit, FederationRequestChallenge, FederationResult}
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, GreetLao, StateLao, UpdateLao}
import ch.epfl.pop.model.network.method.message.data.meeting.{CreateMeeting, StateMeeting}
import ch.epfl.pop.model.network.method.message.data.popcha.Authenticate
import ch.epfl.pop.model.network.method.message.data.rollCall.{CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall}
import ch.epfl.pop.model.network.method.message.data.socialMedia.*
import ch.epfl.pop.model.network.method.message.data.witness.WitnessMessage
import ch.epfl.pop.model.network.method.message.data.{ActionType, MessageData, ObjectType}
import ch.epfl.pop.pubsub.MessageRegistry.RegisterEntry
import ch.epfl.pop.pubsub.graph.SchemaVerifier.createSchemaVerifier
import ch.epfl.pop.pubsub.graph.handlers.*
import ch.epfl.pop.pubsub.graph.validators.*
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
      (ObjectType.lao, ActionType.create),
      createSchemaVerifier("dataCreateLao.json"),
      CreateLao.buildFromJson,
      LaoValidator.validateCreateLao,
      LaoHandler.handleCreateLao
    )
    register.add(
      (ObjectType.lao, ActionType.state),
      createSchemaVerifier("dataStateLao.json"),
      StateLao.buildFromJson,
      LaoValidator.validateStateLao,
      LaoHandler.handleStateLao
    )
    register.add(
      (ObjectType.lao, ActionType.update_properties),
      createSchemaVerifier("dataUpdateLao.json"),
      UpdateLao.buildFromJson,
      LaoValidator.validateUpdateLao,
      LaoHandler.handleUpdateLao
    )
    register.add(
      (ObjectType.lao, ActionType.greet),
      createSchemaVerifier("dataGreetLao.json"),
      GreetLao.buildFromJson,
      LaoValidator.validateGreetLao,
      LaoHandler.handleGreetLao
    )
    // data meeting
    register.add(
      (ObjectType.meeting, ActionType.create),
      createSchemaVerifier("dataCreateMeeting.json"),
      CreateMeeting.buildFromJson,
      MeetingValidator.validateCreateMeeting,
      MeetingHandler.handleCreateMeeting
    )
    register.add(
      (ObjectType.meeting, ActionType.state),
      createSchemaVerifier("dataStateMeeting.json"),
      StateMeeting.buildFromJson,
      MeetingValidator.validateStateMeeting,
      MeetingHandler.handleStateMeeting
    )

    // data roll call
    register.add(
      (ObjectType.roll_call, ActionType.create),
      createSchemaVerifier("dataCreateRollCall.json"),
      CreateRollCall.buildFromJson,
      RollCallValidator.validateCreateRollCall,
      RollCallHandler.handleCreateRollCall
    )
    register.add(
      (ObjectType.roll_call, ActionType.open),
      createSchemaVerifier("dataOpenRollCall.json"),
      OpenRollCall.buildFromJson,
      r => RollCallValidator.validateOpenRollCall(r),
      RollCallHandler.handleOpenRollCall
    )
    register.add(
      (ObjectType.roll_call, ActionType.reopen),
      createSchemaVerifier("dataOpenRollCall.json"),
      ReopenRollCall.buildFromJson,
      RollCallValidator.validateReopenRollCall,
      RollCallHandler.handleReopenRollCall
    )
    register.add(
      (ObjectType.roll_call, ActionType.close),
      createSchemaVerifier("dataCloseRollCall.json"),
      CloseRollCall.buildFromJson,
      RollCallValidator.validateCloseRollCall,
      RollCallHandler.handleCloseRollCall
    )

    // data election
    register.add(
      (ObjectType.election, ActionType.setup),
      createSchemaVerifier("dataSetupElection.json"),
      SetupElection.buildFromJson,
      ElectionValidator.validateSetupElection,
      ElectionHandler.handleSetupElection
    )
    register.add(
      (ObjectType.election, ActionType.open),
      createSchemaVerifier("dataOpenElection.json"),
      OpenElection.buildFromJson,
      ElectionValidator.validateOpenElection,
      ElectionHandler.handleOpenElection
    )
    register.add(
      (ObjectType.election, ActionType.cast_vote),
      createSchemaVerifier("dataCastVote.json"),
      CastVoteElection.buildFromJson,
      ElectionValidator.validateCastVoteElection,
      ElectionHandler.handleCastVoteElection
    )
    register.add(
      (ObjectType.election, ActionType.end),
      createSchemaVerifier("dataEndElection.json"),
      EndElection.buildFromJson,
      ElectionValidator.validateEndElection,
      ElectionHandler.handleEndElection
    )
    register.add(
      (ObjectType.election, ActionType.key),
      createSchemaVerifier("dataKeyElection.json"),
      KeyElection.buildFromJson,
      ElectionValidator.validateKeyElection,
      ElectionHandler.handleKeyElection
    )
    register.add(
      (ObjectType.election, ActionType.result),
      createSchemaVerifier("dataResultElection.json"),
      ResultElection.buildFromJson,
      ElectionValidator.validateResultElection,
      ElectionHandler.handleResultElection
    )

    // data witness
    register.add(
      (ObjectType.message, ActionType.witness),
      createSchemaVerifier("dataWitnessMessage.json"),
      WitnessMessage.buildFromJson,
      WitnessValidator.validateWitnessMessage,
      WitnessHandler.handleWitnessMessage
    )

    // data social media
    register.add(
      (ObjectType.chirp, ActionType.add),
      createSchemaVerifier("dataAddChirp.json"),
      AddChirp.buildFromJson,
      SocialMediaValidator.validateAddChirp,
      SocialMediaHandler.handleAddChirp
    )
    register.add(
      (ObjectType.chirp, ActionType.delete),
      createSchemaVerifier("dataDeleteChirp.json"),
      DeleteChirp.buildFromJson,
      SocialMediaValidator.validateDeleteChirp,
      SocialMediaHandler.handleDeleteChirp
    )
    register.add(
      (ObjectType.chirp, ActionType.notify_add),
      createSchemaVerifier("dataNotifyAddChirp.json"),
      NotifyAddChirp.buildFromJson,
      SocialMediaValidator.validateNotifyAddChirp,
      SocialMediaHandler.handleNotifyAddChirp
    )
    register.add(
      (ObjectType.chirp, ActionType.notify_delete),
      createSchemaVerifier("dataNotifyDeleteChirp.json"),
      NotifyDeleteChirp.buildFromJson,
      SocialMediaValidator.validateNotifyDeleteChirp,
      SocialMediaHandler.handleNotifyDeleteChirp
    )

    register.add(
      (ObjectType.reaction, ActionType.add),
      createSchemaVerifier("dataAddReaction.json"),
      AddReaction.buildFromJson,
      SocialMediaValidator.validateAddReaction,
      SocialMediaHandler.handleAddReaction
    )
    register.add(
      (ObjectType.reaction, ActionType.delete),
      createSchemaVerifier("dataDeleteReaction.json"),
      DeleteReaction.buildFromJson,
      SocialMediaValidator.validateDeleteReaction,
      SocialMediaHandler.handleDeleteReaction
    )

    // data digital cash
    register.add(
      (ObjectType.coin, ActionType.post_transaction),
      createSchemaVerifier("dataPostTransactionCoin.json"),
      PostTransaction.buildFromJson,
      CoinValidator.validatePostTransaction,
      CoinHandler.handlePostTransaction
    )

    // popcha
    register.add(
      (ObjectType.popcha, ActionType.authenticate),
      createSchemaVerifier("dataAuthenticateUser.json"),
      Authenticate.buildFromJson,
      PopchaValidator.validateAuthenticateRequest,
      PopchaHandler.handleAuthentication
    )

    // data federation
    register.add(
      (ObjectType.federation, ActionType.challenge),
      createSchemaVerifier("dataFederationChallenge.json"),
      FederationChallenge.buildFromJson,
      FederationValidator.validateFederationChallenge,
      FederationHandler.handleFederationChallenge
    )

    register.add(
      (ObjectType.federation, ActionType.challenge_request),
      createSchemaVerifier("dataFederationChallengeRequest.json"),
      FederationRequestChallenge.buildFromJson,
      FederationValidator.validateFederationRequestChallenge,
      FederationHandler.handleFederationRequestChallenge
    )

    register.add(
      (ObjectType.federation, ActionType.init),
      createSchemaVerifier("dataFederationInit.json"),
      FederationInit.buildFromJson,
      FederationValidator.validateFederationInit,
      FederationHandler.handleFederationInit
    )

    register.add(
      (ObjectType.federation, ActionType.expect),
      createSchemaVerifier("dataFederationExpect.json"),
      FederationExpect.buildFromJson,
      FederationValidator.validateFederationExpect,
      FederationHandler.handleFederationExpect
    )

    register.add(
      (ObjectType.federation, ActionType.federation_result),
      createSchemaVerifier("dataFederationResult.json"),
      FederationResult.buildFromJson,
      FederationValidator.validateFederationResult,
      FederationHandler.handleFederationResult
    )

    new MessageRegistry(register.get)
  }
}
