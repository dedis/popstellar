package ch.epfl.pop.model.objects

import com.google.crypto.tink.subtle.Ed25519Sign

import org.scalatest.{FunSuite, Matchers}

class PrivateKeySuite extends FunSuite with Matchers {
    test("Constructor/apply works as intended"){
        val keyData: Base64Data = Base64Data.encode("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
        val data: Base64Data = Base64Data.encode("abc")
        val ed: Ed25519Sign = new Ed25519Sign(keyData.decode())

        val expected: Signature = Signature(Base64Data.encode(ed.sign(data.decode())))

        val pk: PrivateKey = new PrivateKey(keyData)

        pk.signData(data) should equal(expected)
    }
} 
