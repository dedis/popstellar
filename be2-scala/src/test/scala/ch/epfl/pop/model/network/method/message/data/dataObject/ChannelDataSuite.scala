package ch.epfl.pop.model.network.method.message.data.dataObject

import org.scalatest.{FunSuite, Matchers}
import ch.epfl.pop.model.objects.{Base64Data, Hash}
import ch.epfl.pop.model.network.method.message.data.ObjectType

class ChannelDataSuite extends FunSuite with Matchers {
    test("Apply works with empty/full list for ChannelData"){
        val channelData: ChannelData = ChannelData(ObjectType.LAO, List.empty)

        channelData.messages should equal (List.empty)
        channelData.channel_type should equal(ObjectType.LAO)

        val channelData2: ChannelData = ChannelData(ObjectType.LAO, List(Hash(Base64Data("a"))))

        channelData2.messages should equal (List(Hash(Base64Data("a"))))
        channelData2.channel_type should equal(ObjectType.LAO)
    }

    test("New messageId is indeed added") {
        val channelData: ChannelData = ChannelData(ObjectType.LAO, List.empty)

        channelData.messages should equal (List.empty)
        channelData.channel_type should equal(ObjectType.LAO)

        val channelData2 = channelData.addMessage(Hash(Base64Data("a")))

        channelData2.messages.size should equal (1)
    }

    test("Json conversions work for ChannelData") {
        val channelData: ChannelData = ChannelData(ObjectType.LAO, List.empty)

        val channelData2: ChannelData = ChannelData.buildFromJson(channelData.toJsonString)

        channelData2.messages should equal (List.empty)
        channelData2.channel_type should equal (ObjectType.LAO)
    }

}
