package siani.districting.architecture.engine.environment;

import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.engine.Action;
import siani.districting.architecture.engine.actions.BuyAction;
import siani.districting.architecture.model.District;
import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IslandDetector {

    private IslandDetector() {
    }

    public static List<Action> findIslands(State state,
                                    AdjacencySolver solver,
                                    List<Action> actionList) {

            Map<Integer, District> districtMap = state.districts().stream()
                    .collect(Collectors.toMap(District::uniqueId, d -> d));
            Map<Precinct, Integer> precinctOwnerMap = state.getPrecinctsAndDistrictsMap();

            List<Action> islandActions = actionList.parallelStream().flatMap(action -> {
                Integer sellingDistrictId = precinctOwnerMap.get(action.precinct());
                Integer buyingDistrictId = action.districtId();

                if (sellingDistrictId == null || sellingDistrictId.equals(buyingDistrictId)) {
                    return Stream.empty();
                }

                District sellingDistrict = districtMap.get(sellingDistrictId);
                Set<Precinct> remainingPrecincts = new HashSet<>(sellingDistrict.precinctList());
                remainingPrecincts.remove(action.precinct());

                if (remainingPrecincts.isEmpty()) {
                    return Stream.empty();
                }

                Optional<Precinct> startNodeOpt = remainingPrecincts.stream().findFirst();
                if (startNodeOpt.isEmpty()) {
                    return Stream.empty();
                }
                Precinct startNode = startNodeOpt.get();

                ArrayDeque<Precinct> queue = new ArrayDeque<>();
                Set<Precinct> visited = new HashSet<>();
                queue.add(startNode);
                visited.add(startNode);

                while (!queue.isEmpty()) {
                    Precinct current = queue.poll();

                    for (Precinct currentNeighbor : solver.getAdjacents(current)) {
                        if (remainingPrecincts.contains(currentNeighbor) && !visited.contains(currentNeighbor)) {
                            visited.add(currentNeighbor);
                            queue.add(currentNeighbor);
                        }
                    }
                }

                if (visited.size() < remainingPrecincts.size()) {
                    Set<Precinct> island;
                    if (visited.size() < remainingPrecincts.size() / 2.0) {
                        island = visited;
                    } else {
                        remainingPrecincts.removeAll(visited);
                        island = remainingPrecincts;
                    }

                    return island.stream().map(p -> new BuyAction(buyingDistrictId, p));
                }
                
                return Stream.empty();
            }).collect(Collectors.toList());

            List<Action> finalActions = new ArrayList<>(actionList);
            finalActions.addAll(islandActions);
            return finalActions;
    }
}
