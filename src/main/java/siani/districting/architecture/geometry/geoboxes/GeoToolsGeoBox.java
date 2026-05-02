package siani.districting.architecture.geometry.geoboxes;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import siani.districting.architecture.geometry.GeoBox;

public record GeoToolsGeoBox( double minX, double maxX,
                                double minY, double maxY ) implements GeoBox {

    public static GeoToolsGeoBox fromEnvelope(Envelope envelope) {
        return new GeoToolsGeoBox(
                envelope.getMinX(),
                envelope.getMaxX(),
                envelope.getMinY(),
                envelope.getMaxY()
        );
    }

}
