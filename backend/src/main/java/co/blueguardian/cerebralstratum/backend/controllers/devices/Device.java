package co.blueguardian.cerebralstratum.backend.controllers.devices;

import co.blueguardian.cerebralstratum.backend.repositories.organisations.OrganisationEntity;
import co.blueguardian.cerebralstratum.backend.repositories.users.UserEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Device {

    public Integer id;
    public String name;
    public UUID uuid;
    public String description;
    public LocalDateTime registered;
    public int owner_id;
    public int organisation_id;
    public String image_path;
    public Status status;

    public Device() {
    }

    public Device(
            Integer id,
            String name,
            UUID uuid,
            String description,
            LocalDateTime registered,
            int owner_id,
            int organisation_id,
            String image_path,
            Status status
    ) {
        this.id = id;
        this.name = name;
        this.uuid = uuid;
        this.description = description;
        this.registered = registered;
        this.owner_id = owner_id;
        this.organisation_id = organisation_id;
        this.image_path = image_path;
        this.status = status;
    }
}
