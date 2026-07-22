package co.blueguardian.cerebralstratum.utils.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public class PointDeserializer extends JsonDeserializer<Point> {

    @Override
    public Point deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        double latitude = node.get(0).asDouble();
        double longitude = node.get(1).asDouble();

        GeometryFactory geometryFactory = new GeometryFactory();
        Coordinate coordinate = node.size() == 3
                ? new Coordinate(longitude, latitude, node.get(2).asDouble())
                : new Coordinate(longitude, latitude);

        return geometryFactory.createPoint(coordinate);
    }
}
