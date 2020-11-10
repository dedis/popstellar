package ch.epfl.pop

import java.io.File

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import ch.epfl.pop.json.JsonMessages.{AnswerMessageServer, FetchChannelServer, JsonMessage, NotifyChannelServer}
import org.iq80.leveldb.{DB, Options}
import org.iq80.leveldb.impl.Iq80DBFactory.factory

/**
 * The role of a DBActor is to serve write and read requests to the database.
 */
object DBActor {

  sealed trait DBMessage

  final case class Write(channel: String, id: String, event: String, replyTo: ActorRef[NotifyChannelServer]) extends DBMessage

  final case class Read(channel: String, id: String, replyTo: ActorRef[JsonMessage]) extends DBMessage

  /**
   * Create an actor handling the database
   * @param path the path of the database
   * @return an actor handling the database
   */
  def apply(path: String): Behavior[DBMessage] = database(path, Map.empty)

  private def database(path : String, channelsDB: Map[String, DB]): Behavior[DBMessage] = Behaviors.receiveMessage { message: DBMessage =>
     message match {

      case Write(channel, id, event, replyTo) =>
        val (newChannelsDB, db) = channelsDB.get(channel) match {
          case Some(db) => (channelsDB, db)
          case None =>
            val options = new Options()
            options.createIfMissing(true)
            val db = factory.open(new File(path + "/" + channel), options)
            (channelsDB + (channel -> db), db)
        }
        db.put(id.getBytes(), event.getBytes())
        replyTo ! NotifyChannelServer(channel, id)

        database(path, newChannelsDB)

      case Read(channel, id, replyTo) =>
        if(channelsDB.contains(channel)) {
          val db = channelsDB(channel)
          val event = db.get(id.getBytes())
          val answer =
            if(event == null) {
            AnswerMessageServer(false, Some("Event does not exist"))
          }
          else {
            FetchChannelServer(channel, id, new String(event))
          }
          replyTo ! answer
        }
        else {
          replyTo ! AnswerMessageServer(false, Some("Event does not exist"))
        }
        Behaviors.same
     }
  }

}
