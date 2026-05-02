package siani.districting.architecture.stores;

import siani.districting.architecture.model.State;
import siani.districting.architecture.engine.environment.StateFactory;
import siani.districting.architecture.stores.deserializers.DeltaDeserializer;
import siani.districting.architecture.stores.deserializers.StateDeserializer;
import siani.districting.architecture.stores.reader.FileReader;

import java.io.File;
import java.io.IOException;

public class StateRestorer {

    public static State restore(File dir, int lastIndex) throws IOException {
        int stateIndex = (lastIndex / 50);
        File snapshotFile = new File(dir, "state" + stateIndex + ".dat");
        State state = StateDeserializer.deserialize(FileReader.read(snapshotFile.getAbsolutePath()));
        if (lastIsDelta(lastIndex)) {
            int deltaIndex = lastIndex % 50;
            File deltaFile = new File(dir, "state" + stateIndex + "_delta" + deltaIndex + ".dat");
            StateDelta delta = DeltaDeserializer.deserialize(FileReader.read(deltaFile.getAbsolutePath()));
            state = StateFactory.applyDelta(state, delta);
        }
        return state;
    }

    private static boolean lastIsDelta(int lastIndex) {
        return lastIndex % 50 != 0;
    }
}
