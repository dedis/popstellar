package com.github.dedis.student20_pop.utility.security;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

/**
 * Private Information Storage Class
 *
 * Uses Internal Storage to store private information.
 */
public class PrivateInfoStorage {
    //can implement as a singleton in the future to only store one private file per user

    public static final String TAG = PrivateInfoStorage.class.getSimpleName();

    /**
     * Store private information in a file
     *
     * @param context of the Application
     * @param fileName name of file
     * @param data to store
     * @throws IllegalArgumentException if one of the inputs are null or the file doesn't exist
     */
    public static void storeData(Context context, String fileName, String data) throws IllegalArgumentException {
        if(context == null || fileName == null || data == null) {
            throw new IllegalArgumentException("Can't have null parameters");
        }
        try {
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fos.write(0);
            fos.close();
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("The file named " + fileName + " doesn't exist");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the data stored in a given file.
     *
     * @param context of the Application
     * @param fileName name of file
     * @return the data stored if the file exists, null otherwise
     * @throws IllegalArgumentException if one of the inputs are null
     */

    public static String readData(Context context, String fileName) throws IllegalArgumentException {
        if(context == null || fileName == null) {
            throw new IllegalArgumentException("Can't have null parameters");
        }
        try {
            FileInputStream fos = context.openFileInput(fileName);
            int read = 0;
            int length = 0;
            while(read != -1) {
                read = fos.read();
                ++length;
            }
            byte[] data = new byte[length];
            fos.read(data);
            fos.close();
            return String.valueOf(data);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("The file named " + fileName + " doesn't exist");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
