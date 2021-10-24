package ch.epfl.pop.model.objects

import org.scalatest.{FunSuite, Matchers, BeforeAndAfterAll}
import com.google.crypto.tink.subtle.Ed25519Sign
import java.nio.charset.StandardCharsets


/*Helper object for testing*/
case class TestObj(ed_signer: Ed25519Sign, keyPair: Ed25519Sign.KeyPair)
class SignatureSuite extends FunSuite with Matchers with BeforeAndAfterAll{

    var tester: TestObj = null;

    override def beforeAll(){
            val kpair = Ed25519Sign.KeyPair.newKeyPair()

            val privateKey = kpair.getPrivateKey 
            val ed_signer  = new Ed25519Sign(privateKey)
            tester = TestObj(ed_signer, kpair) 

    } 
    

    /*Provides correct signature for a msg*/
    def getTrueSignatureTest(msg: String): Signature = {

        val data = msg.getBytes(StandardCharsets.UTF_8)
        val signed_data = tester.ed_signer.sign(data)
        val signature = Signature(Base64Data.encode(signed_data))
        signature

    }

    /*Provides falsified signature for a msg*/
    def getFalseSignatureTest(msg: String): Signature = {
        getTrueSignatureTest("X" + msg + "Y")

    }
    
    test("Basic true signature (1)"){
        /***/
        val msg = "HelloWorld"
        val signature = getTrueSignatureTest(msg);
        //Assertion
        val verify_pk = PublicKey(Base64Data.encode(tester.keyPair.getPublicKey))
        val msg_encoded = Base64Data.encode(msg)
        signature.verify(verify_pk,msg_encoded) should be (true)


    }

    test("Basic true signature (2)"){
        /***/
        val msg = "Not true is false"
        val signature = getTrueSignatureTest(msg);
        
        val verify_pk = PublicKey(Base64Data.encode(tester.keyPair.getPublicKey))
        val msg_encoded = Base64Data.encode(msg)

        signature.verify(verify_pk,msg_encoded) should be (true)


    }

    test("Basic true empty message signature (1)"){
        /***/
        val msg = ""
        val signature = getTrueSignatureTest(msg);
        //Assertion
        val verify_pk = PublicKey(Base64Data.encode(tester.keyPair.getPublicKey))
        val msg_encoded = Base64Data.encode(msg)
        signature.verify(verify_pk,msg_encoded) should be (true)


    }

     test("Basic true one letter message signature (1)"){
        /***/
        val msg = "A"
        val signature = getTrueSignatureTest(msg);
        //Assertion
        val verify_pk = PublicKey(Base64Data.encode(tester.keyPair.getPublicKey))
        val msg_encoded = Base64Data.encode(msg)
        signature.verify(verify_pk,msg_encoded) should be (true)


    }

     test("Basic false signature (1)"){
        /**Fake signature**/
        val msg = "Not false is true"
        val signature = getFalseSignatureTest(msg);
        //Assertion
        val verify_pk = PublicKey(Base64Data.encode(tester.keyPair.getPublicKey))
        val msg_encoded = Base64Data.encode(msg)
        signature.verify(verify_pk,msg_encoded) should be (false)


    }

    test("Basic false signature (2)"){
        /*Fake signature*/
        val msg = "Non empty can be fully non empty"
        val signature = getFalseSignatureTest(msg);
        //Assertion
        val verify_pk = PublicKey(Base64Data.encode(tester.keyPair.getPublicKey))
        val msg_encoded = Base64Data.encode(msg)
        signature.verify(verify_pk,msg_encoded) should be (false)


    }

    
    test("Basic false empty message signature (2)"){
        /*Fake signature*/
        val msg = ""
        val signature = getFalseSignatureTest(msg);
        //Assertion
        val verify_pk = PublicKey(Base64Data.encode(tester.keyPair.getPublicKey))
        val msg_encoded = Base64Data.encode(msg)
        signature.verify(verify_pk,msg_encoded) should be (false)


    }
        test("Basic false one letter message signature (1)"){
        /***/
        val msg = "A"
        val signature = getFalseSignatureTest(msg);
        //Assertion
        val verify_pk = PublicKey(Base64Data.encode(tester.keyPair.getPublicKey))
        val msg_encoded = Base64Data.encode(msg)
        signature.verify(verify_pk,msg_encoded) should be (false)


    }


}