package co.blueguardian.cerebralstratum.utils.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

import org.locationtech.jts.geom.Point;

public class PointSerializer extends JsonSerializer<Point> {

    @Override
    public void serialize(Point point, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        double altitude = point.getCoordinate().getZ();

        jsonGenerator.writeStartArray();
        jsonGenerator.writeNumber(point.getY());
        jsonGenerator.writeNumber(point.getX());
        if (!Double.isNaN(altitude)) {
            jsonGenerator.writeNumber(altitude);
        }
        jsonGenerator.writeEndArray();
    }
}
