package ch.epfl.pop.model.objects

import org.scalatest.{FunSuite, Matchers}
import ch.epfl.pop.model.objects.Channel

class ChannelSuite extends FunSuite with Matchers   {
    

  test("Root/Sub channel test (1)"){
    val channel = Channel.rootChannel
    channel.isRootChannel should be (true)
    channel.isSubChannel should be (false)
  }

    test("Root/Sub channel test (2)"){
    val channel = Channel("/root/not/")

    channel.isRootChannel should be (false)
    channel.isSubChannel should be (true)

  }
  test("Root/Sub channel test (3)"){
    val channel = Channel(Channel.rootChannelPrefix)

    channel.isRootChannel should be (false)
    channel.isSubChannel should be (true)

  }
    test("Root/Sub channel test (4)"){
    val channel = Channel("/root//")

    channel.isRootChannel should be (false)
    channel.isSubChannel should be (true)

  }

    test("Empty child channel test"){
        val channel = Channel("")
        val expected = Hash(Base64Data(""))
        channel.extractChildChannel should equal(expected)

    }
     test("Slash empty child channel test"){
        val channel = Channel(""+ Channel.SEPARATOR)
        val expected = Hash(Base64Data(""))
        channel.extractChildChannel should equal(expected)

    }

     test("Slash single child channel test"){
        val channel = Channel(""+ Channel.SEPARATOR +"channelID")
        val expected = Hash(Base64Data("channelID"))
        channel.extractChildChannel should equal(expected)

    }

     test("Root empty child channel test"){
        val channel = Channel.rootChannel
        val expected = Hash(Base64Data("root"))
        channel.extractChildChannel should equal(expected)

    }
    
       test("Root + 3 children channel test"){
        val channel = Channel("/root/to/ultra/test/")
        val expected = Hash(Base64Data("test"))
        channel.extractChildChannel should equal(expected)

    }

       test("Root + 2 children channel test"){
        val channel = Channel("/root/full/pop")
        val expected = Hash(Base64Data("pop"))
        channel.extractChildChannel should equal(expected)

    }

        test("LaoId extraction channel test"){
        val laoId = "base64_lao_id";
        val channel = Channel(Channel.rootChannelPrefix + Base64Data.encode(laoId))
        val expected = laoId.getBytes()

        channel.decodeSubChannel.get should equal (expected)

    }
      test("Empty LaoId extraction channel test"){
        val laoId = "";
        val channel = Channel(Channel.rootChannelPrefix + Base64Data.encode(laoId))
        val expected = laoId.getBytes()

        channel.decodeSubChannel.get should equal (expected)

    }
      test("Real LaoId extraction channel test"){
        val laoId = "mEKXWFCMwb";
        val channel = Channel(Channel.rootChannelPrefix + Base64Data.encode(laoId))
        val expected = laoId.getBytes()

        channel.decodeSubChannel.get should equal (expected)

    }
       test("Bad LaoId: dosn't start with /root/ extraction channel test (1)"){
        val laoId = "/toor/base64_lao_id";
        val channel = Channel(laoId)
        val expected = None
        
        channel.decodeSubChannel should equal (expected)
    }
       test("Bad LaoId: not encoded in base64 extraction channel test (2)"){
        val laoId = "base64_lao_id"; // Not encoded in BASE64
        val channel = Channel(Channel.rootChannelPrefix + laoId)
        val expected = None
        
        channel.decodeSubChannel should equal (expected)
    }




}
