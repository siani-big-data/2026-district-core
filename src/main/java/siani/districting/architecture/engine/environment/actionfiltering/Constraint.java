package siani.districting.architecture.engine.environment.actionfiltering;

import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.engine.Action;
import siani.districting.architecture.model.District;
import siani.districting.architecture.model.State;

import java.util.List;

public enum Constraint implements ConstraintCommand {

    MIN_PRECINCTS {
        @Override
        public List<Action> filter(State state,
                                   AdjacencySolver solver,
                                   List<Action> actionList,
                                   int simulationStep) {
            if (actionList.isEmpty()) return actionList;
            return actionList.parallelStream().filter(action ->  {
                int sellingDistrictId = state.getPrecinctsAndDistrictsMap().get(action.precinct());
                District district = state.districts()
                        .stream()
                        .filter(d -> d.uniqueId() == sellingDistrictId).findAny().get();
                return district.precinctList().size() > 100;
            }).toList();
        }
    },

    MAX_PRECINCTS {
        @Override
        public List<Action> filter(State state,
                                   AdjacencySolver solver,
                                   List<Action> actionList,
                                   int simulationStep) {
            if (actionList.isEmpty()) return actionList;

            return actionList.parallelStream().filter(action ->  {
                District district = state.districts().stream()
                        .filter(d -> d.uniqueId() == action.districtId())
                        .findAny().get();
                return district.precinctList().size() < 500;
            }).toList();
        }
    },

}
