package siani.districting.architecture.engine.environment;

import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.model.District;
import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MatrixMultiplicationBoundaryCalculator {
    private Set<String> differentOwnershipMatrix;
    private AdjacencySolver solver;

    private Map<Integer, Map<Integer, Set<Precinct>>> currentBoundariesMap;

    public MatrixMultiplicationBoundaryCalculator() {}

    public Map<Integer, Map<Integer, Set<Precinct>>> calculateBoundariesForFirstTime(State state, AdjacencySolver solver) {
        Map<Integer, Map<Integer, Set<Precinct>>> boundaryPrecincts = new HashMap<>();
        this.solver = solver;
        this.currentBoundariesMap = new HashMap<>(); // Inicializamos el mapa
        differentOwnershipMatrix = calculateDifferentOwnerMatrix(state, solver);
        return calculateBoundariesForEachDistrict(state, solver, boundaryPrecincts);
    }

    private Map<Integer, Map<Integer, Set<Precinct>>> calculateBoundariesForEachDistrict(
            State state,
            AdjacencySolver solver,
            Map<Integer, Map<Integer, Set<Precinct>>> boundaryPrecincts)
    {
        long start = System.currentTimeMillis();
        state.districts().forEach(district -> {

            boundaryPrecincts.put(district.uniqueId(), calculateBoundariesOfDistrict(state, district, solver, differentOwnershipMatrix));

        } );
        System.out.println("Tiempo de construcción del mapa: " + (System.currentTimeMillis() - start) + " ms");
        currentBoundariesMap = boundaryPrecincts;
        return boundaryPrecincts;
    }

    private Map<Integer, Set<Precinct>> calculateBoundariesOfDistrict(State state, District district, AdjacencySolver solver, Set<String> differentOwnershipMatrix) {
        Map<Integer, Set<Precinct>> boundariesPerNeighbourDistrictMap = new ConcurrentHashMap<>();
        district.precinctList().parallelStream().forEach(precinct -> {
            List<Precinct> adjacents = solver.getAdjacents(precinct);
            for (Precinct adjacent : adjacents) {
                if (differentOwnershipMatrix.contains(sortIds(precinct,adjacent))) {
                    int adjacentDistrictId = state.getPrecinctsAndDistrictsMap().get(adjacent);
                    boundariesPerNeighbourDistrictMap.computeIfAbsent(adjacentDistrictId,
                            k -> ConcurrentHashMap.newKeySet()).add(precinct);
                }
            }
        });
        return boundariesPerNeighbourDistrictMap;
    }

    private Set<String> calculateDifferentOwnerMatrix(State state, AdjacencySolver solver) {
        long start = System.currentTimeMillis();
        Set<String> differentOwnershipMatrix = new HashSet<>();

        state.precints().parallelStream().forEach(precinct -> {
            solver.getAdjacents(precinct).forEach(adjacent -> {
                if (!Objects.equals(state.getPrecinctsAndDistrictsMap().get(precinct), state.getPrecinctsAndDistrictsMap().get(adjacent))) {
                    differentOwnershipMatrix.add(sortIds(precinct, adjacent));
                }
            });
        });
        System.out.println("tiempo calculo diff ownership: " + (System.currentTimeMillis() - start) + " ms." );
        return differentOwnershipMatrix;
    }

    private String sortIds(Precinct precinct1, Precinct precinct2) {
        return precinct1.id().compareTo(precinct2.id()) < 0 ? precinct1.id() + "," + precinct2.id()
                : precinct2.id() + "," + precinct1.id();
    }

    // ----------UPDATE-MATRIX---------------

    public Map<Integer, Map<Integer, Set<Precinct>>> updateMatrix(State newState, Set<Precinct> changes) {
        long start = System.currentTimeMillis();

        for (Precinct precinct : changes) {
            deleteAllEntries(precinct);
            calculateNewEntriesOwnership(newState, precinct);
        }

        Set<Precinct> affectedPrecincts = new HashSet<>(changes);
        for (Precinct p : changes) {
            affectedPrecincts.addAll(solver.getAdjacents(p));
        }

        for (Map<Integer, Set<Precinct>> neighborsMap : currentBoundariesMap.values()) {
            for (Set<Precinct> borderSet : neighborsMap.values()) {
                borderSet.removeAll(affectedPrecincts);
            }
        }

        for (Precinct p : affectedPrecincts) {
            Integer myDistrictId = newState.getPrecinctsAndDistrictsMap().get(p);

            for (Precinct neighbor : solver.getAdjacents(p)) {
                Integer neighborDistrictId = newState.getPrecinctsAndDistrictsMap().get(neighbor);

                if (!neighborDistrictId.equals(myDistrictId)) {

                    currentBoundariesMap
                            .computeIfAbsent(myDistrictId, k -> new HashMap<>())
                            .computeIfAbsent(neighborDistrictId, k -> new HashSet<>())
                            .add(p);
                }
            }
        }

        System.out.println("Updating time: " + (System.currentTimeMillis() - start) + " ms");

        return currentBoundariesMap;
    }

    private void calculateNewEntriesOwnership(State newState, Precinct precinct) {
        solver.getAdjacents(precinct).forEach(adjacent -> {
            if (!Objects.equals(newState.getPrecinctsAndDistrictsMap().get(precinct), newState.getPrecinctsAndDistrictsMap().get(adjacent))) {
                differentOwnershipMatrix.add(sortIds(precinct, adjacent));
            }
        });
    }

    private void deleteAllEntries(Precinct precinct) {
        solver.getAdjacents(precinct).forEach(adjacent -> {
            differentOwnershipMatrix.remove(sortIds(precinct,adjacent));
        });
    }

}
