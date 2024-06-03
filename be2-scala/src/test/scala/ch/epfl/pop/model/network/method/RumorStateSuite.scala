package ch.epfl.pop.model.network.method

import ch.epfl.pop.model.objects.{Base64Data, PublicKey}
import org.scalatest.matchers.should.Matchers
import org.scalatest.funsuite.AnyFunSuite as FunSuite

class RumorStateSuite extends FunSuite with Matchers {

  test("constructor from json works for RumorState") {
    val state: Map[PublicKey, Int] = Map(
      PublicKey(Base64Data.encode("1")) -> 1,
      PublicKey(Base64Data.encode("2")) -> 2,
      PublicKey(Base64Data.encode("3")) -> 3
    )

    val rumorState: RumorState = RumorState(state)

    val encodedDecoded = RumorState.buildFromJson(rumorState.toJsonString)

    encodedDecoded.state shouldBe state
  }

  test("difference from rumorState works well if second element as more rumors") {
    val rumorState1: RumorState = RumorState(Map(
      PublicKey(Base64Data.encode("1")) -> 1,
      PublicKey(Base64Data.encode("2")) -> 2
    ))

    val rumorState2: RumorState = RumorState(Map(
      PublicKey(Base64Data.encode("1")) -> 3,
      PublicKey(Base64Data.encode("2")) -> 5,
      PublicKey(Base64Data.encode("3")) -> 3
    ))

    val diffResult = rumorState1.isMissingRumorsFrom(rumorState2)

    diffResult shouldBe Map(
      PublicKey(Base64Data.encode("1")) -> List(2, 3),
      PublicKey(Base64Data.encode("2")) -> List(3, 4, 5),
      PublicKey(Base64Data.encode("3")) -> List(0, 1, 2, 3)
    )
  }

  test("difference from rumorState works well if first element as more rumors") {
    val rumorState1: RumorState = RumorState(Map(
      PublicKey(Base64Data.encode("1")) -> 3,
      PublicKey(Base64Data.encode("2")) -> 5,
      PublicKey(Base64Data.encode("3")) -> 3
    ))

    val rumorState2: RumorState = RumorState(Map(
      PublicKey(Base64Data.encode("1")) -> 1,
      PublicKey(Base64Data.encode("2")) -> 2
    ))

    val diffResult = rumorState1.isMissingRumorsFrom(rumorState2)
    diffResult shouldBe Map.empty
  }
}
