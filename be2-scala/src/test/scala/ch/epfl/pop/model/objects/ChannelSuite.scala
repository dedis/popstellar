package ch.epfl.pop.model.objects

import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers

class ChannelSuite extends FunSuite with Matchers {
  test("Root/Sub channel test (1)") {
    val channel = Channel.ROOT_CHANNEL
    channel.isRootChannel should be(true)
    channel.isSubChannel should be(false)
  }

  test("Root/Sub channel test (2)") {
    def channel = Channel("/root/not/")

    an[IllegalArgumentException] shouldBe thrownBy(channel)
  }

  test("Root/Sub channel test (3)") {
    def channel = Channel(Channel.ROOT_CHANNEL_PREFIX)

    an[IllegalArgumentException] shouldBe thrownBy(channel)
  }

  test("Root/Sub channel test (4)") {
    def channel = Channel("/root//")

    an[IllegalArgumentException] shouldBe thrownBy(channel)
  }

  test("Empty child channel test") {
    def channel = Channel("")

    an[IllegalArgumentException] shouldBe thrownBy(channel)
  }

  test("Slash empty child channel test") {
    def channel = Channel("" + Channel.CHANNEL_SEPARATOR)

    an[IllegalArgumentException] shouldBe thrownBy(channel)
  }

  test("Slash single child channel test") {
    def channel = Channel(Channel.ROOT_CHANNEL_PREFIX + "base64==")

    val expected = Hash(Base64Data("base64=="))
    noException shouldBe thrownBy(channel)
    channel.extractChildChannel should equal(expected)
  }

  test("Root empty child channel test") {
    def channel = Channel.ROOT_CHANNEL

    val expected = Hash(Base64Data("root"))
    channel.extractChildChannel should equal(expected)
  }

  test("Root + 3 children channel test") {
    def channel = Channel("/root/to/ultra/test")

    val expected = Hash(Base64Data("test"))
    noException shouldBe thrownBy(channel)
    channel.extractChildChannel should equal(expected)
  }

  test("Root + 2 children channel test") {
    def channel = Channel("/root/full/pop")

    val expected = Hash(Base64Data("pop"))
    an[IllegalArgumentException] shouldNot be(thrownBy(channel))
    channel.extractChildChannel should equal(expected)
  }

  test("Encoded LaoId extraction channel test") {
    val laoId = "base64_lao_id"

    def channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data.encode(laoId))

    val expected = Hash(Base64Data.encode(laoId))
    noException shouldBe thrownBy(channel)
    channel.decodeChannelLaoId.get should equal(expected)
  }

  test("Real LaoId extraction channel test") {
    val laoId = "mEKXWFCMwb"

    def channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data(laoId))

    val expected = Hash(Base64Data(laoId))

    noException shouldBe thrownBy(channel)
    channel.decodeChannelLaoId.get should equal(expected)
  }

  test("Real LaoId extraction from the middle of a channel test") {
    val laoId = "mEKXWFCMwb"

    def channel = Channel(Channel.ROOT_CHANNEL_PREFIX + Base64Data(laoId) + Channel.CHANNEL_SEPARATOR + Base64Data.encode("social"))

    val expected = Hash(Base64Data(laoId))

    noException shouldBe thrownBy(channel)
    channel.decodeChannelLaoId.get should equal(expected)
  }

  test("Subchannel extraction fails with wrong channel structure (not base64Data)") {
    val laoId = "not_base64_lao_id"

    def channel = Channel(Channel.ROOT_CHANNEL_PREFIX + laoId)

    val expected = None

    noException shouldBe thrownBy(channel)
    channel.decodeChannelLaoId should equal(expected)
  }

  test("LaoId extraction fails with wrong channel structure (not base64Data)") {
    val laoId = "not_base64_lao_id"

    def channel = Channel(Channel.ROOT_CHANNEL_PREFIX + laoId)

    val expected = None

    noException shouldBe thrownBy(channel)
    channel.decodeChannelLaoId should equal(expected)
  }

  test("Bad LaoId: dosn't start with /root/ extraction channel test (1)") {
    val laoId = "/toor/base64_lao_id"

    def channel = Channel(laoId)

    an[IllegalArgumentException] shouldBe thrownBy(channel)
  }

  test("Bad LaoId: not encoded in base64 extraction channel test (2)") {
    val laoId = "not_base64_lao_id"

    def channel = Channel(Channel.ROOT_CHANNEL_PREFIX + laoId)

    val expected = None
    noException shouldBe thrownBy(channel)
    channel.decodeChannelLaoId should equal(expected)
  }

  test("extractLaoChannel() correctly extract main lao channel") {
    val chan1 = Channel("/root/wex")
    val chan2 = Channel("/root/wex/xyz")
    val chan3 = Channel("/root")

    chan1.extractLaoChannel should equal(Some(chan1))
    chan2.extractLaoChannel should equal(Some(chan1))
    chan3.extractLaoChannel should equal(None)
  }

  test("isRootLaoChannel() on /root/lao_id return true ") {
    val rootLaoId = Channel("/root/wex")

    rootLaoId.isRootLaoChannel should equal(true)
  }

  test("isRootLaoChannel() on /root/lao_id/sub return false ") {
    val rootLaoIdSub = Channel("/root/wex/sub")

    rootLaoIdSub.isRootLaoChannel should equal(false)
  }
}
