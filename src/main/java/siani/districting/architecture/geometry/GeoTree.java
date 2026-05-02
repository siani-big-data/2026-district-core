package siani.districting.architecture.geometry;

import java.io.Serializable;
import java.util.List;

public interface GeoTree<T> extends Serializable {

    void insert(GeoBox box, T object);

    void build();

    List<T> query(GeoBox box);

}