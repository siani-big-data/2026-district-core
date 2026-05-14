package siani.districting.architecture.stores.serializers;

import siani.districting.architecture.model.State;
import siani.districting.architecture.precinctinfo.PrecinctInfoContainer;
import siani.districting.architecture.stores.writer.FileWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class PrecinctInfoContainerSerializer {

    public PrecinctInfoContainerSerializer() {}

    public byte[] serialize(PrecinctInfoContainer container) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            return baos.toByteArray();
        }
    }

    public byte[] serialize(String path, PrecinctInfoContainer container) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos);
        ) {
            oos.writeObject(container);
            oos.flush();
            FileWriter.writeObject(path, baos.toByteArray());
            return baos.toByteArray();
        }
    }
}
