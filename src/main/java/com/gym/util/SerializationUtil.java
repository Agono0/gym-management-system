package com.gym.util;

import com.gym.exception.DatabaseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

// Saves and loads serializable objects using binary files
public class SerializationUtil {

    private SerializationUtil() {
    }

    public static void saveToFile(Object object, String filePath) throws DatabaseException {
        if (object == null) {
            throw new DatabaseException("Cannot save a null object");
        }
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(object);
        } catch (IOException e) {
            throw new DatabaseException("Could not save object to file: " + filePath, e);
        }
    }

    public static Object loadFromFile(String filePath) throws DatabaseException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new DatabaseException("File not found: " + filePath);
        }

        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            return ois.readObject();
        } catch (IOException e) {
            throw new DatabaseException("Could not read file: " + filePath, e);
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("Saved class definition not found for: " + filePath, e);
        }
    }
}
