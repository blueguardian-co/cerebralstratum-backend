package co.blueguardian.cerebralstratum.utils.model;

import com.fasterxml.jackson.databind.module.SimpleModule;

import org.locationtech.jts.geom.Point;

public class GeometryJacksonModule extends SimpleModule {

    public GeometryJacksonModule() {
        super("GeometryJacksonModule");
        addSerializer(Point.class, new PointSerializer());
        addDeserializer(Point.class, new PointDeserializer());
    }
}
