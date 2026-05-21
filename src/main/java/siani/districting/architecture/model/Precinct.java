package siani.districting.architecture.model;

import siani.districting.architecture.geometry.GeoShape;

import java.io.Serializable;
import java.util.Objects;

public class Precinct implements Serializable {

    private final String id;
    private final GeoShape shape;
    private int population;

    public Precinct(String id, GeoShape shape) {
        this.id = id;
        this.shape = shape;
    }

    public String id() {
        return id;
    }

    public GeoShape boundaries() {
        return shape;
    }


    public int getPopulation() {
        return population;
    }

    public void setPopulation(int population) {
        this.population = population;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Precinct precinct)) return false;
        return Objects.equals(id, precinct.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
