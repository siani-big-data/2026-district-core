package siani.districting.architecture.model;

import siani.districting.architecture.geometry.GeoShape;

import java.io.Serializable;

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
}
