package ch.epfl.pop.pubsub.graph.validators

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
//import akka.actor.typed.ActorRef
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender,TestKit,TestProbe}
import akka.util.Timeout

import ch.epfl.pop.model.objects.{Base64Data, Channel, ChannelData, LaoData, PrivateKey, PublicKey}
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.pubsub.{AskPatternConstants, PubSubMediator}
import ch.epfl.pop.pubsub.graph.{DbActor, ErrorCodes, GraphMessage, PipelineError}

//import util.examples.MessageExample._
import util.examples.JsonRpcRequestExample._

import org.scalatest.{BeforeAndAfterAll,FunSuiteLike,Matchers}

import scala.concurrent.duration.FiniteDuration

import scala.reflect.io.Directory
import java.io.File

import java.util.concurrent.TimeUnit

import scala.concurrent.Await

class RollCallValidatorSuite extends TestKit(ActorSystem("rollCallValidatorTestActorSystem"))
    with FunSuiteLike
    with ImplicitSender
    with Matchers with BeforeAndAfterAll with AskPatternConstants {

    implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)

    final val DB_TEST_FOLDER: String = "databaseRollCallTest"
    //final val DB_TEST_CHANNEL: String = "/root/testChannel"

    val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props, "PubSubMediator")
    val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, DB_TEST_FOLDER)), "DbActor")

    override def afterAll(): Unit = {
        // Stops the test actor system
        TestKit.shutdownActorSystem(system)

        // Deletes the test database
        val directory = new Directory(new File(DB_TEST_FOLDER))
        directory.deleteRecursively()
    }

    test("Roll Call creation works as intended"){

    }

}