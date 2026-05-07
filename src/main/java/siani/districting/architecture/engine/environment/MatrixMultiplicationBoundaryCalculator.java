package siani.districting.architecture.engine.environment;

import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.model.District;
import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MatrixMultiplicationBoundaryCalculator {
    private AdjacencySolver solver;

    private Map<Integer, Map<Integer, Set<Precinct>>> currentBoundariesMap;

    public MatrixMultiplicationBoundaryCalculator() {}

    public Map<Integer, Map<Integer, Set<Precinct>>> calculateBoundariesForFirstTime(State state, AdjacencySolver solver) {
        this.solver = solver;
        this.currentBoundariesMap = new ConcurrentHashMap<>(); // Hacemos el mapa principal concurrente
        return calculateBoundariesForEachDistrict(state);
    }

    private Map<Integer, Map<Integer, Set<Precinct>>> calculateBoundariesForEachDistrict(State state)
    {
        long start = System.currentTimeMillis();
        state.districts().forEach(district -> {
            currentBoundariesMap.put(district.uniqueId(), calculateBoundariesOfDistrict(state, district));
        } );
        System.out.println("Tiempo de construcción del mapa: " + (System.currentTimeMillis() - start) + " ms");
        return currentBoundariesMap;
    }

    private Map<Integer, Set<Precinct>> calculateBoundariesOfDistrict(State state, District district) {
        Map<Integer, Set<Precinct>> boundariesPerNeighbourDistrictMap = new ConcurrentHashMap<>();
        Map<Precinct, Integer> stateMap = state.getPrecinctsAndDistrictsMap();

        district.precinctList().parallelStream().forEach(precinct -> {
            List<Precinct> adjacents = solver.getAdjacents(precinct);
            for (Precinct adjacent : adjacents) {
                Integer adjacentDistrictId = stateMap.get(adjacent);
                if (!adjacentDistrictId.equals(district.uniqueId())) {
                    boundariesPerNeighbourDistrictMap.computeIfAbsent(adjacentDistrictId,
                            k -> ConcurrentHashMap.newKeySet()).add(precinct);
                }
            }
        });
        return boundariesPerNeighbourDistrictMap;
    }

    // ----------UPDATE-MATRIX---------------

    public Map<Integer, Map<Integer, Set<Precinct>>> updateMatrix(State newState, Set<Precinct> changes) {
        long start = System.currentTimeMillis();

        Set<Precinct> affectedPrecincts = new HashSet<>(changes);
        for (Precinct p : changes) {
            affectedPrecincts.addAll(solver.getAdjacents(p));
        }

        for (Map<Integer, Set<Precinct>> neighborsMap : currentBoundariesMap.values()) {
            for (Set<Precinct> borderSet : neighborsMap.values()) {
                borderSet.removeAll(affectedPrecincts);
            }

            neighborsMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        }

        Map<Precinct, Integer> stateMap = newState.getPrecinctsAndDistrictsMap();

        for (Precinct p : affectedPrecincts) {
            Integer disctrictId = stateMap.get(p);

            for (Precinct neighbor : solver.getAdjacents(p)) {
                Integer neighborDistrictId = stateMap.get(neighbor);

                if (!neighborDistrictId.equals(disctrictId)) {

                    currentBoundariesMap
                            .computeIfAbsent(disctrictId, k -> new ConcurrentHashMap<>())
                            .computeIfAbsent(neighborDistrictId, k -> ConcurrentHashMap.newKeySet())
                            .add(p);
                }
            }
        }

        System.out.println("Updating time: " + (System.currentTimeMillis() - start) + " ms");

        return currentBoundariesMap;
    }

}
