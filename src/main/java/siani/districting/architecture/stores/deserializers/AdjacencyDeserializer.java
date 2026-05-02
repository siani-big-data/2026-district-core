package siani.districting.architecture.stores.deserializers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Set;

public class AdjacencyDeserializer {

    public static Set<String> deserialize(InputStream fis) throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(fis)) {
            return (Set<String>) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No se pudo deserializar el archivo: ");
        }

    }

}
