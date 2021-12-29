package ch.epfl.pop.pubsub.graph.validators

import akka.actor.{Actor,ActorSystem,Props}
import akka.actor.typed.ActorRef
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender,TestKit,TestProbe}
import akka.util.Timeout

import ch.epfl.pop.model.network.{JsonRpcRequest, MethodType}
import ch.epfl.pop.model.network.method.message.Message
import ch.epfl.pop.model.network.method.ParamsWithMessage
import ch.epfl.pop.model.objects.{Base64Data, Channel, LaoData, PrivateKey, PublicKey}
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}

import util.examples.MessageExample

import org.scalatest.{BeforeAndAfterAll,FunSuiteLike,Matchers}

import scala.concurrent.duration.FiniteDuration

import com.google.crypto.tink.subtle.Ed25519Sign

class MessageValidatorSuite extends TestKit(ActorSystem("myTestActorSystem")) with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll {

    // Implicites for system actors
    implicit val duration = FiniteDuration(5 ,"seconds")
    implicit val timeout = Timeout(duration)
    override def afterAll(): Unit = {
        // Stops the testKit
        TestKit.shutdownActorSystem(system)
    }

    private final val KEYPAIR: Ed25519Sign.KeyPair = Ed25519Sign.KeyPair.newKeyPair
    private final val PUBLICKEY: PublicKey = PublicKey(Base64Data.encode(KEYPAIR.getPublicKey))
    private final val PRIVATEKEY: PrivateKey = PrivateKey(Base64Data.encode(KEYPAIR.getPrivateKey))
    private final val PKOWNER: PublicKey = PublicKey(Base64Data.encode("owner"))
    private final val PKATTENDEE: PublicKey = PublicKey(Base64Data.encode("attendee1"))
    private final val PKFALSE: PublicKey = PublicKey(Base64Data.encode("false"))
    private final val CHANNEL: Channel = Channel(Channel.ROOT_CHANNEL_PREFIX + "channel")
    private final val laoData: LaoData = LaoData(PKOWNER, List(PKATTENDEE), PRIVATEKEY, PUBLICKEY, List.empty)

    private final val message: Message = MessageExample.MESSAGE_WORKING_WS_PAIR
    private final val messageFaultyId: Message = MessageExample.MESSAGE_FAULTY_ID
    private final val messageFaultyWSPair: Message = MessageExample.MESSAGE_FAULTY_WS_PAIR
    private final val messageFaultySignature: Message = MessageExample.MESSAGE_FAULTY_SIGNATURE
    private final val rpc: String = "rpc"
    private final val id: Option[Int] = Some(0)
    private final val methodType: MethodType.MethodType = MethodType.PUBLISH
    private final val paramsWithMessage: ParamsWithMessage = new ParamsWithMessage(CHANNEL, message)
    private final val paramsWithFaultyIdMessage: ParamsWithMessage = new ParamsWithMessage(CHANNEL, messageFaultyId)
    private final val paramsWithFaultyWSMessage: ParamsWithMessage = new ParamsWithMessage(CHANNEL, messageFaultyWSPair)
    private final val paramsWithFaultySignatureMessage: ParamsWithMessage = new ParamsWithMessage(CHANNEL, messageFaultySignature)
    private final val validRpc: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithMessage, id)
    private final val invalidIdRpc: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithFaultyIdMessage, id)
    private final val invalidWSPairRpc: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithFaultyWSMessage, id)
    private final val invalidSignatureRpc: JsonRpcRequest = JsonRpcRequest(rpc, methodType, paramsWithFaultySignatureMessage, id)


    private def mockDbNack: AskableActorRef = {
        val mockedDB = Props(new Actor(){
            override def receive = {
                case DbActor.ReadLaoData(channel) =>
                    sender ! DbActor.DbActorNAck(0, "error")
            }
        }
        )
        system.actorOf(mockedDB)
    }

    private def mockDbAckWithNone: AskableActorRef = {
        val mockedDB = Props(new Actor(){
            override def receive = {
                case DbActor.ReadLaoData(channel) =>
                    sender ! DbActor.DbActorReadLaoDataAck(None)
            }
        }
        )
        system.actorOf(mockedDB)
    }

    private def mockDbAckWithSome: AskableActorRef = {
        val mockedDB = Props(new Actor(){
            override def receive = {
                case DbActor.ReadLaoData(channel) =>
                    sender ! DbActor.DbActorReadLaoDataAck(Some(laoData))
            }
        }
        )
        system.actorOf(mockedDB)
    }

    test("validateOwner handles NAck with 'false' output"){
        lazy val dbActorRef = mockDbNack
        MessageValidator.validateOwner(PKOWNER, CHANNEL, dbActorRef) should equal(false)
        system.stop(dbActorRef.actorRef)
    }

    test("validateAttendee handles NAck with 'false' output"){
        lazy val dbActorRef = mockDbNack
        MessageValidator.validateAttendee(PKATTENDEE, CHANNEL, dbActorRef) should equal(false)
        system.stop(dbActorRef.actorRef)
    }

    test("validateOwner handles ReadLaoDataAck containing None with 'false' output"){
        lazy val dbActorRef = mockDbAckWithNone
        MessageValidator.validateOwner(PKOWNER, CHANNEL, dbActorRef) should equal(false)
        system.stop(dbActorRef.actorRef)
    }

    test("validateAttendee handles ReadLaoDataAck containing None with 'false' output"){
        lazy val dbActorRef = mockDbAckWithNone
        MessageValidator.validateAttendee(PKATTENDEE, CHANNEL, dbActorRef) should equal(false)
        system.stop(dbActorRef.actorRef)
    }

    test("validateOwner handles ReadLaoDataAck containing LaoData with corresponding output"){
        lazy val dbActorRef = mockDbAckWithSome
        MessageValidator.validateOwner(PKOWNER, CHANNEL, dbActorRef) should equal(true)
        MessageValidator.validateOwner(PKFALSE, CHANNEL, dbActorRef) should equal(false)
        system.stop(dbActorRef.actorRef)
    }

    test("validateAttendee handles ReadLaoDataAck containing LaoData with corresponding output"){
        lazy val dbActorRef = mockDbAckWithSome
        MessageValidator.validateAttendee(PKATTENDEE, CHANNEL, dbActorRef) should equal(true)
        MessageValidator.validateAttendee(PKFALSE, CHANNEL, dbActorRef) should equal(false)
        system.stop(dbActorRef.actorRef)
    }

    test("validateMessage accepts valid Message example"){
        MessageValidator.validateMessage(validRpc) should be (Left(validRpc))
    }

    test("validateMessage rejects request with invalid id"){
        MessageValidator.validateMessage(invalidIdRpc) shouldBe a [Right[_,PipelineError]]
    }

    test("validateMessage rejects request with invalid WS pair"){
        MessageValidator.validateMessage(invalidWSPairRpc) shouldBe a [Right[_,PipelineError]]
    }

    test("validateMessage rejects request with invalid signature"){
        MessageValidator.validateMessage(invalidSignatureRpc) shouldBe a [Right[_,PipelineError]]
    }    

}