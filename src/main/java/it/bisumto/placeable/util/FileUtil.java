package it.bisumto.placeable.util;

import it.bisumto.placeable.Placeable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {
    public static void copy(@NotNull InputStream inputStream, @NotNull File file) {
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] array = new byte[1024];
            int read;
            while ((read = inputStream.read(array)) > 0) {
                outputStream.write(array, 0, read);
            }
            outputStream.close();
            inputStream.close();
        } catch (IOException exception) {
            Placeable.LOGGER.error("A problem occurred while copying the file", exception);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean create(@NotNull File file) {
        if (file.exists()) {
            return false;
        }

        File parent = file.getParentFile();
        if (parent == null) {
            return false;
        }

        parent.mkdirs();
        try {
            return file.createNewFile();
        } catch (IOException exception) {
            Placeable.LOGGER.error("A problem occurred while create the file", exception);
            return false;
        }
    }
}
