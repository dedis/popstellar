package ch.epfl.pop.decentralized

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.funsuite.{AnyFunSuiteLike => FunSuiteLike}

class ConnectionMediatorSuite extends TestKit(ActorSystem("ConnectionMediatorSuiteActorSystem")) with FunSuiteLike with Matchers with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    // Stops the test actor system
    TestKit.shutdownActorSystem(system)
  }

}
