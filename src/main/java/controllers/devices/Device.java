package controllers.devices;

import repositories.organisations.OrganisationEntity;
import repositories.users.UserEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Device {

    public Integer id;
    public String name;
    public UUID uuid;
    public String description;
    public LocalDateTime registered;
    public UserEntity owner_id;
    public List<Integer> shared_users_read;
    public List<Integer> shared_users_modify;
    public OrganisationEntity organisation_id;
    public String image_path;
    public Set<Status> status;

    public Device() {
    }

    public Device(
            Integer id,
            String name,
            UUID uuid,
            String description,
            LocalDateTime registered,
            UserEntity owner_id,
            List<Integer> shared_users_read,
            List<Integer> shared_users_modify,
            OrganisationEntity organisation,
            String image_path,
            Set<Status> status
    ) {
        this.id = id;
        this.name = name;
        this.uuid = uuid;
        this.description = description;
        this.registered = registered;
        this.owner_id = owner_id;
        this.shared_users_read = shared_users_read;
        this.shared_users_modify = shared_users_modify;
        this.organisation_id = organisation_id;
        this.image_path = image_path;
        this.status = status;
    }
}
