package siani.districting.architecture.stores.serializers;

import siani.districting.architecture.stores.writer.FileWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Set;

public class AdjacencySerializer {

    public byte[] serialize(Set<String> adjacencySet) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(adjacencySet);
            oos.flush();
            return baos.toByteArray();
        }
    }

    public byte[] serialize(String path, Set<String> adjacencySet) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(adjacencySet);
            oos.flush();
            FileWriter.writeObject(path, baos.toByteArray());
            return baos.toByteArray();
        }
    }

}
