package siani.districting.architecture.stores.deserializers;

import siani.districting.architecture.stores.StateDelta;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class DeltaDeserializer {

    public static StateDelta deserialize(InputStream is) {
        try (ObjectInputStream ois = new ObjectInputStream(is)) {
            return (StateDelta) ois.readObject();
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException("No se pudo deserializar el archivo: ");
        }
    }

}
