package ch.epfl.pop.pubsub.graph.handlers

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Status}
import akka.pattern.{AskableActorRef, ask}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.objects.DbActorNAckException
import ch.epfl.pop.pubsub.{MessageRegistry, PubSubMediator, PublishSubscribe}
import ch.epfl.pop.pubsub.graph.PipelineError
import ch.epfl.pop.storage.DbActor
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuiteLike as FunSuiteLike
import org.scalatest.matchers.should.Matchers
import util.examples.LaoDataExample
import util.examples.data.{AddChirpMessages, AddReactionMessages, DeleteChirpMessages, DeleteReactionMessages}

import scala.concurrent.duration.FiniteDuration

class SocialMediaHandlerSuite extends TestKit(ActorSystem("SocialMedia-DB-System")) with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll {
  // Implicits for system actors
  implicit val duration: FiniteDuration = FiniteDuration(5, "seconds")
  implicit val timeout: Timeout = Timeout(duration)
  private val mockMed: ActorRef = mockMediator

  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)
  }

  def mockMediator: ActorRef = {
    val mediatorMock = Props(new Actor() {
      override def receive: Receive = {
        case PubSubMediator.Propagate(_, _) =>
          sender() ! PubSubMediator.PropagateAck()

        case _ =>
      }
    })
    system.actorOf(mediatorMock)
  }

  def mockDbWithNack: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case m @ DbActor.WriteAndPropagate(_, _) =>
          system.log.info("Received {}", m)
          system.log.info("Responding with a Nack")

          sender() ! Status.Failure(DbActorNAckException(1, "error"))
        case x =>
          system.log.info(s"Received - error $x")
      }
    })
    system.actorOf(dbActorMock)
  }

  def mockDbWithAck: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case m: DbActor.WriteAndPropagate =>
          system.log.info("Received {}", m)
          system.log.info("Responding with a Ack")

          sender() ! DbActor.DbActorAck()

        case m: DbActor.ReadLaoData =>
          system.log.info("Received {}", m)
          system.log.info("Responding with a Ack")

          sender() ! DbActor.DbActorReadLaoDataAck(LaoDataExample.LAODATA)
      }
    })
    system.actorOf(dbActorMock)
  }

  def mockDbWithAckAndNotifyNAck: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case DbActor.WriteAndPropagate(channel, message) =>
          if (channel == AddChirpMessages.CHANNEL) {
            system.log.info(s"Received WAP on channel $channel")
            system.log.info("Responding with a Ack")

            sender() ! DbActor.DbActorAck()
          } else {
            system.log.info(s"Received WAP on channel $channel")
            system.log.info("Responding with a NAck")

            sender() ! Status.Failure(DbActorNAckException(1, "error"))
          }

        case m: DbActor.ReadLaoData =>
          system.log.info("Received {}", m)
          system.log.info("Responding with a Ack")

          sender() ! DbActor.DbActorReadLaoDataAck(LaoDataExample.LAODATA)
      }
    })
    system.actorOf(dbActorMock)
  }

  def mockDbWithAckButEmptyAckLaoData: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case m: DbActor.WriteAndPropagate =>
          system.log.info("Received {}", m)
          system.log.info("Responding with a Ack")

          sender() ! DbActor.DbActorAck()

        case m: DbActor.ReadLaoData =>
          system.log.info("Received {}", m)
          system.log.info("Responding with a NAck")

          sender() ! Status.Failure(DbActorNAckException(1, "No corresponding lao data (mocked)"))
      }
    })
    system.actorOf(dbActorMock)
  }

  def mockDbWithAckButNAckLaoData: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        // You can modify the following match case to include more args, names...
        case m: DbActor.WriteAndPropagate =>
          system.log.info("Received {}", m)
          system.log.info("Responding with a Ack")

          sender() ! DbActor.DbActorAck()

        case m: DbActor.ReadLaoData =>
          system.log.info("Received {}", m)
          system.log.info("Responding with a NAck")

          sender() ! Status.Failure(DbActorNAckException(1, "error"))
      }
    })
    system.actorOf(dbActorMock)
  }

  private def injectDb(dbRef: AskableActorRef) = PublishSubscribe.buildGraph(mockMed, dbRef, Actor.noSender, MessageRegistry(), Actor.noSender, Actor.noSender, Actor.noSender, false)

  test("AddReaction fails if the database fails storing the message") {
    val mockedDB = mockDbWithNack
    injectDb(mockedDB)
    val request = AddReactionMessages.addReaction

    SocialMediaHandler.handleAddReaction(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("AddReaction succeeds if the database succeeds storing the message") {
    val mockedDB = mockDbWithAck
    injectDb(mockedDB)
    val request = AddReactionMessages.addReaction

    SocialMediaHandler.handleAddReaction(request) should equal(Right(request))

    system.stop(mockedDB.actorRef)
  }

  test("DeleteReaction fails if the database fails storing the message") {
    val mockedDB = mockDbWithNack
    injectDb(mockedDB)
    val request = DeleteReactionMessages.deleteReaction

    SocialMediaHandler.handleDeleteReaction(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("DeleteReaction succeeds if the database succeeds storing the message") {
    val mockedDB = mockDbWithAck
    injectDb(mockedDB)
    val request = DeleteReactionMessages.deleteReaction

    SocialMediaHandler.handleDeleteReaction(request) should equal(Right(request))

    system.stop(mockedDB.actorRef)
  }

  test("AddChirp fails if the database fails storing the message") {
    val mockedDB = mockDbWithNack
    injectDb(mockedDB)
    val request = AddChirpMessages.addChirp

    SocialMediaHandler.handleAddChirp(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("AddChirp fails if the database provides 'None' laoId") {
    val mockedDB = mockDbWithAckButEmptyAckLaoData
    injectDb(mockedDB)
    val request = AddChirpMessages.addChirp

    SocialMediaHandler.handleAddChirp(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("AddChirp fails if the database fails to provide laoId") {
    val mockedDB = mockDbWithAckButNAckLaoData
    injectDb(mockedDB)
    val request = AddChirpMessages.addChirp

    SocialMediaHandler.handleAddChirp(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("AddChirp succeeds if the database succeeds storing the message and managing the notify") {
    val mockedDB = mockDbWithAck
    injectDb(mockedDB)
    val request = AddChirpMessages.addChirp

    SocialMediaHandler.handleAddChirp(request) should equal(Right(request))

    system.stop(mockedDB.actorRef)
  }

  test("DeleteChirp fails if the database fails storing the message") {
    val mockedDB = mockDbWithNack
    injectDb(mockedDB)

    val request = DeleteChirpMessages.deleteChirp

    SocialMediaHandler.handleDeleteChirp(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("DeleteChirp succeeds if the database succeeds storing the message and managing the notify") {
    val mockedDB = mockDbWithAck
    injectDb(mockedDB)
    val request = DeleteChirpMessages.deleteChirp

    SocialMediaHandler.handleDeleteChirp(request) should equal(Right(request))

    system.stop(mockedDB.actorRef)
  }

  test("DeleteChirp fails if the database succeeds storing the message but fails the notify") {
    val mockedDB = mockDbWithAckAndNotifyNAck
    injectDb(mockedDB)
    val request = DeleteChirpMessages.deleteChirp

    SocialMediaHandler.handleDeleteChirp(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("DeleteChirp fails if the database provides 'None' laoId") {
    val mockedDB = mockDbWithAckButEmptyAckLaoData
    injectDb(mockedDB)
    val request = DeleteChirpMessages.deleteChirp

    SocialMediaHandler.handleDeleteChirp(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("DeleteChirp fails if the database fails to provide laoId") {
    val mockedDB = mockDbWithAckButNAckLaoData
    injectDb(mockedDB)
    val request = DeleteChirpMessages.deleteChirp

    SocialMediaHandler.handleDeleteChirp(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

}
