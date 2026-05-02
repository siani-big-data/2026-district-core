package siani.districting.architecture.geometry.geotrees;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;
import siani.districting.architecture.geometry.GeoBox;
import siani.districting.architecture.geometry.GeoTree;

import java.util.List;

public class GeoToolsGeoTree<T> implements GeoTree<T> {

    private final STRtree tree;

    public GeoToolsGeoTree() {
        this.tree = new STRtree();
    }

    public GeoToolsGeoTree(STRtree tree) {
        this.tree = tree;
    }

    @Override
    public void insert(GeoBox box, T object) {
        this.tree.insert(toEnvelope(box), object);
    }

    @Override
    public void build() {
        this.tree.build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> query(GeoBox box) {
        return this.tree.query(toEnvelope(box));
    }

    private Envelope toEnvelope(GeoBox box) {
        return new Envelope(
                box.minX(),
                box.maxX(),
                box.minY(),
                box.maxY()
        );
    }
}