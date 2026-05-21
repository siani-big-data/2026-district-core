package siani.districting.architecture.stores;

import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;
import siani.districting.architecture.precinctinfo.PrecinctInfoContainer;
import siani.districting.architecture.stores.serializers.AdjacencySerializer;
import siani.districting.architecture.stores.serializers.DeltaSerializer;
import siani.districting.architecture.stores.serializers.PrecinctInfoContainerSerializer;
import siani.districting.architecture.stores.serializers.StateSerializer;
import siani.districting.architecture.stores.deserializers.AdjacencyDeserializer;
import siani.districting.architecture.stores.deserializers.DeltaDeserializer;
import siani.districting.architecture.stores.deserializers.PrecinctInfoContainerDeserializer;
import siani.districting.architecture.stores.reader.FileReader;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SerializerManager {
    private final String path;
    private Map<Precinct, Integer> changesMap;
    private int stepCount;
    private State lastState;
    private Set<String> lastAdjacencySet;
    private PrecinctInfoContainer lastContainer;
    private int snapshotStep = 50;
    DeltaSerializer deltaSerializer = new DeltaSerializer();
    StateSerializer stateSerializer = new StateSerializer();
    AdjacencySerializer adjacencySerializer = new AdjacencySerializer();
    PrecinctInfoContainerSerializer containerSerializer = new PrecinctInfoContainerSerializer();


    public SerializerManager(String dirPath, int snapshotStep) throws IOException {
        this.snapshotStep = snapshotStep;
        this.path = dirPath;
        File dir = new File(dirPath);
        this.stepCount = 0;
        this.lastState = null;
        this.changesMap = new HashMap<>();
        retrieveLastState(dir);
    }

    public SerializerManager(String dirPath) throws IOException {
        this.path = dirPath;
        File dir = new File(dirPath);
        this.stepCount = 0;
        this.lastState = null;
        this.changesMap = new HashMap<>();
        retrieveLastState(dir);
    }


    private void retrieveLastState(File dir) throws IOException {
        int lastStep = getLastStep();
        if (lastStep != -1) {
            this.stepCount = lastStep + 1;
            System.out.println("Recuperando último estado guardado");
            fetchLastState(lastStep);
            
            File adjacencyFile = new File(path + "/adjacency.dat");
            if (adjacencyFile.exists()) {
                this.lastAdjacencySet = AdjacencyDeserializer.deserialize(FileReader.read(adjacencyFile.getAbsolutePath()));
            }

            File containerFile = new File(path + "/precinctInfoContainer.dat");
            if (containerFile.exists()) {
                this.lastContainer = PrecinctInfoContainerDeserializer.deserialize(FileReader.read(containerFile.getAbsolutePath()));
            }
        } else {
            dir.mkdirs();
        }
    }

    public State getLastState() {
        return lastState;
    }

    public Set<String> getLastAdjacencySet() {
        return lastAdjacencySet;
    }

    public PrecinctInfoContainer getLastContainer() {
        return lastContainer;
    }

    public int getStepCount() {
        return stepCount;
    }

    private void fetchLastState(int lastIndex) throws IOException {
        this.lastState = StateRestorer.restore(new File(path), lastIndex, snapshotStep);
        this.stepCount = lastIndex + 1;
        if (lastIndex % snapshotStep != 0) {
            int snapshotNumber = lastIndex / snapshotStep;
            int deltaNumber = lastIndex % snapshotStep;
            File deltaFile = new File(path + "/state" + snapshotNumber + "_delta" + deltaNumber + ".dat");
            if (deltaFile.exists()) {
                try {
                    StateDelta lastDelta = DeltaDeserializer.deserialize(FileReader.read(deltaFile.getAbsolutePath()));
                    this.changesMap = new HashMap<>(lastDelta.differencies());
                } catch (Exception e) {
                    e.printStackTrace();
                    this.changesMap = new HashMap<>();
                }
            }
        }
    }

    private int getLastStep() {
        int maxStep = -1;
        File dir = new File(this.path);
        if (!dir.exists()) {return maxStep;}
        for (File file : getFilesAsArrayFrom(dir)) {
            String fileName = file.getName();
            if (fileName.startsWith("state") && fileName.endsWith(".dat")) {
                int currentIndex = getAbsoluteStep(fileName);
                if (currentIndex > maxStep) maxStep = currentIndex;
            }
        }
        return maxStep;
    }

    private int getAbsoluteStep(String fileName) {
        try {
            String name = fileName.replace(".dat", "");
            if (name.contains("_delta")) {
                String[] parts = name.split("_delta");
                int snap = Integer.parseInt(parts[0].replace("state", ""));
                int delta = Integer.parseInt(parts[1]);
                return snap * snapshotStep + delta;
            } else {
                int snap = Integer.parseInt(name.replace("state", ""));
                return snap * snapshotStep;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    private static File[] getFilesAsArrayFrom(File dir) {
        return Objects.requireNonNull(dir.listFiles());
    }

    public void serialize(Set<String> adjacency) throws IOException {
        adjacencySerializer.serialize(path + "/adjacency.dat", adjacency);
    }

    public void serialize(State state0, StateDelta delta, State state1) throws IOException {
        if (stepCount % snapshotStep == 0 || stepCount == 0) {
            serialize(state1);
            this.changesMap = new HashMap<>();
            stepCount++;
        }
        else {
            addDeltaChangesToMap(delta);
            serialize(delta);
            stepCount++;
            this.lastState = state1;
        }
    }

    private void addDeltaChangesToMap(StateDelta delta) {
        changesMap.putAll(delta.differencies());
    }

    private void serialize(StateDelta delta) throws IOException {
        int snapshotNumber = stepCount / snapshotStep;
        int deltaNumber = stepCount % snapshotStep;
        deltaSerializer.serialize(path + "/state" + snapshotNumber + "_delta" + deltaNumber + ".dat",
                new StateDelta(changesMap));
    }

    public void serialize(PrecinctInfoContainer container) throws IOException {
        containerSerializer.serialize(path + "/precinctInfoContainer.dat", container);
    }

    public void serializeAll(State state, Set<String> adjacency, PrecinctInfoContainer table) throws IOException {
        serialize(state);
        serialize(adjacency);
        serialize(table);
    }

    public void serialize(State state) throws IOException {
        if (this.stepCount == 0) {
            serializeInitialState(state);
        } else {
            int snapshotNumber = stepCount / snapshotStep;
            stateSerializer.serialize(path + "/state" + snapshotNumber + ".dat", state);
            this.lastState = state;
        }
    }

    public void serializeInitialState(State state) throws IOException {
        stateSerializer.serialize(path + "/state0.dat", state);
        this.lastState = state;
        this.changesMap = new HashMap<>();
    }
}
