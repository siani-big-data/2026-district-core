package siani.districting.architecture.model;

import java.io.Serializable;
import java.util.List;

public class District implements Serializable {


    private final Integer uniqueId;
    private final List<Precinct> precinctList;

    public District(Integer uniqueId, List<Precinct> precinctList) {
        this.uniqueId = uniqueId;
        this.precinctList = precinctList;
    }

    public Integer uniqueId() {
        return uniqueId;
    }

    public List<Precinct> precinctList() {
        return precinctList;
    }

    public int population() {
        return precinctList.stream().mapToInt(Precinct::getPopulation).sum();
    }

}
