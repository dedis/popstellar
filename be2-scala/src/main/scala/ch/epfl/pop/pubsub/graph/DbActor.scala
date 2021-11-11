package ch.epfl.pop.pubsub.graph

import java.io.File
import java.nio.charset.StandardCharsets

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingReceive
import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.network.method.message.data.dataObject._
import ch.epfl.pop.model.objects.{Channel, Hash, Signature}
import ch.epfl.pop.pubsub.{AskPatternConstants, PubSubMediator, PublishSubscribe}
import org.iq80.leveldb.impl.Iq80DBFactory.factory
import org.iq80.leveldb.{DB, DBIterator, Options}

import scala.collection.mutable
import scala.util.{Failure, Success, Try}
import org.slf4j.LoggerFactory
import java.io.IOException

object DbActor extends AskPatternConstants {

  final val DATABASE_MAX_CHANNELS: Int = 1 << 10
  final lazy val INSTANCE: AskableActorRef = PublishSubscribe.getDbActorRef

  // DbActor Events correspond to messages the actor may receive
  sealed trait Event

  /**
   * Request to write a message in the database
   *
   * @param channel the channel where the message should be published
   * @param message the message to write in the database
   */
  final case class Write(channel: Channel, message: Message) extends Event

  /**
   * Request to write a message and a LaoData object in the database
   *
   * @param channel the channel where the message should be published
   * @param message the message to write in the database
   * @param obj the LaoData we write
   */
  final case class WriteLaoData(channel: Channel, message: Message, obj: LaoData) extends Event 

  /** Request to read a specific message with id <messageId> from <channel>
    *
    * @param channel
    *   the channel where the message was published
    * @param messageId
    *   the id of the message (message_id) we want to read
    */
  final case class Read(channel: Channel, messageId: Hash) extends Event

  /** Request to read a specific message with id <messageId> from <channel>
    *
    * @param channel
    *   the channel we need the data for
    */
  final case class ReadChannelData(channel: Channel) extends Event

  /** Request to read all messages from a specific <channel>
    *
    * @param channel
    *   the channel where the messages should be fetched
    */
  final case class Catchup(channel: Channel) extends Event

  /**
   * Request to write a <message> in the database and propagate said message
   * to clients subscribed to the <channel>
   *
   * @param channel the channel where the message should be published
   * @param message the message to write in db and propagate to clients
   *
   * @note DbActor will answer with a [[DbActorWriteAck]] if successful since the propagation cannot fail
   */
  final case class WriteAndPropagate(channel: Channel, message: Message) extends Event

  /**
   * Request to create channel <channel> in the db
   *
   * @param channel channel to create
   */
  final case class CreateChannel(channel: Channel, objectType: ObjectType.ObjectType) extends Event

  /** Request to check if channel <channel> exists in the db
    *
    * @param channel
    *   targeted channel
    * @note
    *   db answers with a simple boolean
    */
  final case class ChannelExists(channel: Channel) extends Event

  /** Request to append witness <signature> to a stored message with message_id
    * <messageId>
    *
    * @param messageId
    *   message_id of the targeted message
    * @param signature
    *   signature to append to the witness signature list of the message
    */
  final case class AddWitnessSignature(messageId: Hash, signature: Signature)
      extends Event

  // DbActor DbActorMessage correspond to messages the actor may emit
  sealed trait DbActorMessage

  /** Response for a [[Write]] db request Receiving [[DbActorWriteAck]] works as
    * an acknowledgement that the write request was successful
    */
  final case object DbActorWriteAck extends DbActorMessage

  /** Response for a [[Read]] db request Receiving [[DbActorReadAck]] works as
    * an acknowledgement that the read request was successful
    *
    * @param message
    *   requested message
    */
  final case class DbActorReadAck(message: Option[Message])
      extends DbActorMessage

  /** Response for a [[ReadChannelData]] db request Receiving [[DbActorReadChannelDataAck]] works as
    * an acknowledgement that the request was successful
    *
    * @param channelData
    *   requested channel data
    */
  final case class DbActorReadChannelDataAck(channelData: Option[ChannelData])
      extends DbActorMessage

  /** Response for a [[Catchup]] db request Receiving [[DbActorCatchupAck]]
    * works as an acknowledgement that the catchup request was successful
    *
    * @param messages
    *   requested messages
    */
  final case class DbActorCatchupAck(messages: List[Message])
      extends DbActorMessage

  /** Response for a general db actor ACK
    */
  final case class DbActorAck() extends DbActorMessage

  /** Response for a negative db request
    *
    * @param code
    *   error code corresponding to the error encountered
    * @param description
    *   description of the error encountered
    */
  final case class DbActorNAck(code: Int, description: String)
      extends DbActorMessage

  def getInstance: AskableActorRef = INSTANCE

  /**
   * Creates a new [[DbActor]] which is aware of channels already stored in the db
   *
   * @param mediatorRef reference pointing towards the pub sub mediator
   * @return the newly created [[DbActor]]
   */
  def apply(mediatorRef: ActorRef, DATABASE_FOLDER: String = "database"): DbActor = {

    val CHANNELS_FOLDER: String = s"$DATABASE_FOLDER/channels"

    /* Create a logger for this DB actor"*/
    val logger = LoggerFactory.getLogger("DBLogger")
    
    val options: Options = new Options()
    options.createIfMissing(true)
    
    val db: DB =
      try { factory.open(new File(DATABASE_FOLDER), options) }
      catch {
        case e: IOException => {
          logger.error("Could not open database folder {}", DATABASE_FOLDER)
          throw e
        }

      }
    DbActor(mediatorRef, db, DATABASE_FOLDER)
  }

  // In the arguments, we might remove the initialChannelsMap and replace the channelNamesDb by the general Db??
  // FIXME: find way to keep count of channel nb
  sealed case class DbActor(mediatorRef: ActorRef, db: DB, DATABASE_FOLDER: String) extends Actor with ActorLogging {
  //sealed case class DbActor(mediatorRef: ActorRef, initialChannelsMap: Map[Channel, DB], channelNamesDb: DB, DATABASE_FOLDER: String, CHANNELS_FOLDER: String) extends Actor with ActorLogging {
    //no need for this soon!!
    //private val channelsMap: mutable.Map[Channel, DB] = initialChannelsMap.to(collection.mutable.Map)
    //however, maybe need to keep count of databases? could be done with a private val channelCount: Int

    override def preStart(): Unit = {
      //FIXME: check if this should be kept
      super.preStart()
    }

    // only for the messages
    private def write(channel: Channel, message: Message): DbActorMessage = {
      val channelKey: String = "laoid/" + channel
      
      Try(db.get(channelKey.getBytes)) match {
        case Success(bytes) if bytes != null =>
          log.info(s"Channel '$channel' already exists.")
        case _ =>
          //FIXME: handle type of message (is it actually a problem or is the MessageExample coded in a weird way?)
          //val objectType: ObjectType.ObjectType = message.decodedData.get._object //or some other way, then match using match case and establish channel type
          val objectType = ObjectType.LAO
          val actualObjectType = if (objectType == ObjectType.ELECTION || objectType == ObjectType.CHIRP){
            objectType
          } else {
            ObjectType.LAO
          }
          createChannel(channel, actualObjectType) match {
            case DbActorAck() =>
            case _ =>
              log.info(s"Actor $self (db) encountered a problem while creating channel '$channel'")
              DbActorNAck(ErrorCodes.SERVER_ERROR.id, s"Error when creating channel '$channel'")
          }
      }
      
      val messageId: Hash = message.message_id
      Try(db.get(channelKey.getBytes)) match {
        case Success(bytes) => {
          if (bytes != null){
            val key: String = "laoid/" + channel + "/" + messageId.toString
            Try(db.createWriteBatch()) match {
              case Success(batch) => {
                val json = new String(bytes, StandardCharsets.UTF_8)
                Try(batch.put(channelKey.getBytes, ChannelData.buildFromJson(json).addMessage(messageId).toJsonString.getBytes)) match {
                  case Success(_) => {
                    Try(batch.put(key.getBytes, message.toJsonString.getBytes)) match {
                      case Success(_) => {
                        //allows writing all data atomically
                        Try(db.write(batch)) match {
                          case Success(_) => 
                            log.info(s"Actor $self (db) wrote object 'objectId' and message_id '$messageId' on channel '$channel'") //change with object and objectId/name/smth like that
                            DbActorWriteAck
                          case Failure(exception) =>
                            log.info(s"Actor $self (db) encountered a problem while writing message_id '$messageId' and object 'objectId' on channel '$channel' because of the batch write")
                            DbActorNAck(ErrorCodes.SERVER_ERROR.id, exception.getMessage)
                        }
                      }
                      case Failure(exception) =>
                        log.info(s"Actor $self (db) encountered a problem while writing message_id '$messageId' and object 'objectId' on channel '$channel' because of a batch put")
                        DbActorNAck(ErrorCodes.SERVER_ERROR.id, exception.getMessage)
                    }
                  }
                  case Failure(exception) =>
                    log.info(s"Actor $self (db) encountered a problem while writing message_id '$messageId' and object 'objectId' on channel '$channel' because of a batch put")
                    DbActorNAck(ErrorCodes.SERVER_ERROR.id, exception.getMessage)
                }
              }
              case Failure(exception) =>
                log.info(s"Actor $self (db) encountered a problem while writing message_id '$messageId' and object 'objectId' on channel '$channel' because of a batch creation")
                DbActorNAck(ErrorCodes.SERVER_ERROR.id, exception.getMessage)
            }
          }
          else{
            log.info(s"Actor $self (db) encountered a problem while writing message_id '$messageId' on channel '$channel', as the channel does not exist")
            DbActorNAck(ErrorCodes.SERVER_ERROR.id, "Could not decode channel data")
          }
        }
        case Failure(exception) =>
          log.info(s"Actor $self (db) encountered a problem while writing message_id '$messageId' on channel '$channel'")
          DbActorNAck(ErrorCodes.SERVER_ERROR.id, exception.getMessage)
      }
    }



    //then, for the other Object (LAOdata etc) storage, we need another function, need laoId for all?????
    //we can get the laoId with an Array[Byte] with decodeSubChannel(channel)
    //need to have a function to store both message and object for atomicity?
    /*private def writeLaoData(channel: Channel, message: Message, obj: LaoData): DbActorMessage = {
      val key: String = "laoid/" + channel //to see better once we know exactly the keys
      val dataKey: String = "laoid/" + channel + "/data"
      val messageKey: String = "laoid/" + channel + "/" + messageId.toString
      //need to do like for message and have JsonString en/decoding
      Try(db.createWriteBatch()) match {
        case Success(batch) => {
          Try(batch.put(dataKey.getBytes, obj.toJsonString.getBytes)) match {
            case Success(_) => {
              Try(batch.put(messageKey.getBytes, message.toJsonString.getBytes)) match {
                case Success(_) => {
                  Try(batch.put(key.getBytes, ChannelData.buildFromJson(json).addMessage(messageId).toJsonString.getBytes)) match {
                    case Success(_) => {
                      //allows writing all data atomically
                      Try(db.write(batch)) match {
                        case Success(_) => 
                          log.info(s"Actor $self (db) wrote object 'objectId' and message_id '$messageId' on channel '$channel'") //change with object and objectId/name/smth like that
                          DbActorWriteAck
                        case Failure(exception) =>
                          log.info(s"Actor $self (db) encountered a problem while writing message_id '$messageId' and object 'objectId' on channel '$channel'")
                          DbActorNAck(ErrorCodes.SERVER_ERROR.id, exception.getMessage)
                      }
                    }
                    case Failure(exception) =>
                      log.info(s"Actor $self (db) encountered a problem while writing message_id '$messageId' and object 'objectId' on channel '$channel'")
                      DbActorNAck(ErrorCodes.SERVER_ERROR.id, exception.getMessage)
                  }
                }
                case Failure(exception) =>
                  log.info(s"Actor $self (db) encountered a problem while writing message_id '$messageId' and object 'objectId' on channel '$channel'")
                  DbActorNAck(ErrorCodes.SERVER_ERROR.id, exception.getMessage)
              }
            }
            case Failure(exception) =>
              log.info(s"Actor $self (db) encountered a problem while writing message_id '$messageId' and object 'objectId' on channel '$channel'")
              DbActorNAck(ErrorCodes.SERVER_ERROR.id, exception.getMessage)
          }
        }
        case Failure(exception) =>
          log.info(s"Actor $self (db) encountered a problem while writing message_id '$messageId' and object 'objectId' on channel '$channel'")
          DbActorNAck(ErrorCodes.SERVER_ERROR.id, exception.getMessage)
      }
      //this is not needed now
      Try(batch.put(key.getBytes, obj.toJsonString.getBytes)) match {
        case Success(_) =>
          log.info(s"Actor $self (db) wrote object 'objectId' on channel '$channel'") //change with object and objectId/name/smth like that
          DbActorWriteAck
        case Failure(exception) =>
          log.info(s"Actor $self (db) encountered a problem while writing object 'objectName' on channel '$channel'")
          DbActorNAck(ErrorCodes.SERVER_ERROR.id, exception.getMessage)
      }
    }*/
    

    private def read(channel: Channel, messageId: Hash): DbActorMessage = {
      val channelKey: String = "laoid/" + channel
      Try(db.get(channelKey.getBytes)) match {
        case Success(bytes) if bytes != null => {
          //val channelDb: DB = channelsMap(channel)
          val key: String = "laoid/" + channel + "/" + messageId.toString
          Try (db.get(key.getBytes)) match {
          //Try(channelDb.get(messageId.getBytes)) match {
            case Success(bytes) if bytes != null =>
              val json = new String(bytes, StandardCharsets.UTF_8)
              DbActorReadAck(Some(Message.buildFromJson(json)))
            case _ =>
              DbActorNAck(ErrorCodes.SERVER_ERROR.id, "Unknown read database error")
          }
        }
        case _ => DbActorReadAck(None)
      }
    }

    private def readChannelData(channel: Channel): DbActorMessage = {
      val channelKey: String = "laoid/" + channel
      Try (db.get(channelKey.getBytes)) match {
        case Success(bytes) if bytes != null =>
          val json = new String(bytes, StandardCharsets.UTF_8)
          DbActorReadChannelDataAck(Some(ChannelData.buildFromJson(json)))
        case _ =>
          DbActorReadChannelDataAck(None)
      }
    }

    //will also need laoId I guess, as for the write above
    private def catchup(channel: Channel): DbActorMessage = {
      val channelKey: String = "laoid/" + channel
      Try(db.get(channelKey.getBytes)) match {
        case Success(bytes) if bytes != null =>
          readChannelData(channel) match {
            case DbActorReadChannelDataAck(Some(data)) =>
              val messageIds: List[Hash] = data.messages //get data as an instance of MessageStorage or smth

              @scala.annotation.tailrec
              def buildCatchupList(msgIds: List[Hash], acc: List[Message]): List[Message] = {
                msgIds match {
                  case Nil => acc
                  case head::Nil => {
                    val key: String = "laoid/" + channel + "/" + head.toString
                    Message.buildFromJson(new String(db.get(key.getBytes), StandardCharsets.UTF_8)) :: acc
                  }
                  case head::tail => {
                    val key: String = "laoid/" + channel + "/" + head.toString
                    buildCatchupList(tail, Message.buildFromJson(new String(db.get(key.getBytes), StandardCharsets.UTF_8)) :: acc)
                  }
                }
              }

              val result: List[Message] = buildCatchupList(messageIds, List.empty)
              DbActorCatchupAck(result)
            case _ =>
              DbActorNAck(ErrorCodes.SERVER_ERROR.id, "Could not access the channel's messages")
          }
        case _ =>
          DbActorNAck(
            ErrorCodes.INVALID_RESOURCE.id,
            "Database cannot catchup from a channel that does not exist in db"
          )
      }
    }

    private def createChannel(channel: Channel, objectType: ObjectType.ObjectType): DbActorMessage = {
      val key: String = "laoid/" + channel
      Try(db.get(key.getBytes)) match {
        case Success(bytes) =>
          if(bytes != null){
            log.info(s"Database cannot create an already existing channel with a value")
            DbActorNAck(ErrorCodes.INVALID_RESOURCE.id, s"Database cannot create an already existing channel ($channel)")
          }
          else{
            Try(db.put(key.getBytes, ChannelData(objectType, List.empty).toJsonString.getBytes)) match {
            case Success(_) => DbActorAck()
            case _ => 
              log.info(s"Error while creating channel in the database")
              DbActorNAck(ErrorCodes.SERVER_ERROR.id, s"Error while creating channel in the database")
          }
          }
        case _ =>
          // create empty? list of messageIds and object type within object
          Try(db.put(key.getBytes, ChannelData(objectType, List.empty).toJsonString.getBytes)) match {
            case Success(_) => DbActorAck()
            case _ =>
              log.info(s"Error while creating channel in the database")
              DbActorNAck(ErrorCodes.SERVER_ERROR.id, s"Error while creating channel in the database")
          }
      }
    }


    private def channelExists(channel: Channel): DbActorMessage = {
      val key:String = "laoid/" + channel
      Try(db.get(key.getBytes)) match {
        case Success(bytes) if bytes != null => DbActorAck()
        case _ => DbActorNAck(ErrorCodes.INVALID_RESOURCE.id, s"Channel '$channel' does not exist in db")
      }
    }


    override def receive: Receive = LoggingReceive {
      case Write(channel, message) =>
        log.info(s"Actor $self (db) received a WRITE request on channel '$channel'")
        sender ! write(channel, message)
      
      
      /*case WriteLaoData(channel, message, laoData) =>
        log.info(s"Actor $self (db) received a WRITELAODATA request on channel '$channel'")
        sender ! writeLaoData(channel, message, laoData)*/
      

      case Read(channel, messageId) =>
        log.info(s"Actor $self (db) received a READ request for message_id '$messageId' from channel '$channel'")
        sender ! read(channel, messageId)
      
      case ReadChannelData(channel) =>
        log.info(s"Actor $self (db) received a READCHANNELDATA request from channel '$channel'")
        sender ! readChannelData(channel)
      

      case Catchup(channel) =>
        log.info(s"Actor $self (db) received a CATCHUP request for channel '$channel'")
        sender ! catchup(channel)

      case WriteAndPropagate(channel, message) =>
        log.info(s"Actor $self (db) received a WriteAndPropagate request on channel '$channel'")
        val answer: DbActorMessage = write(channel, message)
        mediatorRef ! PubSubMediator.Propagate(channel, message)
        sender ! answer

      case CreateChannel(channel, objectType) =>
        log.info(s"Actor $self (db) received an CreateChannel request for channel '$channel' of type '$objectType'")
        sender ! createChannel(channel, objectType)

      case ChannelExists(channel) =>
        log.info(s"Actor $self (db) received an ChannelExists request for channel '$channel'")
        sender ! channelExists(channel)

      case AddWitnessSignature(messageId, _) =>
        log.info(
          s"Actor $self (db) received an AddWitnessSignature request for message_id '$messageId'"
        )
        sender ! DbActorNAck(
          ErrorCodes.SERVER_ERROR.id,
          s"NOT IMPLEMENTED: database actor cannot handle AddWitnessSignature requests yet"
        )

      case m @ _ =>
        log.info(s"Actor $self (db) received an unknown message")
        sender ! DbActorNAck(
          ErrorCodes.SERVER_ERROR.id,
          s"database actor received a message '$m' that it could not recognize"
        )
    }
  }

}
