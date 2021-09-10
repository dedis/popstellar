package ch.epfl.pop.pubsub.graph

import java.util.concurrent.TimeUnit
import java.io.File

import akka.actor.{Actor, ActorLogging}
import akka.event.LoggingReceive
import akka.pattern.AskableActorRef
import akka.util.Timeout
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Channel, Hash}
import ch.epfl.pop.pubsub.PublishSubscribe
import org.iq80.leveldb.impl.Iq80DBFactory.factory
import org.iq80.leveldb.{DB, Options}

import scala.collection.mutable
import scala.concurrent.duration.{Duration, FiniteDuration}

object DbActor {

  final val DATABASE_FOLDER: String = "database"
  final val DURATION: FiniteDuration = Duration(1, TimeUnit.SECONDS)
  final val TIMEOUT: Timeout = Timeout(DURATION)
  final lazy val INSTANCE: AskableActorRef = PublishSubscribe.getDbActorRef

  sealed trait Event

  /**
   * Request to write a message in the database
   * @param channel the channel where the message must be published
   * @param message the message to write in the database
   */
  final case class Write(channel: Channel, message: Message) extends Event

  /**
   * Request to read a specific messages on a channel
   * @param channel the channel where the message was published
   * @param id the id of the message we want to read
   */
  final case class Read(channel: Channel, id: Hash) extends Event


  sealed trait DbActorMessage

  final case object DbActorWriteAck extends DbActorMessage
  final case object DbActorReadAck extends DbActorMessage
  final case class DbActorNAck(code: Int, description: String) extends DbActorMessage


  def getInstance: AskableActorRef = INSTANCE

  def getTimeout: Timeout = TIMEOUT

  def getDuration: FiniteDuration = DURATION

  def apply(): DbActor = DbActor()

  sealed case class DbActor() extends Actor with ActorLogging {
    private val channelsMap: mutable.Map[Channel, DB] = mutable.Map.empty

    override def receive: Receive = LoggingReceive {
      case Write(channel, message) =>
        log.info(s"Actor $self (db) received a WRITE request on channel '${channel.toString}'")
        val channelDb: DB = channelsMap.get(channel) match {
          case Some(channelDb) => channelDb
          case _ =>
            val options = new Options()
            options.createIfMissing(true)
            val channelDb = factory.open(new File(s"$DATABASE_FOLDER/${channel.toString}"), options)
            channelsMap += (channel -> channelDb)
            channelDb
        }

        val messageId: Hash = message.message_id
        channelDb.put(messageId.getBytes, "TODO message into bytes?".getBytes)

        log.info(s"Actor $self (db) wrote message_id '$messageId' on channel '$channel'")
        sender ! DbActorWriteAck

      case Read(channel, _) =>
        log.info(s"Actor $self (db) received a READ request on channel '$channel'")
        // TODO READ FROM DB
        sender ! DbActorReadAck
      case m@_ =>
        log.info(s"Actor $self (db) received an unknown message")
        sender ! DbActorNAck(ErrorCodes.SERVER_ERROR.id, s"database actor received a message '$m' that it could not recognize")
    }
  }
}
