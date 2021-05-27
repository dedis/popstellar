package com.github.dedis.student20_pop.home;

import static org.junit.Assert.assertArrayEquals;

import android.app.Application;
import android.content.Context;
import androidx.core.util.Pair;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import com.github.dedis.student20_pop.home.HomeActivity;
import com.github.dedis.student20_pop.model.Wallet;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.StringJoiner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class WalletTest {


  private Context context = ApplicationProvider.getApplicationContext();

  @Test
  public void importSeedAndExportSeedAreCoherent()
      throws Exception {


    String Lao_ID = "1234123412341234";
    String Roll_Call_ID = "1234123412341234";

    Wallet hdw1 = new Wallet();

    hdw1.initKeysManager(context);

    String[] exp_str = hdw1.exportSeed();
    StringJoiner joiner = new StringJoiner(" ");
    for(String i: exp_str) joiner.add(i);

    Pair<byte[], byte[]> res1 =  hdw1.findKeyPair(Lao_ID,Roll_Call_ID);

    Wallet hdw2 = new Wallet();
    hdw2.initKeysManager(context);
    hdw2.importSeed(joiner.toString(), new HashMap<>());
    Pair<byte[], byte[]> res2 =  hdw2.findKeyPair(Lao_ID,Roll_Call_ID);

    assertArrayEquals(res1.first, res2.first);
    assertArrayEquals(res1.second, res2.second);

  }

}
