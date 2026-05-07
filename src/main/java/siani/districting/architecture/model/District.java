package siani.districting.architecture.model;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public class District implements Serializable {


    private final Integer uniqueId;
    private final List<Precinct> precinctList;
    private final int population;

    public District(Integer uniqueId, List<Precinct> precinctList) {
        this.uniqueId = uniqueId;
        this.precinctList = precinctList;
        this.population = precinctList.stream().map(Precinct::getPopulation).reduce(0, Integer::sum);
    }

    public Integer uniqueId() {
        return uniqueId;
    }

    public List<Precinct> precinctList() {
        return precinctList;
    }

    public int population() {
        return population;
    }

}
