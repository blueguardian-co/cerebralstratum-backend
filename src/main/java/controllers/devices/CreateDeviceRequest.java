package controllers.devices;

import repositories.users.UserEntity;


public class CreateDeviceRequest {

    public String name;
    public String description;
    public String image_path;

    public CreateDeviceRequest() {
    }

    public CreateDeviceRequest(
            String name,
            String description,
            String image_path
    ) {
        this.name = name;
        this.description = description;
        this.image_path = image_path;
    }
}
