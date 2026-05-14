package siani.districting.architecture.stores.writer;

import siani.districting.architecture.engine.environment.ElectionCalculator;
import siani.districting.architecture.model.District;
import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;
import siani.districting.architecture.precinctinfo.PrecinctInfoContainer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class StateCsvExporter {
    public static void export(State state, String path) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            writer.println("precinct_id,district_id,election_winner");
            for (Map.Entry<Precinct, Integer> entry : state.getPrecinctsAndDistrictsMap().entrySet()) {
                writer.println(entry.getKey().id() +
                        "," + entry.getValue());
            }
        }
    }

    public static void exportWithWinnersPerDistrict(State state, String path, PrecinctInfoContainer container, Map<Object, Object> candidateToPartyMap) throws IOException {
        Map<Integer, String> winners = ElectionCalculator.calculateWinnersPerDistrict(state, container, candidateToPartyMap);
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            writer.println("precinct_id,district_id,election_winner");
            for (Map.Entry<Precinct, Integer> entry : state.getPrecinctsAndDistrictsMap().entrySet()) {
                writer.print(entry.getKey().id());
                writer.print(',');
                writer.print(entry.getValue());
                writer.print(',');
                writer.println(winners.getOrDefault(entry.getValue(), "UNKNOWN"));
            }
        }
    }
}