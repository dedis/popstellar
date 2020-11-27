package ch.epfl.pop

import java.io.File
import java.nio.charset.StandardCharsets

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.stream.scaladsl.{Sink, Source}
import ch.epfl.pop.json.JsonMessages._
import ch.epfl.pop.json._
import ch.epfl.pop.json.JsonMessageParser.serializeMessage
import org.iq80.leveldb.impl.Iq80DBFactory.factory
import org.iq80.leveldb.{DB, Options}


/**
 * The role of a DBActor is to serve write and read requests to the database.
 */
object DBActor {

  sealed trait DBMessage

  /**
   * Request to write a message in the database
   * @param message the message to write in the database
   * @param rid the id of the client request
   * @param replyTo the actor to respond to
   */
  final case class Write(message: MessageParameters, rid : Int, replyTo: ActorRef[JsonMessageAnswerServer]) extends DBMessage

  final case class Catchup(channel: String, rid: Int, replyTo: ActorRef[JsonMessageAnswerServer]) extends DBMessage

  /**
   * Create an actor handling the database
   * @param path the path of the database
   * @return an actor handling the database
   */
  def apply(path: String, pubEntry: Sink[PropagateMessageServer, NotUsed]): Behavior[DBMessage] = database(path, Map.empty, pubEntry)

  private def database(path : String, channelsDB: Map[String, DB], pubEntry: Sink[PropagateMessageServer, NotUsed]): Behavior[DBMessage] = Behaviors.receive { (ctx, message) =>
     message match {

      case Write(params, rid, replyTo) =>
        val channel = params.channel
        val (newChannelsDB, db) = channelsDB.get(channel) match {
          case Some(db) => (channelsDB, db)
          case None =>
            val options = new Options()
            options.createIfMissing(true)
            val db = factory.open(new File(path + "/" + channel), options)
            (channelsDB + (channel -> db), db)
        }



        val message: MessageContent = params.message.get
        val id = message.message_id
        db.put(id, serializeMessage(message).getBytes)

        val propagate = PropagateMessageServer(params)
        implicit val system: ActorSystem[Nothing] = ctx.system

        Source.single(propagate).runWith(pubEntry)
        replyTo ! AnswerResultIntMessageServer(id = rid)

        database(path, newChannelsDB, pubEntry)

      case Catchup(channel, rid, replyTo) =>
        if(channelsDB.contains(channel)) {
          val db = channelsDB(channel)
          val it = db.iterator()
          var messages : List[ChannelMessage] = Nil
          while(it.hasNext) {
            val message: Array[Byte] = it.next().getValue
            ctx.log.debug(new String(message, StandardCharsets.UTF_8))
            val messageParsed: ChannelMessage = JsonMessageParser.parseChannelMessage(message)
            messages =  messageParsed :: messages
          }
          replyTo ! AnswerResultArrayMessageServer(result = ChannelMessages(messages.reverse), id = rid)

        }
        else {
          ctx.log.debug("Error invalid channel " + channel + "on catchup.")
          val error = MessageErrorContent(-2, "Invalid resource: channel " + channel + " does not exist.")
          replyTo ! AnswerErrorMessageServer(error = error, id = rid)
        }
        Behaviors.same
     }
  }

}
