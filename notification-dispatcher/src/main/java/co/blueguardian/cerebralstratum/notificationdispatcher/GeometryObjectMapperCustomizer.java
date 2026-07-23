package co.blueguardian.cerebralstratum.notificationdispatcher;

import co.blueguardian.cerebralstratum.utils.model.GeometryJacksonModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

@Singleton
public class GeometryObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.registerModule(new GeometryJacksonModule());
    }
}