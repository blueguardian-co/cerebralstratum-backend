package co.blueguardian.cerebralstratum.backend.controllers.devices;

import java.util.UUID;

public class UpdateDeviceRequest {
    public String name;
    public String description;
    public UUID keycloak_user_id;
    public UUID keycloak_org_id;
    public String image_path;

    public UpdateDeviceRequest() {
    }

    public UpdateDeviceRequest(
            String name,
            String description,
            UUID keycloak_user_id,
            UUID keycloak_org_id,
            String image_path
    ) {
        this.name = name;
        this.description = description;
        this.keycloak_user_id = keycloak_user_id;
        this.keycloak_org_id = keycloak_org_id;
        this.image_path = image_path;
    }
}
