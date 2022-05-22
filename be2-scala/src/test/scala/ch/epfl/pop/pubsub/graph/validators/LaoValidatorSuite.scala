package ch.epfl.pop.pubsub.graph.validators

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import ch.epfl.pop.model.network.method.message.data.ObjectType
import ch.epfl.pop.model.objects.{Base64Data, ChannelData, LaoData, PrivateKey, PublicKey}
import ch.epfl.pop.storage.{DbActor, InMemoryStorage}
//import akka.actor.typed.ActorRef
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.pubsub.graph.{GraphMessage, PipelineError}
import ch.epfl.pop.pubsub.{AskPatternConstants, MessageRegistry, PubSubMediator}

//import util.examples.MessageExample._
import java.io.File
import java.util.concurrent.TimeUnit

import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import util.examples.JsonRpcRequestExample._

import scala.reflect.io.Directory

class LaoValidatorSuite extends TestKit(ActorSystem("laoValidatorTestActorSystem"))
  with FunSuiteLike
  with ImplicitSender
  with Matchers with BeforeAndAfterAll with AskPatternConstants {

  implicit val timeout: Timeout = Timeout(1, TimeUnit.SECONDS)

  final val DB_TEST_FOLDER: String = "databaseLaoTest"

  val pubSubMediatorRef: ActorRef = system.actorOf(PubSubMediator.props, "PubSubMediator")
  val dbActorRef: AskableActorRef = system.actorOf(Props(DbActor(pubSubMediatorRef, MessageRegistry(), InMemoryStorage())), "DbActor")

  override def afterAll(): Unit = {
    // Stops the test actor system
    TestKit.shutdownActorSystem(system)

    // Deletes the test database
    val directory = new Directory(new File(DB_TEST_FOLDER))
    directory.deleteRecursively()
  }

  test("LAO creation works as intended") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_RPC)
    message should equal(Left(CREATE_LAO_RPC))
  }

  test("LAO creation fails with wrong channel") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_WRONG_CHANNEL_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO creation fails with stale Timestamp") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_WRONG_TIMESTAMP_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO creation fails with duplicate witnesses") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_WRONG_WITNESSES_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO creation fails with wrong id") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_WRONG_ID_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO creation fails with wrong sender") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_WRONG_SENDER_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO creation fails with empty name") {
    val message: GraphMessage = LaoValidator.validateCreateLao(CREATE_LAO_EMPTY_NAME_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO creation fails without ParamsWithMessage") {
    val message: GraphMessage = LaoValidator.validateCreateLao(RPC_NO_PARAMS)
    message shouldBe a[Right[_, PipelineError]]
  }

  //GreetLao tests
  test("LAO greeting works as intended") {
    val message: GraphMessage = LaoValidator.validateGreetLao(GREET_LAO_RPC)
    message should equal(Left(GREET_LAO_RPC))
  }

  test("LAO greeting fails with wrong lao id") {
    val message: GraphMessage = LaoValidator.validateGreetLao(GREET_LAO_WRONG_LAO_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO greeting fails with wrong frontend") {
    val message: GraphMessage = LaoValidator.validateGreetLao(GREET_LAO_WRONG_FRONTEND_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO greeting fails with wrong address") {
    val message: GraphMessage = LaoValidator.validateGreetLao(GREET_LAO_WRONG_ADDRESS_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO greeting fails with wrong sender") {
    val message: GraphMessage = LaoValidator.validateGreetLao(GREET_LAO_WRONG_SENDER_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO greeting fails with wrong channel") {
    val message: GraphMessage = LaoValidator.validateGreetLao(GREET_LAO_WRONG_CHANNEL_RPC)
    message shouldBe a[Right[_, PipelineError]]
  }

  test("LAO greeting fails without ParamsWithMessage") {
    val message: GraphMessage = LaoValidator.validateCreateLao(RPC_NO_PARAMS)
    message shouldBe a[Right[_, PipelineError]]
  }


private final val sender: PublicKey = PublicKey(Base64Data("J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM="))

  private final val PUBLIC_KEY: PublicKey = PublicKey(Base64Data("jsNj23IHALvppqV1xQfP71_3IyAHzivxiCz236_zzQc="))
  private final val PRIVATE_KEY: PrivateKey = PrivateKey(Base64Data("qRfms3wzSLkxAeBz6UtwA-L1qP0h8D9XI1FSvY68t7Y="))
  private final val PK_OWNER: PublicKey = PublicKey(Base64Data.encode("wrongOwner"))
  private final val laoDataRight: LaoData = LaoData(sender, List(sender), PRIVATE_KEY, PUBLIC_KEY, List.empty)
  private final val laoDataWrong: LaoData = LaoData(PK_OWNER, List(PK_OWNER), PRIVATE_KEY, PUBLIC_KEY, List.empty)
  private final val channelDataRightSetup: ChannelData = ChannelData(ObjectType.LAO, List.empty)
  private final val channelDataWrongSetup: ChannelData = ChannelData(ObjectType.ELECTION, List.empty)

  private final val channelDataRightElection: ChannelData = ChannelData(ObjectType.ELECTION, List.empty)
  private final val channelDataWrongElection: ChannelData =ChannelData(ObjectType.LAO, List.empty)

  private def mockDbWorkingSetup: AskableActorRef = {
    val dbActorMock = Props(new Actor() {
      override def receive: Receive = {
        case DbActor.ReadLaoData(_) =>
          sender() ! DbActor.DbActorReadLaoDataAck(laoDataRight)
        case DbActor.ReadChannelData(_) =>
          sender() ! DbActor.DbActorReadChannelDataAck(channelDataRightSetup)
      }
    })
    system.actorOf(dbActorMock)
  }

  //Create a valid Roll Call
  test("Roll call setup works as intended") {
    val dbActorRef = mockDbWorkingSetup
    println(dbActorRef)
    val rollCallActor: RollCallValidator = new RollCallValidator(dbActorRef)
    val message: GraphMessage = rollCallActor.validateCreateRollCall(CREATE_ROLL_CALL_VALID_RPC)
    message should equal(Left(CREATE_ROLL_CALL_VALID_RPC))
    system.stop(dbActorRef.actorRef)
  }

  test("Roll Call create with wrong sender should fail"){
    val dbActorRef = mockDbWorkingSetup
    println(dbActorRef)
    val rollCallActor: RollCallValidator = new RollCallValidator(dbActorRef)
    val message: GraphMessage = rollCallActor.validateCreateRollCall(CREATE_ROLL_CALL_WRONG_SENDER_RPC)
    val message2: GraphMessage = RollCallValidator.validateCreateRollCall(CREATE_ROLL_CALL_WRONG_SENDER_RPC)
    message shouldBe a[Right[_, PipelineError]]
    message2 shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

  test("Roll Call create with roll call id should fail"){
    val dbActorRef = mockDbWorkingSetup
    println(dbActorRef)
    val rollCallActor: RollCallValidator = new RollCallValidator(dbActorRef)
    val message: GraphMessage = rollCallActor.validateCreateRollCall(CREATE_ROLL_CALL_WRONG_SENDER_RPC)
    val message2: GraphMessage = RollCallValidator.validateCreateRollCall(CREATE_ROLL_CALL_WRONG_SENDER_RPC)
    message shouldBe a[Right[_, PipelineError]]
    message2 shouldBe a[Right[_, PipelineError]]
    system.stop(dbActorRef.actorRef)
  }

}
