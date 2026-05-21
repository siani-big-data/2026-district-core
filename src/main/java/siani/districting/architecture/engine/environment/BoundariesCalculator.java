package siani.districting.architecture.engine.environment;


import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.model.District;
import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BoundariesCalculator {
    private BoundariesCalculator() {}

    public static Map<Integer, Map<Integer, Set<Precinct>>> calculateBoundariesOf(State state, AdjacencySolver solver) {
        Map<Integer, Map<Integer, Set<Precinct>>> boundaryPrecincts = new HashMap<>();
        return getBoundariesInsideState(state, solver, boundaryPrecincts);
    }

    private static Map<Integer, Map<Integer, Set<Precinct>>> getBoundariesInsideState (
            State state,
            AdjacencySolver solver,
            Map<Integer, Map<Integer, Set<Precinct>>> districtBoundariesMap
    ) {
        for (District district : state.districts()) {
            Map<Integer, Set<Precinct>> boundaryPrecincts = getBoundaryPrecinctsOfDistrict(district, state, solver);
            districtBoundariesMap.put(district.uniqueId(), boundaryPrecincts);
        }
        return districtBoundariesMap;
    }

    private static Map<Integer, Set<Precinct>> getBoundaryPrecinctsOfDistrict(District district, State state, AdjacencySolver solver) {
        Map<Integer, Set<Precinct>> boundariesWithOtherDistrictsMap = new HashMap<>();
        district.precinctList().parallelStream()
                .filter(p -> precinctIsBoundary(state, solver, p))
                .forEach(p -> putPrecinctInMap(state, district, p, solver, boundariesWithOtherDistrictsMap));
        return boundariesWithOtherDistrictsMap;
    }

    private static void putPrecinctInMap(State state, District district, Precinct precinct, AdjacencySolver solver, Map<Integer, Set<Precinct>> boundariesWithOtherDistrictsMap) {
        solver.getAdjacents(precinct).stream()
                .map(adjacent -> state.getPrecinctsAndDistrictsMap().get(adjacent))
                .distinct()
                .filter(districtId -> !Objects.equals(districtId, district.uniqueId()))
                .forEach(districtId -> boundariesWithOtherDistrictsMap.computeIfAbsent(districtId, k -> ConcurrentHashMap.newKeySet()).add(precinct));
    }

    private static boolean precinctIsBoundary(State state, AdjacencySolver solver, Precinct current) {
        return solver.getAdjacents(current).stream()
                .map(adjacent -> state.getPrecinctsAndDistrictsMap().get(adjacent))
                .distinct()
                .count() > 1;
    }

}
