package ch.epfl.pop

import java.io.File
import java.nio.charset.StandardCharsets

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.scaladsl.{Sink, Source}
import ch.epfl.pop.json.JsonMessages.{AnswerErrorMessageServer, AnswerResultArrayMessageServer, AnswerResultIntMessageServer, JsonMessageAnswerServer, PropagateMessageClient}
import ch.epfl.pop.json.{ChannelMessage, ChannelMessages, MessageContent, MessageErrorContent, MessageParameters, Methods}
import org.iq80.leveldb.{DB, Options}
import org.iq80.leveldb.impl.Iq80DBFactory.factory

/**
 * The role of a DBActor is to serve write and read requests to the database.
 */
object DBActor {

  sealed trait DBMessage

  /**
   * Request to write a message in the database
   * @param channel the channel the message was published to
   * @param id the id of the message
   * @param message the message to write in the database
   * @param rid the id of the client request
   * @param replyTo the actor to respond to
   */
  final case class Write(channel: String, id: String, message: MessageContent, rid : Int, replyTo: ActorRef[JsonMessageAnswerServer]) extends DBMessage

  final case class Catchup(channel: String, rid: Int, replyTo: ActorRef[JsonMessageAnswerServer]) extends DBMessage

  /**
   * Create an actor handling the database
   * @param path the path of the database
   * @return an actor handling the database
   */
  def apply(path: String, pubEntry: Sink[PropagateMessageClient, NotUsed]): Behavior[DBMessage] = database(path, Map.empty, pubEntry)

  private def database(path : String, channelsDB: Map[String, DB], pubEntry: Sink[PropagateMessageClient, NotUsed]): Behavior[DBMessage] = Behaviors.receive { (ctx, message) =>
     message match {

      case Write(channel, id, message, rid, replyTo) =>
        val (newChannelsDB, db) = channelsDB.get(channel) match {
          case Some(db) => (channelsDB, db)
          case None =>
            val options = new Options()
            options.createIfMissing(true)
            val db = factory.open(new File(path + "/" + channel), options)
            (channelsDB + (channel -> db), db)
        }
        db.put(id.getBytes(), message.getBytes()) //TODO: serialize message in Json

        val method = Methods.Message
        val params = MessageParameters(channel, Some(message))
        val propagate = PropagateMessageClient(method = method, params = params)
        implicit val system = ctx.system
        Source.single(propagate).runWith(pubEntry)
        replyTo ! AnswerResultIntMessageServer(id = rid)

        database(path, newChannelsDB, pubEntry)

      case Catchup(channel, rid, replyTo) =>
        if(channelsDB.contains(channel)) {
          val db = channelsDB(channel)
          val it = db.iterator()
          var messages : List[ChannelMessage] = Nil
          while(it.hasNext) {
            val message = it.next().getValue
            messages = new String(message, StandardCharsets.UTF_8) :: messages
          }
          AnswerResultArrayMessageServer(result = ChannelMessages(messages), id = rid)

        }
        else {
          val error = MessageErrorContent(-2, "Invalid resource: channel " + channel + " does not exist.")
          replyTo ! AnswerErrorMessageServer(error = error, id = rid)
        }
        Behaviors.same
     }
  }

}
