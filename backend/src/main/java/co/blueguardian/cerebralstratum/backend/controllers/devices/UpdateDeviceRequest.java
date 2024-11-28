package co.blueguardian.cerebralstratum.backend.controllers.devices;

import java.util.List;

public class UpdateDeviceRequest {
    public String name;
    public String description;
    public Integer owner_id;
    public List<Integer> shared_users_read;
    public List<Integer> shared_users_modify;
    public Integer organisation_id;
    public String image_path;

    public UpdateDeviceRequest() {
    }

    public UpdateDeviceRequest(
            String name,
            String description,
            Integer owner_id,
            List<Integer> shared_users_read,
            List<Integer> shared_users_modify,
            Integer organisation_id,
            String image_path
    ) {
        this.name = name;
        this.description = description;
        this.owner_id = owner_id;
        this.shared_users_read = shared_users_read;
        this.shared_users_modify = shared_users_modify;
        this.organisation_id = organisation_id;
        this.image_path = image_path;
    }
}
