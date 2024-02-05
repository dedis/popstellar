package com.github.dedis.popstellar.ui.home

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.TestKeysetModule.provideWalletKeysetManager
import com.github.dedis.popstellar.model.objects.Wallet
import com.github.dedis.popstellar.utility.error.keys.KeyException
import com.github.dedis.popstellar.utility.error.keys.SeedValidationException
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.lang.String.join
import java.security.GeneralSecurityException
import net.i2p.crypto.eddsa.Utils
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WalletTest {
  @JvmField @Rule var rule = HiltAndroidRule(this)

  @Test
  @Throws(Exception::class)
  fun importSeedAndExportSeedAreCoherent() {
    val laoID = "1234123412341234"
    val rollCallID = "1234123412341234"
    val hdw1 = Wallet(provideWalletKeysetManager())
    hdw1.importSeed(hdw1.newSeed())

    val seed = join(" ", *hdw1.exportSeed())
    val res1 = hdw1.generatePoPToken(laoID, rollCallID)
    val hdw2 = Wallet(provideWalletKeysetManager())
    hdw2.importSeed(seed)

    val res2 = hdw2.generatePoPToken(laoID, rollCallID)

    Assert.assertEquals(res1, res2)
  }

  @Test
  @Throws(GeneralSecurityException::class, KeyException::class)
  fun crossValidationWithFe1Web() {
    val laoID = "T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg="
    val rollCallID = "T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg="
    val hdw = Wallet(provideWalletKeysetManager())
    hdw.importSeed(
      "garbage effort river orphan negative kind outside quit hat camera approve first"
    )
    val res = hdw.generatePoPToken(laoID, rollCallID)

    Assert.assertEquals(
      "9e8ca414e088b2276d140bb69302269ccede242197e1f1751c45ec40b01678a0",
      Utils.bytesToHex(res.privateKey.data)
    )
    Assert.assertEquals(
      "7147759d146897111bcf74f60a1948b1d3a22c9199a6b88c236eb7326adc2efc",
      Utils.bytesToHex(res.publicKey.data)
    )
  }

  @Test
  @Throws(GeneralSecurityException::class, SeedValidationException::class)
  fun walletMnemonicCoherenceTest() {
    val wallet = Wallet(provideWalletKeysetManager())
    val wordArray =
      arrayOf(
        "jar",
        "together",
        "minor",
        "alley",
        "glow",
        "hybrid",
        "village",
        "creek",
        "meadow",
        "atom",
        "travel",
        "bracket"
      )
    wallet.importSeed(join(" ", *wordArray))
    val exported = wallet.exportSeed()

    Assert.assertArrayEquals(wordArray, exported)
  }
}
