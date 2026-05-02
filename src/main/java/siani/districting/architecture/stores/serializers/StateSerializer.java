package siani.districting.architecture.stores.serializers;

import siani.districting.architecture.model.State;
import siani.districting.architecture.stores.writer.FileWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class StateSerializer {

    public StateSerializer() {}

    public byte[] serialize(State state) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            return baos.toByteArray();
        }
    }

    public byte[] serialize(String path, State state) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos);
        ) {
            oos.writeObject(state);
            oos.flush();
            FileWriter.writeObject(path, baos.toByteArray());
            return baos.toByteArray();
        }
    }
}
