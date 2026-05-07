package siani.districting.architecture.engine.environment;

import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;
import siani.districting.architecture.precinctinfo.PrecinctInfoContainer;
import siani.districting.readers.CsvToMapReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ElectionCalculator {

    private ElectionCalculator() {
    }

    public static Map<Integer, String> calculateWinnersPerDistrict(State state, PrecinctInfoContainer container, Map<Object, Object> representantToPartyMap) {
        Map<Integer, String> winnersPerDistrictMap = new ConcurrentHashMap<>();
        state.districts().parallelStream().forEach(district -> {
            Map<String, Integer> votesPerPartyMap = new HashMap<>();
            for (Precinct precint : district.precinctList()) {
                for (String column : container.getColumnNames()) {
                    Integer precinctValue = container.getValueOf(precint.id(), column);
                    if (precinctValue != null) {
                        votesPerPartyMap.merge(column, precinctValue, Integer::sum);
                    }
                }
            }

            votesPerPartyMap.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .ifPresent(maxEntry -> {
                        Object party = representantToPartyMap.get(maxEntry.getKey());
                        winnersPerDistrictMap.put(district.uniqueId(), party != null ? party.toString() : "UNKNOWN");
                    });
        });
        return winnersPerDistrictMap;
    }
}
