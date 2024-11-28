package co.blueguardian.cerebralstratum.backend.repositories.users;

import co.blueguardian.cerebralstratum.backend.controllers.users.User;
import co.blueguardian.cerebralstratum.backend.controllers.users.CreateUserRequest;
import co.blueguardian.cerebralstratum.backend.controllers.users.DeleteUserRequest;
import co.blueguardian.cerebralstratum.backend.controllers.users.UpdateUserRequest;

import java.util.List;

public interface UserRepository {

    public List<User> findAll();

    public User getById(int id);

    public User getByUsername(String username);

    public User create(CreateUserRequest request);

    public User delete(DeleteUserRequest request);

    public User update(UpdateUserRequest request);
}