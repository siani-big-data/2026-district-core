package siani.districting.architecture.geometry;

import java.io.Serializable;

public interface GeoShape extends Serializable {

    GeoBox getBoundingBox();

    boolean intersects(GeoShape other);

}