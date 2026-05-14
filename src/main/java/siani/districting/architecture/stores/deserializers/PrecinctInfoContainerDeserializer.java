package siani.districting.architecture.stores.deserializers;

import siani.districting.architecture.precinctinfo.PrecinctInfoContainer;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class PrecinctInfoContainerDeserializer {
    public static PrecinctInfoContainer deserialize(InputStream is) throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(is)) {
            return (PrecinctInfoContainer) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No se pudo deserializar el archivo: ", e);
        }
    }

}
