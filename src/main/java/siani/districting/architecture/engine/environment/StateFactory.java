package siani.districting.architecture.engine.environment;

import siani.districting.architecture.model.District;
import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;
import siani.districting.architecture.stores.StateDelta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class StateFactory {

    public static State applyDelta(State state, StateDelta delta) {
        return applyDeltaToState(state, delta);
    }

    private static State applyDeltaToState(State state, StateDelta delta) {
        List<District> clonedDistricts = state.districts().stream()
                .map(d -> new District(d.uniqueId(), new ArrayList<>(d.precinctList())))
                .collect(Collectors.toList());

        State newState = new State(state.getName(), clonedDistricts);
        Map<Precinct, Integer> newStateMap = newState.getPrecinctsAndDistrictsMap();
        Map<Precinct, Integer> deltaMap = delta.differencies();

        for (Precinct precinct : deltaMap.keySet()) {
            Integer oldDistrictId = newStateMap.get(precinct);
            Integer newDistrictId = deltaMap.get(precinct);
            if (!Objects.equals(oldDistrictId, newDistrictId)) {
                adjustPrecinct(newState, precinct, oldDistrictId, newDistrictId);
                newStateMap.put(precinct, newDistrictId);
            }
        }
        return newState;
    }

    private static void adjustPrecinct(State newState, Precinct precinct, Integer oldDistrictId, Integer newDistrictId) {
        for (District district : newState.districts()) {
            if (Objects.equals(district.uniqueId(), oldDistrictId)) {
                district.precinctList().remove(precinct);
            } else if (Objects.equals(district.uniqueId(), newDistrictId)) {
                district.precinctList().add(precinct);
            }
        }
    }

}
