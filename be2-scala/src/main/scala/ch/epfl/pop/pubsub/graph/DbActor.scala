package ch.epfl.pop.pubsub.graph

import java.io.File
import java.nio.charset.StandardCharsets

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import akka.pattern.AskableActorRef
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Channel, Hash, Signature}
import ch.epfl.pop.pubsub.{AskPatternConstants, PublishSubscribe}
import org.iq80.leveldb.impl.Iq80DBFactory.factory
import org.iq80.leveldb.{DB, DBIterator, Options}

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

object DbActor extends AskPatternConstants {

  final val DATABASE_FOLDER: String = "database"
  final val CHANNELS_FOLDER: String = s"$DATABASE_FOLDER/channels"
  final val DATABASE_MAX_CHANNELS: Int = 1 << 10
  final lazy val INSTANCE: AskableActorRef = PublishSubscribe.getDbActorRef

  // DbActor Events correspond to messages the actor may receive
  sealed trait Event

  /**
   * Request to write a message in the database
   * @param channel the channel where the message must be published
   * @param message the message to write in the database
   */
  final case class Write(channel: Channel, message: Message) extends Event

  /**
   * Request to read a specific message with id <messageId> from <channel>
   * @param channel the channel where the message was published
   * @param messageId the id of the message (message_id) we want to read
   */
  final case class Read(channel: Channel, messageId: Hash) extends Event

  /**
   * Request to read all messages from a specific <channel>
   * @param channel the channel where the messages should be fetched
   */
  final case class Catchup(channel: Channel) extends Event

  /**
   * Request to create channel <channel> in the db
   * @param channel channel to create
   */
  final case class CreateChannel(channel: Channel) extends Event

  /**
   * Request to check if channel <channel> exists in the db
   * @param channel targeted channel
   *
   * Note: db answers with a simple boolean
   */
  final case class ChannelExists(channel: Channel) extends Event

  /**
   * Request to append witness <signature> to a stored message with message_id <messageId>
   * @param messageId message_id of the targeted message
   * @param signature signature to append to the witness signature list of the message
   */
  final case class AddWitnessSignature(messageId: Hash, signature: Signature) extends Event


  // DbActor DbActorMessage correspond to messages the actor may emit
  sealed trait DbActorMessage

  /**
   * Response for a [[Write]] db request
   * Receiving [[DbActorWriteAck]] works as an acknowledgement that the write request was successful
   */
  final case object DbActorWriteAck extends DbActorMessage

  /**
   * Response for a [[Read]] db request
   * Receiving [[DbActorReadAck]] works as an acknowledgement that the read request was successful
   *
   * @param message requested message
   */
  final case class DbActorReadAck(message: Option[Message]) extends DbActorMessage

  /**
   * Response for a [[Catchup]] db request
   * Receiving [[DbActorCatchupAck]] works as an acknowledgement that the catchup request was successful
   *
   * @param messages requested messages
   */
  final case class DbActorCatchupAck(messages: List[Message]) extends DbActorMessage

  /**
   * Response for a general db actor ACK
   */
  final case class DbActorAck() extends DbActorMessage

  /**
   * Response for a negative db request
   *
   * @param code error code corresponding to the error encountered
   * @param description description of the error encountered
   */
  final case class DbActorNAck(code: Int, description: String) extends DbActorMessage


  def getInstance: AskableActorRef = INSTANCE

  /**
   * Creates a new [[DbActor]] which is aware of channels already stored in the db
   *
   * @return the newly created [[DbActor]]
   */
  def apply(): DbActor = {

    val options: Options = new Options()
    options.createIfMissing(true)
    val channelNamesDb: DB = factory.open(new File(CHANNELS_FOLDER), options)

    val iterator: DBIterator = channelNamesDb.iterator
    val initialChannelsMap: mutable.Map[Channel, DB] = mutable.Map.empty

    iterator.seekToFirst()
    options.createIfMissing(false)

    while (iterator.hasNext) {
      // open each db associated with each channel name
      val channelName: String = new String(iterator.next().getKey, StandardCharsets.UTF_8)
      val channelDb: DB = factory.open(new File(channelName), options)

      // store the channel name and its database in the map
      initialChannelsMap += (Channel(channelName.toString.stripPrefix(DATABASE_FOLDER)) -> channelDb)
    }

    iterator.close()
    DbActor(initialChannelsMap.toMap, channelNamesDb)
  }

  sealed case class DbActor(initialChannelsMap: Map[Channel, DB], channelNamesDb: DB) extends Actor with ActorLogging {
    private val channelsMap: mutable.Map[Channel, DB] = initialChannelsMap.to(collection.mutable.Map)

    override def preStart(): Unit = {
      log.info(s"Actor $self (db) was initialised with a total of ${initialChannelsMap.size} recovered channels")
      if (initialChannelsMap.size > DATABASE_MAX_CHANNELS) {
        log.warning(s"Actor $self (db) has surpassed a large number of active lao channels (${initialChannelsMap.size} > $DATABASE_MAX_CHANNELS)")
      }

      super.preStart()
    }

    private def createDatabase(channel: Channel): DB = {
      val options: Options = new Options()
      options.createIfMissing(true)

      val channelName: String = s"$DATABASE_FOLDER$channel"
      val channelDb = factory.open(new File(channelName), options)

      channelNamesDb.put(channelName.getBytes, "".getBytes)
      channelsMap += (channel -> channelDb)

      channelDb
    }

    override def receive: Receive = LoggingReceive {
      case Write(channel, message) =>
        log.info(s"Actor $self (db) received a WRITE request on channel '$channel'")

        val channelDb: DB = channelsMap.get(channel) match {
          case Some(channelDb) => channelDb
          case _ => createDatabase(channel)
        }

        val messageId: Hash = message.message_id
        Try(channelDb.put(messageId.getBytes, message.toJsonString.getBytes)) match {
          case Success(_) =>
            log.info(s"Actor $self (db) wrote message_id '$messageId' on channel '$channel'")
            sender ! DbActorWriteAck
          case Failure(exception) =>
            log.info(s"Actor $self (db) encountered a problem while writing message_id '$messageId' on channel '$channel'")
            sender ! DbActorNAck(ErrorCodes.SERVER_ERROR.id, exception.getMessage)
        }

      case Read(channel, messageId) =>
        log.info(s"Actor $self (db) received a READ request for message_id '$messageId' from channel '$channel'")

        if (channelsMap.contains(channel)) {
          val channelDb: DB = channelsMap(channel)
          Try(channelDb.get(messageId.getBytes)) match {
            case Success(bytes) if bytes != null =>
              sender ! DbActorReadAck(Some(Message.buildFromJson(new String(bytes, StandardCharsets.UTF_8))))
            case _ =>
              sender ! DbActorNAck(ErrorCodes.SERVER_ERROR.id, "Unknown read database error")
          }
        } else {
          sender ! DbActorReadAck(None)
        }

      case Catchup(channel) =>
        log.info(s"Actor $self (db) received a CATCHUP request for channel '$channel'")

        @scala.annotation.tailrec
        def buildCatchupList(iterator: DBIterator, acc: List[Message]): List[Message] = {
          if (iterator.hasNext) {
            val value: Message = Message.buildFromJson(new String(iterator.next().getValue, StandardCharsets.UTF_8))
            buildCatchupList(iterator, value :: acc)
          } else {
            acc
          }
        }

        if (channelsMap.contains(channel)) {
          val iterator: DBIterator = channelsMap(channel).iterator

          iterator.seekToFirst()
          val result: List[Message] = buildCatchupList(iterator, List.empty).reverse
          iterator.close()

          sender ! DbActorCatchupAck(result)

        } else {
          sender ! DbActorNAck(
            ErrorCodes.INVALID_RESOURCE.id,
            "Database cannot catchup from a channel that does not exist in db"
          )
        }

      case CreateChannel(channel) =>
        log.info(s"Actor $self (db) received an CreateChannel request for channel '$channel'")
        channelsMap.get(channel) match {
          case Some(_) => sender ! DbActorNAck(
            ErrorCodes.INVALID_RESOURCE.id,
            s"Database cannot create an already existing channel ($channel)"
          )
          case _ =>
            createDatabase(channel)
            sender ! DbActorAck
        }

      case ChannelExists(channel) =>
        log.info(s"Actor $self (db) received an ChannelExists request for channel '$channel'")
        sender ! channelsMap.contains(channel)

      case AddWitnessSignature(messageId, _) =>
        log.info(s"Actor $self (db) received an AddWitnessSignature request for message_id '$messageId'")
        sender ! DbActorNAck(ErrorCodes.SERVER_ERROR.id, s"NOT IMPLEMENTED: database actor cannot handle AddWitnessSignature requests yet")

      case m@_ =>
        log.info(s"Actor $self (db) received an unknown message")
        sender ! DbActorNAck(ErrorCodes.SERVER_ERROR.id, s"database actor received a message '$m' that it could not recognize")
    }
  }
}
