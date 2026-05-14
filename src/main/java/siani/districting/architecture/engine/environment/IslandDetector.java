package siani.districting.architecture.engine.environment;

import org.jspecify.annotations.NonNull;
import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.engine.Action;
import siani.districting.architecture.engine.actions.BuyAction;
import siani.districting.architecture.model.District;
import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;

import java.util.*;

public class IslandDetector {

    private IslandDetector() {
    }

    public static List<Action> findIslands(State state,
                                           AdjacencySolver solver,
                                           List<Action> actionList) {

        Map<Precinct, Integer> runningOwnerMap = new HashMap<>(state.getPrecinctsAndDistrictsMap());
        Map<Integer, Set<Precinct>> runningDistrictsMap = new HashMap<>();
        for (District d : state.districts()) {
            runningDistrictsMap.put(d.uniqueId(), new HashSet<>(d.precinctList()));
        }

        List<Action> finalActions = new ArrayList<>();

        for (Action action : actionList) {

            Precinct boughtPrecinct = action.precinct();
            Integer buyingDistrictId = action.districtId();
            Integer sellingDistrictId = runningOwnerMap.get(boughtPrecinct);

            if (sellingDistrictId == null || sellingDistrictId.equals(buyingDistrictId)) continue;

            boolean isContiguousToBuyer = false;
            for (Precinct neighbor : solver.getAdjacents(boughtPrecinct)) {
                if (buyingDistrictId.equals(runningOwnerMap.get(neighbor))) {
                    isContiguousToBuyer = true;
                    break;
                }
            }

            if (!isContiguousToBuyer) {
                continue;
            }

            finalActions.add(action);

            runningOwnerMap.put(boughtPrecinct, buyingDistrictId);
            runningDistrictsMap.get(sellingDistrictId).remove(boughtPrecinct);
            runningDistrictsMap.computeIfAbsent(buyingDistrictId, k -> new HashSet<>()).add(boughtPrecinct);

            Set<Precinct> sellerPrecincts = runningDistrictsMap.get(sellingDistrictId);
            if (sellerPrecincts.isEmpty()) continue;

            List<Precinct> sellerNeighbors = solver.getAdjacents(boughtPrecinct).stream()
                    .filter(sellerPrecincts::contains)
                    .toList();

            if (sellerNeighbors.size() <= 1) continue;

            List<Set<Precinct>> directNeighborsGroups = agglutinateBfs(solver, sellerNeighbors, sellerPrecincts, true);

            if (directNeighborsGroups.size() == 1) continue;

            List<Set<Precinct>> components = agglutinateBfs(solver, sellerNeighbors, sellerPrecincts, false);

            if (components.size() > 1) {
                components.sort((c1, c2) -> Integer.compare(c2.size(), c1.size()));

                for (int i = 1; i < components.size(); i++) {
                    Set<Precinct> island = components.get(i);
                    for (Precinct islandPrecinct : island) {
                        finalActions.add(new BuyAction(buyingDistrictId, islandPrecinct));

                        runningOwnerMap.put(islandPrecinct, buyingDistrictId);
                        sellerPrecincts.remove(islandPrecinct);
                        runningDistrictsMap.get(buyingDistrictId).add(islandPrecinct);
                    }
                }
            }
        }

        return finalActions;
    }

    private static List<Set<Precinct>> agglutinateBfs(AdjacencySolver solver, List<Precinct> sellerNeighbors, Set<Precinct> sellerPrecincts, boolean onlyDirectNeighbors) {
        List<Set<Precinct>> components = new ArrayList<>();
        Set<Precinct> unvisitedNeighbors = new HashSet<>(sellerNeighbors);
        Set<Precinct> globalVisited = new HashSet<>();
        Set<Precinct> allowedBoundary = onlyDirectNeighbors ? new HashSet<>(sellerNeighbors) : sellerPrecincts;

        while (!unvisitedNeighbors.isEmpty()) {
            Precinct startNode = unvisitedNeighbors.iterator().next();
            Set<Precinct> currentComponent = new HashSet<>();
            Queue<Precinct> queue = new ArrayDeque<>();

            queue.add(startNode);
            globalVisited.add(startNode);
            currentComponent.add(startNode);

            while (!queue.isEmpty()) {
                Precinct current = queue.poll();
                for (Precinct neighbor : solver.getAdjacents(current)) {
                    if (allowedBoundary.contains(neighbor) && !globalVisited.contains(neighbor)) {
                        globalVisited.add(neighbor);
                        currentComponent.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
            unvisitedNeighbors.removeAll(currentComponent);
            components.add(currentComponent);
        }
        return components;
    }
}
