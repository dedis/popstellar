package ch.epfl.pop.pubsub.graph.handlers

import akka.actor.{Actor, ActorSystem, Props, Status}
import akka.pattern.{AskableActorRef, ask}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.objects.{Base64Data, DbActorNAckException, Hash, Signature, WitnessSignaturePair}
import ch.epfl.pop.pubsub.graph.PipelineError
import ch.epfl.pop.storage.DbActor
import ch.epfl.pop.storage.DbActor.DbActorAddWitnessSignatureAck
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}
import org.scalatest.matchers.should.Matchers
import util.examples.data.WitnessMessages
import util.examples.lao.CreateLaoExamples.{SENDER, createLao}

import scala.concurrent.duration.FiniteDuration

class WitnessHandlerTest extends TestKit(ActorSystem("Witness-DB-System")) with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll {
  // Implicits for system actors
  implicit val duration: FiniteDuration = FiniteDuration(5, "seconds")
  implicit val timeout: Timeout = Timeout(duration)

  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)
  }

  final val message: Message = Message(
    data = Base64Data("eyJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUiLCJuYW1lIjoiTEFPIiwiY3JlYXRpb24iOjE2MzMwMzU3MjEsIm9yZ2FuaXplciI6Iko5ZkJ6SlY3MEprNWMtaTMyNzdVcTRDbWVMNHQ1M1dEZlVnaGFLMEhwZU09Iiwid2l0bmVzc2VzIjpbXSwiaWQiOiJwX0VZYkh5TXY2c29wSTVRaEVYQmY0ME1PX2VOb3E3Vl9MeWdCZDRjOVJBPSJ9"),
    sender = SENDER,
    signature = Signature(Base64Data("ONylxgHA9cbsB_lwdfbn3iyzRd4aTpJhBMnvEKhmJF_niE_pUHdmjxDXjEwFyvo5WiH1NZXWyXG27SYEpkasCA==")),
    message_id = Hash(Base64Data("kAG_m4nEQXkguuO_LVphXFE_c_dPoQrHNsb0MvwhXTA=")),
    witness_signatures = List.empty
  )

  final val witnessSignature: Signature = Signature(Base64Data.encode("witnessSignature"))
  final val witnessMessage: Message = message.addWitnessSignature(WitnessSignaturePair(message.sender, witnessSignature))

  def mockDbWithNack: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.WriteAndPropagate(_, _) =>
          system.log.info("Received a message")
          system.log.info("Responding with a Nack")
          sender() ! Status.Failure(DbActorNAckException(1, "error"))

        case DbActor.AddWitnessSignature(_, _, _) =>
          system.log.info("Received a message")
          system.log.info("Responding with a Nack")
          sender() ! Status.Failure(DbActorNAckException(1, "error"))
        case x =>
          system.log.info(s"Received - error $x")
      }
    })
    system.actorOf(dbActorMock, "MockedDB-NACK")
  }

  def mockDbWithAck: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.WriteAndPropagate(_, _) =>
          system.log.info("Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorAck()
        case DbActor.AddWitnessSignature(_, _, _) =>
          system.log.info("Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorAddWitnessSignatureAck(witnessMessage)
        case x =>
          system.log.info(s"Received - error $x")
      }
    })
    system.actorOf(dbActorMock, "MockedDB-ACK")
  }

  def mockDbWithNackAddWitnessSignature: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.WriteAndPropagate(_, _) =>
          system.log.info("Received a message")
          system.log.info("Responding with a Ack")
          sender() ! DbActor.DbActorAck()
        case DbActor.AddWitnessSignature(_, _, _) =>
          system.log.info("Received a message")
          system.log.info("Responding with a Nack")
          sender() ! Status.Failure(DbActorNAckException(1, "error"))
        case x =>
          system.log.info(s"Received - error $x")
      }
    })
    system.actorOf(dbActorMock, "MockedDBAddWitnessSignature-ACK")
  }

  test("WitnessMessage fails if the database fails storing the message") {
    val mockedDB = mockDbWithNack
    val rc = new WitnessHandler(mockedDB)
    val request = WitnessMessages.witnessMessage

    rc.handleWitnessMessage(request) shouldBe an[Right[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }

  test("WitnessMessage succeeds if the database succeeds storing the message") {
    val mockedDB = mockDbWithAck
    val rc = new WitnessHandler(mockedDB)
    val request = WitnessMessages.witnessMessage

    rc.handleWitnessMessage(request) should equal(Left(request))

    system.stop(mockedDB.actorRef)
  }

  test("WitnessMessage fails if the database fails adding a witness signature") {
    val mockedDB = mockDbWithNackAddWitnessSignature
    val rc = new WitnessHandler(mockedDB)
    val request = WitnessMessages.witnessMessage

    rc.handleWitnessMessage(request) shouldBe an[Right[PipelineError, _]]

    system.stop(mockedDB.actorRef)
  }
}
