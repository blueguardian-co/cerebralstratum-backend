package co.blueguardian.cerebralstratum.utils.messaging;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public class LocationMessageDeserializer extends JsonDeserializer<LocationMessage> {

    @Override
    public LocationMessage deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        UUID deviceId = UUID.fromString(node.get("device_id").asText());

        GeometryFactory geometryFactory = new GeometryFactory();
        JsonNode coordinatesNode = node.get("coordinates");
        Point point;

        if (coordinatesNode.size() == 3) {
            double latitude = coordinatesNode.get(0).asDouble();
            double longitude = coordinatesNode.get(1).asDouble();
            double altitude = coordinatesNode.get(2).asDouble();
            point = geometryFactory.createPoint(new Coordinate(longitude, latitude, altitude));
        } else {
            double latitude = coordinatesNode.get(0).asDouble();
            double longitude = coordinatesNode.get(1).asDouble();
            point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        }

        int updateFrequency = node.get("update_frequency").asInt();
        int accuracy = node.get("accuracy").asInt();
        double speed = node.get("speed").asDouble();
        double bearing = node.get("bearing").asDouble();
        LocalDateTime timestamp = LocalDateTime.parse(node.get("timestamp").asText());

        LocationMessage locationMessage = new LocationMessage();
        locationMessage.device_id = deviceId;
        locationMessage.coordinates = point;
        locationMessage.update_frequency = updateFrequency;
        locationMessage.accuracy = accuracy;
        locationMessage.speed = speed;
        locationMessage.bearing = bearing;
        locationMessage.timestamp = timestamp;

        return locationMessage;
    }
}
