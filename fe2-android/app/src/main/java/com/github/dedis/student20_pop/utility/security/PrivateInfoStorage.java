package com.github.dedis.student20_pop.utility.security;

import android.content.Context;
import android.util.Log;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKey;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * Private Information Storage Class
 *
 * <p>The private information is stored in an EncryptedFile, generated with a MasterKey.
 */
@Deprecated
public class PrivateInfoStorage {

  public static final String TAG = PrivateInfoStorage.class.getSimpleName();

  /**
   * Store private information in a file, overwrite existing data if the file already exists
   *
   * @param context of the Application
   * @param fileName name of file
   * @param data to store
   * @return true if successful, false if problem
   * @throws IllegalArgumentException if one of the inputs are null or the file doesn't exist
   */
  public static Boolean storeData(Context context, String fileName, String data) {
    if (context == null || fileName == null || data == null) {
      throw new IllegalArgumentException("Can't have null parameters");
    }
    try {
      EncryptedFile encryptedFile = buildEncryptedFile(context, fileName);
      OutputStream outputStream = encryptedFile.openFileOutput();
      outputStream.write(data.getBytes(StandardCharsets.UTF_8));
      outputStream.flush();
      outputStream.close();
      return true;
    } catch (GeneralSecurityException | IOException e) {
      Log.e(TAG, "Problem storing the data", e);
      return false;
    }
  }

  /**
   * Read the private data stored in a given file.
   *
   * @param context of the Application
   * @param fileName name of file
   * @return the private data read, null if problem
   * @throws IllegalArgumentException if one of the inputs are null
   */
  public static String readData(Context context, String fileName) {
    if (context == null || fileName == null) {
      throw new IllegalArgumentException("Can't have null parameters");
    }
    try {
      EncryptedFile encryptedFile = buildEncryptedFile(context, fileName);
      InputStream inputStream = encryptedFile.openFileInput();
      String data = readInputStream(inputStream);
      inputStream.close();
      return data;
    } catch (GeneralSecurityException | IOException e) {
      Log.e(TAG, "Problem reading the data", e);
      return null;
    }
  }

  private static String readInputStream(InputStream inputStream) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    int nextByte = inputStream.read();
    while (nextByte != -1) {
      byteArrayOutputStream.write(nextByte);
      nextByte = inputStream.read();
    }
    return byteArrayOutputStream.toString();
  }

  private static EncryptedFile buildEncryptedFile(Context context, String fileName)
      throws GeneralSecurityException, IOException {
    if (fileName.contains("/")) {
      fileName = fileName.replaceAll("/", "");
    }

    // format the file name
    String file = fileName + ".txt";

    EncryptedFile.Builder encryptedFileBuilder =
        new EncryptedFile.Builder(
            context,
            new File(context.getFilesDir(), file),
            // get primary master key for the given context
            new MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB);
    return encryptedFileBuilder.build();
  }
}
