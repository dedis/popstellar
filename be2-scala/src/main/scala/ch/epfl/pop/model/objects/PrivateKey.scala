package ch.epfl.pop.model.objects

import com.google.crypto.tink.subtle.Ed25519Sign

case class PrivateKey(base64Data: Base64Data) {
<<<<<<< HEAD

=======
  
>>>>>>> b5f38d817d041d3e55ba3f0b0640d3cdea1ca483
  def signData(data: Base64Data): Signature = {
    val ed: Ed25519Sign = new Ed25519Sign(base64Data.decode())
    Signature(Base64Data.encode(ed.sign(data.decode())))
  }

<<<<<<< HEAD
} 
=======
}
>>>>>>> b5f38d817d041d3e55ba3f0b0640d3cdea1ca483
