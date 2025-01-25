package co.blueguardian.cerebralstratum.backend.repositories.users;

import co.blueguardian.cerebralstratum.backend.controllers.users.User;
import co.blueguardian.cerebralstratum.backend.controllers.users.CreateUserRequest;
import co.blueguardian.cerebralstratum.backend.controllers.users.DeleteUserRequest;
import co.blueguardian.cerebralstratum.backend.controllers.users.UpdateUserRequest;

import java.util.List;
import java.util.UUID;

public interface UserRepository {

    public List<User> findAll();

    public User getById(UUID id);

    public User getByKeycloakUserId(UUID keycloak_user_id);

    public User create(CreateUserRequest request);

    public User delete(DeleteUserRequest request);

    public User update(UpdateUserRequest request);
}