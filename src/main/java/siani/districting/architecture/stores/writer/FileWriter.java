package siani.districting.architecture.stores.writer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileWriter {

    private FileWriter() {}

    public static void writeObject(String path, byte[] serializedObject) throws IOException {
        Files.write(Paths.get(path), serializedObject);
    }

}
