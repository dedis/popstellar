package com.github.dedis.student20_pop.utility.security;

import android.content.Context;

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
 * The private information is stored in an encrypted file, generated with a Master Key.
 */
public class PrivateInfoStorage {

    public static final String TAG = PrivateInfoStorage.class.getSimpleName();
    public static final String DIRECTORY = "PrivateInformationStorage";

    /**
     * Store private information in a file,
     * overwrite existing data if the file already exists
     *
     * @param context of the Application
     * @param fileName name of file, can't contain path separator
     * @param data to store
     * @throws IllegalArgumentException if one of the inputs are null or the file doesn't exist
     * @throws GeneralSecurityException if problem building the encrypted file of master key
     * @throws IOException if problem writing on the file
     */
    public static void storeData(Context context, String fileName, String data) throws GeneralSecurityException, IOException {
        if(context == null || fileName == null || data == null) {
            throw new IllegalArgumentException("Can't have null parameters");
        }
        EncryptedFile encryptedFile = buildEncryptedFile(context, fileName);
        OutputStream outputStream = encryptedFile.openFileOutput();
        outputStream.write(data.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();
    }

    /**
     * Get the data stored in a given file.
     *
     * @param context of the Application
     * @param fileName name of file, can't contain path separator
     * @return the data stored if the file exists, null otherwise
     * @throws IllegalArgumentException if one of the inputs are null
     * @throws GeneralSecurityException if problem building the encrypted file of master key
     * @throws IOException if problem reading the file
     */

    public static String readData(Context context, String fileName) throws GeneralSecurityException, IOException {
        if(context == null || fileName == null) {
            throw new IllegalArgumentException("Can't have null parameters");
        }
        EncryptedFile encryptedFile = buildEncryptedFile(context, fileName);
        InputStream inputStream = encryptedFile.openFileInput();
        String data = readInputStream(inputStream);
        inputStream.close();
        return data;
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

    private static EncryptedFile buildEncryptedFile(Context context, String fileName) throws GeneralSecurityException, IOException {
        if(fileName.contains("/")) {
            throw new IllegalArgumentException("The file name can't contain path separators");
        }

        // format the file name
        String file = fileName + ".txt";

        EncryptedFile.Builder encryptedFileBuilder = new EncryptedFile.Builder(context,
                new File(DIRECTORY, file),
                // get primary master key for the given context
                new MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB );
        return encryptedFileBuilder.build();
    }
}
