package ch.epfl.pop.model.network.method.message.data.dataObject

import org.scalatest.{FunSuite, Matchers}
import ch.epfl.pop.model.objects.{Base64Data, Hash, PublicKey}

class LaoDataSuite extends FunSuite with Matchers {
    test("Apply works with empty/full list for LaoData"){
        val laoData: LaoData = LaoData(PublicKey(Base64Data("a")), List.empty)

        laoData.owner should equal (PublicKey(Base64Data("a")))
        laoData.attendees should equal(List.empty)

        val laoData2: LaoData = LaoData(PublicKey(Base64Data("a")), List(PublicKey(Base64Data("b"))))

        laoData2.owner should equal (PublicKey(Base64Data("a")))
        laoData2.attendees should equal(List(PublicKey(Base64Data("b"))))
    }

    test("Json conversions work for LaoData") {
        val laoData: LaoData = LaoData(PublicKey(Base64Data("a")), List.empty)

        val laoData2: LaoData = LaoData.buildFromJson(laoData.toJsonString)

        laoData2.attendees should equal (List.empty)
        laoData2.owner should equal (PublicKey(Base64Data("a")))
    }

}
