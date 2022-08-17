package ch.epfl.pop.model.objects

import ch.epfl.pop.model.network.method.message.data.ObjectType
import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers

class ChannelDataSuite extends FunSuite with Matchers {
  test("Apply works with empty/full list for ChannelData") {
    val channelData: ChannelData = ChannelData(ObjectType.LAO, List.empty)

    channelData.messages should equal(List.empty)
    channelData.channelType should equal(ObjectType.LAO)

    val channelData2: ChannelData = ChannelData(ObjectType.LAO, List(Hash(Base64Data("base64=="))))

    channelData2.messages should equal(List(Hash(Base64Data("base64=="))))
    channelData2.channelType should equal(ObjectType.LAO)
  }

  test("New messageId is indeed added") {
    val channelData: ChannelData = ChannelData(ObjectType.LAO, List.empty)

    channelData.messages should equal(List.empty)
    channelData.channelType should equal(ObjectType.LAO)

    val channelData2 = channelData.addMessage(Hash(Base64Data("base64==")))

    channelData2.messages.size should equal(1)
  }

  test("Json conversions work for ChannelData") {
    val channelData: ChannelData = ChannelData(ObjectType.LAO, List.empty)

    val channelData2: ChannelData = ChannelData.buildFromJson(channelData.toJsonString)

    channelData2 should equal(channelData)
  }

}
