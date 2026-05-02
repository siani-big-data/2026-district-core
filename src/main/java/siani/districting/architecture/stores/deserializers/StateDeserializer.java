package siani.districting.architecture.stores.deserializers;

import siani.districting.architecture.model.State;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class StateDeserializer {
    public static State deserialize(InputStream is) throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(is)) {
            return (State) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No se pudo deserializar el archivo: ", e);
        }
    }

}
