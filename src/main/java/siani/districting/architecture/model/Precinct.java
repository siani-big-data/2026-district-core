package siani.districting.architecture.model;

import siani.districting.architecture.geometry.GeoShape;

import java.io.Serializable;

public record Precinct(String id, GeoShape boundaries) implements Serializable {
}
