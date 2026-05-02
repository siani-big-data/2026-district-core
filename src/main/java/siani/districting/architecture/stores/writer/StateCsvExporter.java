package siani.districting.architecture.stores.writer;

import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class StateCsvExporter {
    public static void export(State state, String path) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            writer.println("precinct_id,district_id");
            for (Map.Entry<Precinct, Integer> entry : state.getPrecinctsAndDistrictsMap().entrySet()) {
                writer.println(entry.getKey().id() + "," + entry.getValue());
            }
        }
    }
}