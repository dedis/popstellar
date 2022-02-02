package ch.epfl.pop.pubsub.graph.validators

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender,TestKit,TestProbe}
import akka.util.Timeout

import ch.epfl.pop.model.objects.{Base64Data, Channel, ChannelData, LaoData, PrivateKey, PublicKey}
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.pubsub.{AskPatternConstants, PubSubMediator}
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}

import util.examples.JsonRpcRequestExample._
import util.examples.socialMedia.AddChirpExamples

import org.scalatest.{BeforeAndAfterAll,FunSuiteLike,Matchers}

import scala.concurrent.duration.FiniteDuration

import scala.reflect.io.Directory
import java.io.File

import java.util.concurrent.TimeUnit

import scala.concurrent.Await

class SocialMediaValidatorSuite extends TestKit(ActorSystem("socialMediaValidatorTestActorSystem"))
    with FunSuiteLike
    with ImplicitSender
    with Matchers with BeforeAndAfterAll with AskPatternConstants {

    final val DB_TEST_FOLDER: String = "databaseSocialMediaTest"

    val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props, "PubSubMediator")
    val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, DB_TEST_FOLDER)), "DbActor")
    /*
    override def afterAll(): Unit = {
        // Stops the test actor system
        TestKit.shutdownActorSystem(system)

        // Deletes the test database
        val directory = new Directory(new File(DB_TEST_FOLDER))
        directory.deleteRecursively()
    }*/

    // Implicit for system actors
    implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)
    override def afterAll(): Unit = {
        // Stops the testKit
        TestKit.shutdownActorSystem(system)

        // Deletes the test database
        val directory = new Directory(new File(DB_TEST_FOLDER))
        directory.deleteRecursively()
    }

    private final val PUBLICKEY: PublicKey = PublicKey(Base64Data("jsNj23IHALvppqV1xQfP71_3IyAHzivxiCz236_zzQc="))
    private final val PRIVATEKEY: PrivateKey = PrivateKey(Base64Data("qRfms3wzSLkxAeBz6UtwA-L1qP0h8D9XI1FSvY68t7Y="))
    private final val PKOWNER: PublicKey = PublicKey(Base64Data.encode("owner"))
    private final val laoDataRight: LaoData = LaoData(PKOWNER, List(AddChirpExamples.SENDER), PRIVATEKEY, PUBLICKEY, List.empty)
    private final val laoDataWrong: LaoData = LaoData(PKOWNER, List(PKOWNER), PRIVATEKEY, PUBLICKEY, List.empty)
    private final val channelDataRight: ChannelData = ChannelData(ObjectType.CHIRP, List.empty)
    private final val channelDataWrong: ChannelData = ChannelData(ObjectType.LAO, List.empty)

    private def mockDbWorking: AskableActorRef = {
        val mockedDB = Props(new Actor(){
            override def receive = {
                case DbActor.ReadLaoData(channel) =>
                    sender ! DbActor.DbActorReadLaoDataAck(Some(laoDataRight))
                case DbActor.ReadChannelData(channel) =>
                    sender ! DbActor.DbActorReadChannelDataAck(Some(channelDataRight))
            }
        }
        )
        system.actorOf(mockedDB)
    }

    private def mockDbWrong: AskableActorRef = {
        val mockedDB = Props(new Actor(){
            override def receive = {
                case DbActor.ReadLaoData(channel) =>
                    sender ! DbActor.DbActorReadLaoDataAck(Some(laoDataWrong))
                case DbActor.ReadChannelData(channel) =>
                    sender ! DbActor.DbActorReadChannelDataAck(Some(channelDataWrong))
            }
        }
        )
        system.actorOf(mockedDB)
    }

    // not functional now due to the need for refactoring the validator with the possibility of introducing an ActorRef
    /*test("Adding a chirp works as intended"){
        val dbActorRef = mockDbWorking
        val message: GraphMessage = SocialMediaValidator.validateAddChirp(ADD_CHIRP_RPC)
        message should equal(Left(ADD_CHIRP_RPC))
        system.stop(dbActorRef.actorRef)
    }

    test("Adding a chirp with too long text fails"){
        val dbActorRef = mockDbWorking
        val message: GraphMessage = SocialMediaValidator.validateAddChirp(ADD_CHIRP_WRONG_TEXT_RPC)
        message shouldBe a [Right[_,PipelineError]]
        system.stop(dbActorRef.actorRef)
    }*/


    

}