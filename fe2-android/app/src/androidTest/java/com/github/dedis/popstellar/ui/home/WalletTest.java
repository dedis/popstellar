package com.github.dedis.popstellar.ui.home;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.model.objects.security.PoPToken;

import net.i2p.crypto.eddsa.Utils;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class WalletTest {

  @Rule public HiltAndroidRule rule = new HiltAndroidRule(this);

  private final Context context = ApplicationProvider.getApplicationContext();

  @Test
  public void importSeedAndExportSeedAreCoherent() throws Exception {

    String Lao_ID = "1234123412341234";
    String Roll_Call_ID = "1234123412341234";

    Wallet hdw1 = new Wallet(context);
    String seed = String.join(" ", hdw1.exportSeed());
    PoPToken res1 = hdw1.findKeyPair(Lao_ID, Roll_Call_ID);

    Wallet hdw2 = new Wallet(context);
    hdw2.importSeed(seed, new HashMap<>());
    PoPToken res2 = hdw2.findKeyPair(Lao_ID, Roll_Call_ID);

    assertEquals(res1, res2);
  }

  @Test
  public void crossValidationWithFe1Web() throws GeneralSecurityException, IOException {
    String Lao_ID = "T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=";
    String Roll_Call_ID = "T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=";

    Wallet hdw = new Wallet(context);
    hdw.importSeed(
        "garbage effort river orphan negative kind outside quit hat camera approve first",
        new HashMap<>());
    PoPToken res = hdw.findKeyPair(Lao_ID, Roll_Call_ID);
    assertEquals(
        "9e8ca414e088b2276d140bb69302269ccede242197e1f1751c45ec40b01678a0",
        Utils.bytesToHex(res.getPrivateKey().getData()));
    assertEquals(
        "7147759d146897111bcf74f60a1948b1d3a22c9199a6b88c236eb7326adc2efc",
        Utils.bytesToHex(res.getPublicKey().getData()));
  }
}
