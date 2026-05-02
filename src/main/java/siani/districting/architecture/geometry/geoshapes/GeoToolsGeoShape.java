package siani.districting.architecture.geometry.geoshapes;

import org.locationtech.jts.geom.Geometry;
import siani.districting.architecture.geometry.GeoBox;
import siani.districting.architecture.geometry.GeoShape;
import siani.districting.architecture.geometry.geoboxes.GeoToolsGeoBox;

public class GeoToolsGeoShape implements GeoShape {

    private final Geometry geometry;
    private final GeoBox boundingBox;

    public GeoToolsGeoShape(Geometry geometry) {
        this.geometry = geometry;
        this.boundingBox = GeoToolsGeoBox.fromEnvelope(geometry.getEnvelopeInternal());
    }

    public static GeoShape of(Geometry geometry) {
        return new GeoToolsGeoShape(geometry);
    }

    @Override
    public GeoBox getBoundingBox() {
        return this.boundingBox;
    }

    @Override
    public boolean intersects(GeoShape other) {
        return this.geometry.intersects(unwrap(other));
    }

    private static Geometry unwrap(GeoShape shape) {
        if (!(shape instanceof GeoToolsGeoShape geoToolsGeoShape)) {
            throw new IllegalArgumentException(
                    "Unsupported GeoShape implementation: " + shape.getClass().getName()
            );
        }
        return geoToolsGeoShape.geometry;
    }
}