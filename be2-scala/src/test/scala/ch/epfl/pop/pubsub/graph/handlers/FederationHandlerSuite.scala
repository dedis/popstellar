package ch.epfl.pop.pubsub.graph.handlers

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Status}
import akka.pattern.{AskableActorRef, ask}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import akka.util.Timeout
import ch.epfl.pop.decentralized.ConnectionMediator
import ch.epfl.pop.model.objects.{Base64Data, DbActorNAckException, PrivateKey, PublicKey}
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.pubsub.PubSubMediator
import ch.epfl.pop.pubsub.graph.PipelineError
import org.scalatest.matchers.should.Matchers
import org.scalatest.funsuite.AnyFunSuiteLike as FunSuiteLike
import util.examples.Federation.FederationExpectExample
import util.examples.data.{FederationChallengeMessages, FederationChallengeRequestMessages, FederationExpectMessages, FederationInitMessages, FederationResultMessages}

import scala.concurrent.duration.FiniteDuration

class FederationHandlerSuite extends TestKit(ActorSystem("Federation-DB-System")) with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll {
  // Implicits for system actors
  implicit val duration: FiniteDuration = FiniteDuration(5, "seconds")
  implicit val timeout: Timeout = Timeout(duration)
  private val mockMed: AskableActorRef = mockMediator
  private val mockConMed: AskableActorRef = mockConnectionMediator

  private final val PUBLIC_KEY: PublicKey = PublicKey(Base64Data("jsNj23IHALvppqV1xQfP71_3IyAHzivxiCz236_zzQc="))
  private final val PRIVATE_KEY: PrivateKey = PrivateKey(Base64Data("qRfms3wzSLkxAeBz6UtwA-L1qP0h8D9XI1FSvY68t7Y="))

  override def afterAll(): Unit = {
    // Stops the testkit
    TestKit.shutdownActorSystem(system)
  }

  def mockMediator: AskableActorRef = {
    val mediatorMock = Props(new Actor() {
      override def receive: Receive = {
        case PubSubMediator.Propagate(_, _) =>
          sender() ! PubSubMediator.PropagateAck()

        case _ =>
      }
    })
    system.actorOf(mediatorMock)
  }

  def mockConnectionMediator: AskableActorRef = {
    val testProbe = TestProbe()
    val testActorRef: ActorRef = testProbe.ref
    val conMediatorMock = Props(new Actor() {
      override def receive: Receive = {
        case ConnectionMediator.GetFederationServer(_) =>
          sender() ! ConnectionMediator.GetFederationServerAck(testActorRef)
        case _ =>
      }

    })
    system.actorOf(conMediatorMock)
  }

  def mockDbWithNAck: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.WriteAndPropagate(_, _) =>
          system.log.info("Received a message ")
          system.log.info("Responding with a Nack")

          sender() ! Status.Failure(DbActorNAckException(1, "error"))

        case DbActor.ReadFederationMessage(_) =>
          system.log.info("Received a ReadFederationMessage")
          system.log.info("Responding with a Nack")

          sender() ! Status.Failure(DbActorNAckException(1, "error"))

        case DbActor.ReadServerPublicKey() | DbActor.ReadServerPrivateKey() =>
          system.log.info("Received a Read key message ")
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
        case DbActor.WriteFederationMessage(_, _) =>
          system.log.info("Received a WriteFederationMessage")
          system.log.info("Responding with an Ack")

          sender() ! DbActor.DbActorAck()

        case DbActor.ReadFederationMessage(_) =>
          system.log.info("Received a ReadFederationMessage")
          system.log.info("Responding with an Ack")

          sender() ! DbActor.DbActorReadFederationMessageAck(Some(FederationExpectExample.EXPECT_MESSAGE))

        case DbActor.DeleteFederationMessage(_) =>
          system.log.info("Received a DeleteFederationMessage")
          system.log.info("Responding with an Ack")

          sender() ! DbActor.DbActorAck

        case DbActor.ReadServerPublicKey() =>
          system.log.info("Received a ReadServerPublicKey")
          system.log.info("Responding with an Ack")

          sender() ! DbActor.DbActorReadServerPublicKeyAck(PUBLIC_KEY)

        case DbActor.ReadServerPrivateKey() =>
          system.log.info("Received a ReadServerPrivateKey")
          system.log.info("Responding with an Ack")

          sender() ! DbActor.DbActorReadServerPrivateKeyAck(PRIVATE_KEY)
      }
    })
    system.actorOf(dbActorMock)
  }

  def mockDbWithNoneReadMessage: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadFederationMessage(_) =>
          system.log.info("Received a ReadFederationMessage")
          system.log.info("Responding with an Ack")

          sender() ! DbActor.DbActorReadFederationMessageAck(None)
      }
    })
    system.actorOf(dbActorMock)
  }

  def mockDbDeleteMessagesFailed: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadFederationMessage(_) =>
          system.log.info("Received a ReadFederationMessage")
          system.log.info("Responding with an Ack")

          sender() ! DbActor.DbActorReadFederationMessageAck(Some(FederationExpectExample.EXPECT_MESSAGE))

        case DbActor.DeleteFederationMessage(_) =>
          system.log.info("Received a DeleteFederationMessage")
          system.log.info("Responding with a Nack")

          sender() ! Status.Failure(DbActorNAckException(1, "error"))

        case DbActor.ReadServerPublicKey() =>
          system.log.info("Received a ReadServerPublicKey")
          system.log.info("Responding with an Ack")

          sender() ! DbActor.DbActorReadServerPublicKeyAck(PUBLIC_KEY)

        case DbActor.ReadServerPrivateKey() =>
          system.log.info("Received a ReadServerPrivateKey")
          system.log.info("Responding with an Ack")

          sender() ! DbActor.DbActorReadServerPrivateKeyAck(PRIVATE_KEY)
      }
    })
    system.actorOf(dbActorMock)
  }

  def mockDbWriteMessagesFailed: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadServerPublicKey() =>
          system.log.info("Received a ReadServerPublicKey")
          system.log.info("Responding with an Ack")

          sender() ! DbActor.DbActorReadServerPublicKeyAck(PUBLIC_KEY)

        case DbActor.ReadServerPrivateKey() =>
          system.log.info("Received a ReadServerPrivateKey")
          system.log.info("Responding with an Ack")

          sender() ! DbActor.DbActorReadServerPrivateKeyAck(PRIVATE_KEY)

        case DbActor.WriteFederationMessage(_, _) =>
          system.log.info("Received a WriteFederationMessage")
          system.log.info("Responding with a Nack")

          sender() ! Status.Failure(DbActorNAckException(1, "error"))
      }
    })
    system.actorOf(dbActorMock)
  }

  test("FederationChallenge should fail if we fail to read federationExpect from the database") {
    val mockedDB = mockDbWithNAck
    val rc = new FederationHandler(mockedDB, mockMed, mockConMed)
    val request = FederationChallengeMessages.federationChallenge

    rc.handleFederationChallenge(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("FederationChallenge should fail if the database provides 'None' federationExpect message ") {
    val mockedDB = mockDbWithNoneReadMessage
    val rc = new FederationHandler(mockedDB, mockMed, mockConMed)
    val request = FederationChallengeMessages.federationChallenge

    rc.handleFederationChallenge(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("FederationChallenge should fail if we fail to delete the challenge and expect messages previously stored in the database") {
    val mockedDB = mockDbDeleteMessagesFailed
    val rc = new FederationHandler(mockedDB, mockMed, mockConMed)
    val request = FederationChallengeMessages.federationChallenge

    rc.handleFederationChallenge(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("FederationChallenge should succeed if we succeed to read federationExpect from the database and delete the messages") {
    val mockedDB = mockDbWithAck
    val rc = new FederationHandler(mockedDB, mockMed, mockConMed)
    val request = FederationChallengeMessages.federationChallenge

    rc.handleFederationChallenge(request) should equal(Right(request))

    system.stop(mockedDB.actorRef)
  }

  test("FederationChallengeRequest should fail if we fail to retrieve the server public and private keys") {
    val mockedDB = mockDbWithNAck
    val rc = new FederationHandler(mockedDB, mockMed, mockConMed)
    val request = FederationChallengeRequestMessages.federationChallengeRequest

    rc.handleFederationChallengeRequest(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("FederationChallengeRequest should fail if we fail to store the challenge in the database") {
    val mockedDB = mockDbWriteMessagesFailed
    val rc = new FederationHandler(mockedDB, mockMed, mockConMed)
    val request = FederationChallengeRequestMessages.federationChallengeRequest

    rc.handleFederationChallengeRequest(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("FederationChallengeRequest should succeed if we succeed to retrieve the keys and write the challenge in the database") {
    val mockedDB = mockDbWithAck
    val rc = new FederationHandler(mockedDB, mockMed, mockConMed)
    val request = FederationChallengeRequestMessages.federationChallengeRequest

    rc.handleFederationChallengeRequest(request) should equal(Right(request))

    system.stop(mockedDB.actorRef)
  }

  test("FederationInit should fail if we fail to store the init message in the database") {
    val mockedDB = mockDbWriteMessagesFailed
    val rc = new FederationHandler(mockedDB, mockMed, mockConMed)
    val request = FederationInitMessages.federationInit

    rc.handleFederationInit(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("FederationInit should succeed if we succeed to store the init message in the database") {
    val mockedDB = mockDbWithAck
    val rc = new FederationHandler(mockedDB, mockMed, mockConMed)
    val request = FederationInitMessages.federationInit

    rc.handleFederationInit(request) should equal(Right(request))

    system.stop(mockedDB.actorRef)
  }

  test("FederationExpect should fail if we fail to store the expect message in the database") {
    val mockedDB = mockDbWriteMessagesFailed
    val rc = new FederationHandler(mockedDB, mockMed, mockConMed)
    val request = FederationExpectMessages.federationExpect

    rc.handleFederationExpect(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("FederationExpect should succeed if we succeed to store the expect message in the database") {
    val mockedDB = mockDbWithAck
    val rc = new FederationHandler(mockedDB, mockMed, mockConMed)
    val request = FederationExpectMessages.federationExpect

    rc.handleFederationExpect(request) should equal(Right(request))

    system.stop(mockedDB.actorRef)
  }

  test("FederationResult should fail if we fail to delete the init message from the database") {
    val mockedDB = mockDbDeleteMessagesFailed
    val rc = new FederationHandler(mockedDB, mockMed, mockConMed)
    val request = FederationResultMessages.federationResult

    rc.handleFederationResult(request) shouldBe an[Left[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("FederationResult should succeed if we succeed to delete the init message from the database") {
    val mockedDB = mockDbWithAck
    val rc = new FederationHandler(mockedDB, mockMed, mockConMed)
    val request = FederationResultMessages.federationResult

    rc.handleFederationResult(request) should equal(Right(request))

    system.stop(mockedDB.actorRef)
  }
}
