package siani.districting.architecture.stores.serializers;

import siani.districting.architecture.stores.StateDelta;
import siani.districting.architecture.stores.writer.FileWriter;

import java.io.*;

public class DeltaSerializer {

    public DeltaSerializer() {}

    public byte[] serialize(StateDelta delta) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            return baos.toByteArray();
        }
    }

    public byte[] serialize(String path, StateDelta delta) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(delta);
            oos.flush();
            FileWriter.writeObject(path, baos.toByteArray());
            return baos.toByteArray();
        }
    }
}
