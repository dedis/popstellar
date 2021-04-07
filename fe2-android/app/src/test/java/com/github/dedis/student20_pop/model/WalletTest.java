package com.github.dedis.student20_pop.model;

import androidx.core.util.Pair;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.StringJoiner;
import javax.crypto.ShortBufferException;
import net.i2p.crypto.eddsa.Utils;
import org.junit.Test;

public class WalletTest {

  @Test
  public void simpleTest()
      throws NoSuchAlgorithmException, ShortBufferException, InvalidKeyException {
    Wallet hdw = new Wallet();
    Pair<byte[], byte[]> res =  hdw.FindKeyPair("1234123412341234","1234123412341234");
    System.out.println("Private KEY FOUND = " + Utils.bytesToHex(res.first));
    System.out.println("Public KEY FOUND  = " + Utils.bytesToHex(res.second));

  }
  @Test
  public void test_with_initseed()
      throws NoSuchAlgorithmException, ShortBufferException, InvalidKeyException {
    Wallet hdw = new Wallet();
    hdw.initialize("47370b02e171eda5a766c149d26c18943c252a6733138b5aa5eaf04a6788b6b8");
    Pair<byte[], byte[]> res =  hdw.FindKeyPair("1234123412341234","1234123412341234");
    System.out.println("Private KEY FOUND = " + Utils.bytesToHex(res.first));
    System.out.println("Public KEY FOUND  = " + Utils.bytesToHex(res.second));
    // d8f59df0ed3a7646bbe9f4fc040c016fe0ab11b0d6204c786431ee4fa1e1fb1a
    // d737bd1a26ab291be2c0fd83f2b3565f13c007e1123619e91ab33826a5be7de6
  }

  @Test
  public void test_memonic(){
    Wallet hdw = new Wallet();
    String[] exp_str = hdw.ExportSeed();
    StringJoiner joiner = new StringJoiner(" ");
    for(String i: exp_str) joiner.add(i);
    System.out.println(joiner.toString());
    //hdw.ImportSeed(joiner.toString());
  }

}
