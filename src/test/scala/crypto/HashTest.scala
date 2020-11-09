package crypto

import ch.epfl.pop.crypto.Hash
import org.scalatest.FunSuite

class HashTest extends FunSuite{

  test("Verify that the id computed is correct") {
    val msg = "{\"type\":\"rollcall\"}"
    val signature = "signature"
    val correctHash = "01c55bfd46fbc1577de65d86ff117440280b460f2c2e26845326cc10588c3d44"
    assert(Hash.computeID(msg, signature) == correctHash)
  }

}
