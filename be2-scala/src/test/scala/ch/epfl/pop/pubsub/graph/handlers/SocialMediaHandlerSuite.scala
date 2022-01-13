package ch.epfl.pop.pubsub.graph.handlers

import akka.actor.{Actor, ActorSystem, Props}
import akka.actor.typed.ActorRef
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout

import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import scala.concurrent.duration.FiniteDuration

import ch.epfl.pop.pubsub.graph.{DbActor, PipelineError}
import ch.epfl.pop.pubsub.graph.validators.RpcValidator
import ch.epfl.pop.model.network.requests.socialMedia._
import ch.epfl.pop.model.network.MethodType
import ch.epfl.pop.model.network.method.ParamsWithMessage
import ch.epfl.pop.model.objects.{Base64Data, Channel, LaoData, PublicKey, PrivateKey}

import util.examples.JsonRpcRequestExample
import util.examples.data.{AddReactionMessages, DeleteReactionMessages, AddChirpMessages, DeleteChirpMessages}


class SocialMediaHandlerSuite extends TestKit(ActorSystem("SocialMedia-DB-System")) with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll {
    // Implicites for system actors
    implicit val duration = FiniteDuration(5 ,"seconds")
    implicit val timeout  = Timeout(duration)

    private final val PUBLICKEY: PublicKey = PublicKey(Base64Data("jsNj23IHALvppqV1xQfP71_3IyAHzivxiCz236_zzQc="))
    private final val PRIVATEKEY: PrivateKey = PrivateKey(Base64Data("qRfms3wzSLkxAeBz6UtwA-L1qP0h8D9XI1FSvY68t7Y="))
    private final val PKOWNER: PublicKey = PublicKey(Base64Data.encode("owner"))
    private final val PKATTENDEE: PublicKey = PublicKey(Base64Data.encode("attendee1"))
    private final val laoData: LaoData = LaoData(PKOWNER, List(PKATTENDEE), PRIVATEKEY, PUBLICKEY, List.empty)

    override def afterAll(): Unit = {
        // Stops the testKit
        TestKit.shutdownActorSystem(system)
    }

    def mockDbWithNack: AskableActorRef = {
        val mockedDB = Props(new Actor(){
            override def receive = {
                // You can modify the following match case to include more args, names...
                case m : DbActor.WriteAndPropagate =>
                    system.log.info("Received {}", m)
                    system.log.info("Responding with a Nack")

                    sender ! DbActor.DbActorNAck(1, "error")
            }
            }
        )
        system.actorOf(mockedDB, "MockedDB-NACK")
    }

    def mockDbWithAck: AskableActorRef = {
        val mockedDB = Props(new Actor(){
            override def receive = {
                // You can modify the following match case to include more args, names...
                case m : DbActor.WriteAndPropagate =>
                    system.log.info("Received {}", m)
                    system.log.info("Responding with a Ack")

                    sender ! DbActor.DbActorWriteAck()

                case m : DbActor.ReadLaoData =>
                    system.log.info("Received {}", m)
                    system.log.info("Responding with a Ack")

                    sender ! DbActor.DbActorReadLaoDataAck(Some(laoData))
            }
            }
        )
        system.actorOf(mockedDB, "MockedDB-ACK")
    }

    def mockDbWithAckAndNotifyNAck: AskableActorRef = {
        val mockedDB = Props(new Actor(){
            override def receive = {
                // You can modify the following match case to include more args, names...
                case DbActor.WriteAndPropagate(channel, message) =>
                    if(channel == AddChirpMessages.CHANNEL){
                        system.log.info(s"Received WAP on channel $channel")
                        system.log.info("Responding with a Ack")

                        sender ! DbActor.DbActorWriteAck()
                    }
                    else{
                        system.log.info(s"Received WAP on channel $channel")
                        system.log.info("Responding with a NAck")

                        sender ! DbActor.DbActorNAck(1, "error")
                    }

                case m : DbActor.ReadLaoData =>
                    system.log.info("Received {}", m)
                    system.log.info("Responding with a Ack")

                    sender ! DbActor.DbActorReadLaoDataAck(Some(laoData))
            }
            }
        )
        system.actorOf(mockedDB, "MockedDB-ACK-NAck-on-Notify")
    }

    def mockDbWithAckButEmptyAckLaoData: AskableActorRef = {
        val mockedDB = Props(new Actor(){
            override def receive = {
                // You can modify the following match case to include more args, names...
                case m : DbActor.WriteAndPropagate =>
                    system.log.info("Received {}", m)
                    system.log.info("Responding with a Ack")

                    sender ! DbActor.DbActorWriteAck()
                
                case m: DbActor.ReadLaoData =>
                    system.log.info("Received {}", m)
                    system.log.info("Responding with a NAck")

                    sender ! DbActor.DbActorReadLaoDataAck(None)
            }
            }
        )
        system.actorOf(mockedDB, "MockedDB-ACK-EmptyAckLaoData")
    }

    def mockDbWithAckButNAckLaoData: AskableActorRef = {
        val mockedDB = Props(new Actor(){
            override def receive = {
                // You can modify the following match case to include more args, names...
                case m : DbActor.WriteAndPropagate =>
                    system.log.info("Received {}", m)
                    system.log.info("Responding with a Ack")

                    sender ! DbActor.DbActorWriteAck()
                
                case m: DbActor.ReadLaoData =>
                    system.log.info("Received {}", m)
                    system.log.info("Responding with a NAck")

                    sender ! DbActor.DbActorNAck(1, "error")
            }
            }
        )
        system.actorOf(mockedDB, "MockedDB-ACK-NAckLaoData")
    }

    test("AddReaction fails if the database fails storing the message"){
        val mockedDB = mockDbWithNack
        val rc = new SocialMediaHandler(mockedDB)
        val request = AddReactionMessages.addReaction

        rc.handleAddReaction(request) shouldBe an [Right[PipelineError,_]]

        system.stop(mockedDB.actorRef)
    }

    test("AddReaction succeeds if the database succeeds storing the message"){
        val mockedDB = mockDbWithAck
        val rc = new SocialMediaHandler(mockedDB)
        val request = AddReactionMessages.addReaction

        rc.handleAddReaction(request) should equal (Left(request))

        system.stop(mockedDB.actorRef)
    }

    test("DeleteReaction fails if the database fails storing the message"){
        val mockedDB = mockDbWithNack
        val rc = new SocialMediaHandler(mockedDB)
        val request = DeleteReactionMessages.deleteReaction

        rc.handleDeleteReaction(request) shouldBe an [Right[PipelineError,_]]

        system.stop(mockedDB.actorRef)
    }

    test("DeleteReaction succeeds if the database succeeds storing the message"){
        val mockedDB = mockDbWithAck
        val rc = new SocialMediaHandler(mockedDB)
        val request = DeleteReactionMessages.deleteReaction

        rc.handleDeleteReaction(request) should equal (Left(request))

        system.stop(mockedDB.actorRef)
    }

    test("AddChirp fails if the database fails storing the message"){
        val mockedDB = mockDbWithNack
        val rc = new SocialMediaHandler(mockedDB)
        val request = AddChirpMessages.addChirp

        rc.handleAddChirp(request) shouldBe an [Right[PipelineError,_]]

        system.stop(mockedDB.actorRef)
    }

    test("AddChirp fails if the database provides 'None' laoId"){
        val mockedDB = mockDbWithAckButEmptyAckLaoData
        val rc = new SocialMediaHandler(mockedDB)
        val request = AddChirpMessages.addChirp

        rc.handleAddChirp(request) shouldBe an [Right[PipelineError,_]]

        system.stop(mockedDB.actorRef)
    }

    test("AddChirp fails if the database fails to provide laoId"){
        val mockedDB = mockDbWithAckButNAckLaoData
        val rc = new SocialMediaHandler(mockedDB)
        val request = AddChirpMessages.addChirp

        rc.handleAddChirp(request) shouldBe an [Right[PipelineError,_]]

        system.stop(mockedDB.actorRef)
    }

    test("AddChirp succeeds if the database succeeds storing the message and managing the notify"){
        val mockedDB = mockDbWithAck
        val rc = new SocialMediaHandler(mockedDB)
        val request = AddChirpMessages.addChirp

        rc.handleAddChirp(request) should equal (Left(request))

        system.stop(mockedDB.actorRef)
    }

    test("AddChirp fails if the database succeeds storing the message and fails the notify"){
        val mockedDB = mockDbWithAckAndNotifyNAck
        val rc = new SocialMediaHandler(mockedDB)
        val request = AddChirpMessages.addChirp

        rc.handleAddChirp(request) shouldBe an [Right[PipelineError,_]]

        system.stop(mockedDB.actorRef)
    }

    test("DeleteChirp fails if the database fails storing the message"){
        val mockedDB = mockDbWithNack
        val rc = new SocialMediaHandler(mockedDB)
        val request = DeleteChirpMessages.deleteChirp

        rc.handleDeleteChirp(request) shouldBe an [Right[PipelineError,_]]

        system.stop(mockedDB.actorRef)
    }

    test("DeleteChirp succeeds if the database succeeds storing the message and managing the notify"){
        val mockedDB = mockDbWithAck
        val rc = new SocialMediaHandler(mockedDB)
        val request = DeleteChirpMessages.deleteChirp

        rc.handleDeleteChirp(request) should equal (Left(request))

        system.stop(mockedDB.actorRef)
    }

    test("DeleteChirp fails if the database succeeds storing the message but fails the notify"){
        val mockedDB = mockDbWithAckAndNotifyNAck
        val rc = new SocialMediaHandler(mockedDB)
        val request = DeleteChirpMessages.deleteChirp

        rc.handleDeleteChirp(request) shouldBe an [Right[PipelineError,_]]

        system.stop(mockedDB.actorRef)
    }

    test("DeleteChirp fails if the database provides 'None' laoId"){
        val mockedDB = mockDbWithAckButEmptyAckLaoData
        val rc = new SocialMediaHandler(mockedDB)
        val request = DeleteChirpMessages.deleteChirp

        rc.handleDeleteChirp(request) shouldBe an [Right[PipelineError,_]]

        system.stop(mockedDB.actorRef)
    }

    test("DeleteChirp fails if the database fails to provide laoId"){
        val mockedDB = mockDbWithAckButNAckLaoData
        val rc = new SocialMediaHandler(mockedDB)
        val request = DeleteChirpMessages.deleteChirp

        rc.handleDeleteChirp(request) shouldBe an [Right[PipelineError,_]]

        system.stop(mockedDB.actorRef)
    }

}
