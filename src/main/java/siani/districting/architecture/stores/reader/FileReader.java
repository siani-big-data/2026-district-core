package siani.districting.architecture.stores.reader;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileReader {

    public static InputStream read(String path) throws IOException {
        byte[] fileBytes = Files.readAllBytes(Path.of(path));
        return new ByteArrayInputStream(fileBytes);
    }

}
