package co.blueguardian.cerebralstratum.backend.controllers.devices;

import java.time.LocalDateTime;
import java.util.UUID;

public class Device {
    public UUID uuid;
    public String name;
    public String description;
    public LocalDateTime registered;
    public UUID keycloak_user_id;
    public UUID keycloak_org_id;
    public String image_path;
    public Status status;

    public Device() {
    }

    public Device(
            UUID uuid,
            String name,
            String description,
            LocalDateTime registered,
            UUID keycloak_user_id,
            UUID keycloak_org_id,
            String image_path,
            Status status
    ) {
        this.uuid = uuid;
        this.name = name;
        this.description = description;
        this.registered = registered;
        this.keycloak_user_id = keycloak_user_id;
        this.keycloak_org_id = keycloak_org_id;
        this.image_path = image_path;
        this.status = status;
    }
}
