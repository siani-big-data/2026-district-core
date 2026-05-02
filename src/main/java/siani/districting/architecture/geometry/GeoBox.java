package siani.districting.architecture.geometry;

import java.io.Serializable;

public interface GeoBox extends Serializable {

    double minX();
    double minY();
    double maxX();
    double maxY();

}