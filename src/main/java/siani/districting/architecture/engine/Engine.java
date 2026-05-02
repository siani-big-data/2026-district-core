package siani.districting.architecture.engine;

import siani.districting.architecture.model.District;
import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Engine {

    private State lastState;

    public Engine() {}

    public  State getLastState() {
        return lastState;
    }

    public Map<Precinct, Integer> applyChangesToDistrict(Map<Precinct, Integer> changes) {
        State newState = new State(lastState.getName(), deepcopyPrecincts(lastState.districts()));
        for (Map.Entry<Precinct, Integer> change : changes.entrySet()) {
            Integer formerDistrictId = lastState.getPrecinctsAndDistrictsMap().get(change.getKey());
            updatePrecinctDistrictMap(change, newState);
            for (District district : newState.districts()) {
                if (Objects.equals(district.uniqueId(), formerDistrictId)) district.precinctList().remove(change.getKey());
                else if (Objects.equals(district.uniqueId(), change.getValue())) district.precinctList().add(change.getKey());
            }
        }
        lastState = newState;
        return changes;
    }

    public Map<Precinct, Integer> applyChangesToDistrict(Map<Precinct, Integer> changes, State formerState) {
        State newState = new State(formerState.getName(), deepcopyPrecincts(formerState.districts()));
        for (Map.Entry<Precinct, Integer> change : changes.entrySet()) {
            Integer formerDistrictId = formerState.getPrecinctsAndDistrictsMap().get(change.getKey());
            updatePrecinctDistrictMap(change, newState);
            for (District district : newState.districts()) {
                if (Objects.equals(district.uniqueId(), formerDistrictId)) district.precinctList().remove(change.getKey());
                else if (Objects.equals(district.uniqueId(), change.getValue())) district.precinctList().add(change.getKey());
            }
        }
        lastState = newState;
        return changes;
    }

    private List<District> deepcopyPrecincts(List<District> districts) {
        List<District> newDistricts = new ArrayList<>();
        for (District district : districts) {
            List<Precinct> newPrecincts = new ArrayList<>();
            newPrecincts.addAll(district.precinctList());
            District newDistrict = new District(district.uniqueId(), newPrecincts);
            newDistricts.add(newDistrict);
        }
        return newDistricts;
    }

    private static void updatePrecinctDistrictMap(Map.Entry<Precinct, Integer> entry, State newState) {
        newState.getPrecinctsAndDistrictsMap().put(entry.getKey(), entry.getValue());
    }


}
