package siani.districting.architecture.stores.writer;

import siani.districting.architecture.engine.environment.ElectionCalculator;
import siani.districting.architecture.engine.environment.actionfiltering.ActionFilter;
import siani.districting.architecture.model.District;
import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;
import siani.districting.architecture.precinctinfo.PrecinctInfoContainer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
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

    public static void exportWithWinnersPerDistrictAndPopulation(State state, String path, PrecinctInfoContainer container, Map<Object, Object> candidateToPartyMap) throws IOException {
        Map<Integer, String> winners = ElectionCalculator.calculateWinnersPerDistrict(state, container, candidateToPartyMap);
        Map<Integer, Integer> districtPopulations = calculateDistrictPopulations(state);
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            writer.println("precinct_id,district_id,election_winner,district_population");
            for (Map.Entry<Precinct, Integer> entry : state.getPrecinctsAndDistrictsMap().entrySet()) {
                Integer districtId = entry.getValue();
                writer.print(entry.getKey().id());
                writer.print(',');
                writer.print(districtId);
                writer.print(',');
                writer.print(winners.getOrDefault(districtId, "UNKNOWN"));
                writer.print(',');
                writer.println(districtPopulations.getOrDefault(districtId, 0));
            }
        }
    }

    public static void exportWithWinnersPerDistrictAndPhase(State state, String path, PrecinctInfoContainer container, Map<Object, Object> candidateToPartyMap, ActionFilter.PhaseName PhaseName) throws IOException {
        Map<Integer, String> winners = ElectionCalculator.calculateWinnersPerDistrict(state, container, candidateToPartyMap);
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            writer.println("precinct_id,district_id,election_winner,phase");
            String PhaseStr = PhaseName != null ? PhaseName.name() : "UNKNOWN";
            for (Map.Entry<Precinct, Integer> entry : state.getPrecinctsAndDistrictsMap().entrySet()) {
                writer.print(entry.getKey().id());
                writer.print(',');
                writer.print(entry.getValue());
                writer.print(',');
                writer.print(winners.getOrDefault(entry.getValue(), "UNKNOWN"));
                writer.print(',');
                writer.println(PhaseStr);
            }
        }
    }

    public static void exportWithWinnersPerDistrictPhaseAndPopulation(State state, String path, PrecinctInfoContainer container, Map<Object, Object> candidateToPartyMap, ActionFilter.PhaseName PhaseName) throws IOException {
        Map<Integer, String> winners = ElectionCalculator.calculateWinnersPerDistrict(state, container, candidateToPartyMap);
        Map<Integer, Integer> districtPopulations = calculateDistrictPopulations(state);
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            writer.println("precinct_id,district_id,election_winner,phase,district_population");
            String PhaseStr = PhaseName != null ? PhaseName.name() : "UNKNOWN";
            for (Map.Entry<Precinct, Integer> entry : state.getPrecinctsAndDistrictsMap().entrySet()) {
                Integer districtId = entry.getValue();
                writer.print(entry.getKey().id());
                writer.print(',');
                writer.print(districtId);
                writer.print(',');
                writer.print(winners.getOrDefault(districtId, "UNKNOWN"));
                writer.print(',');
                writer.print(PhaseStr);
                writer.print(',');
                writer.println(districtPopulations.getOrDefault(districtId, 0));
            }
        }
    }

    private static Map<Integer, Integer> calculateDistrictPopulations(State state) {
        Map<Integer, Integer> districtPopulations = new HashMap<>();
        for (District district : state.districts()) {
            districtPopulations.put(district.uniqueId(), district.population());
        }
        return districtPopulations;
    }
}
