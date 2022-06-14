package ch.epfl.pop.pubsub.graph.handlers

import akka.actor.{Actor, ActorSystem, Props, Status}
import akka.pattern.AskableActorRef
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import ch.epfl.pop.model.objects.DbActorNAckException
import ch.epfl.pop.pubsub.graph.PipelineError
import ch.epfl.pop.storage.DbActor
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import util.examples.LaoDataExample
import util.examples.data.PostTransactionMessages._

import scala.concurrent.duration.FiniteDuration


class CoinHandlerSuite extends TestKit(ActorSystem("SocialMedia-DB-System")) with FunSuiteLike with ImplicitSender with Matchers with BeforeAndAfterAll {
  // Implicits for system actors
  implicit val duration: FiniteDuration = FiniteDuration(5, "seconds")
  implicit val timeout: Timeout = Timeout(duration)


  override def afterAll(): Unit = {
    // Stops the testKit
    TestKit.shutdownActorSystem(system)
  }

  test("PostTransaction is handled with no-op") {
    val rc = CoinHandler
    val request = postTransaction

    rc.handlePostTransaction(request) should equal (Left(request))
  }

}
