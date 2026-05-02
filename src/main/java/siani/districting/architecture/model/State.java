package siani.districting.architecture.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class  State implements Serializable {
    private final String name;
    private final List<District> districts;
    private final Map<Precinct, Integer> precinctsAndDistrictsMap;

    public State(String name, List<District> districts) {
        this.name = name;
        this.districts = districts;
        this.precinctsAndDistrictsMap = buildMap();
    }

    public String getName() {
        return name;
    }

    private Map<Precinct, Integer> buildMap() {
        Map<Precinct, Integer> map = new HashMap<>();
        for (District district : districts) {
            district.precinctList().forEach(precinct -> map.put(precinct, district.uniqueId()));
        }
        return map;
    }

    public Map<Precinct, Integer> getPrecinctsAndDistrictsMap() {
        return precinctsAndDistrictsMap;
    }

    public List<District> districts() {
        return districts;
    }

    public List<Precinct> precints() {
        List<Precinct> precincts = new ArrayList<>();
        for (District district : districts)
            precincts.addAll(district.precinctList());
        return precincts;
    }

}
