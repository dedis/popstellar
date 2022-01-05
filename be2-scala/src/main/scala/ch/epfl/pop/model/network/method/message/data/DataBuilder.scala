package ch.epfl.pop.model.network.method.message.data

import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.election.{CastVoteElection, EndElection, ResultElection, SetupElection}
import ch.epfl.pop.model.network.method.message.data.lao.{CreateLao, StateLao, UpdateLao}
import ch.epfl.pop.model.network.method.message.data.meeting.{CreateMeeting, StateMeeting}
import ch.epfl.pop.model.network.method.message.data.rollCall.{CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall}
import ch.epfl.pop.model.network.method.message.data.witness.WitnessMessage
import ch.epfl.pop.model.network.method.message.data.socialMedia.{AddChirp, AddBroadcastChirp}
import spray.json._
import scala.util.{Try,Success,Failure}

/**Companion object of DataBuilder that helps building MessageData instances**/
object DataBuilder {
    val dataBuilder = DataBuilder(DataRegistryModule.REGISTRY)
    def buildData(_object: ObjectType, action: ActionType, payload: String): MessageData = dataBuilder.buildData(_object, action, payload)
}

/**
  * Builds and parses message data or rejects if it's json schema is incorrect
  * @param REGISTRY: registry that contains metadata about MessageData builders/parsers
  */
sealed case class DataBuilder(final val REGISTRY: DataRegistry) {
  /**
   * Builds a MessageData from its headers ('object' and 'action' fields) and its json representation
   *
   * @param _object 'object' field of the message data
   * @param action  'action' field of the message data
   * @param payload json string representation of the message data
   * @throws ch.epfl.pop.model.network.method.message.data.ProtocolException if the tuple (_object, action) doesn't make sense with respect to the protocol
   * @return
   */
  @throws(classOf[ProtocolException])
  def buildData(_object: ObjectType, action: ActionType, payload: String): MessageData = {
    val metadata = REGISTRY.getMetaData(_object, action)
    buildOrReject(payload)(metadata.schemaValidator)(metadata.buildFromJson)(metadata.errMessage)
  }

  /**
    * Builds a message payload after passing a schema validation check
    *
    * @param payload payload to build
    * @param validator one of the validators at [[DataSchemaValidator]] to valid the schema of the payload
    * @param buildFromJson the data builder
    * @param errMsg error message to include in description in case of error
    * @return built MessageData or throws an exceptition in case of schema failure
    */
  @throws(classOf[ProtocolException])
  private def buildOrReject(payload: String)(validator: String => Try[Unit])(buildFromJson: String => MessageData )(errMsg: String): MessageData = {
      validator(payload) match {
        case Success(_) => buildFromJson(payload)
        case Failure(e) => throw new ProtocolException(s"$errMsg: ${e.getMessage}")
      }
  }
}
