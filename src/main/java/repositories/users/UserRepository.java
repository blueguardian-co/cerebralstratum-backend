package repositories.users;

import java.util.List;
import controllers.users.User;
import controllers.users.CreateUserRequest;
import controllers.users.DeleteUserRequest;
import controllers.users.UpdateUserRequest;

public interface UserRepository {

    public List<User> findAll();

    public User getById(int id);

    public User getByUsername(String username);

    public User create(CreateUserRequest request);

    public User delete(DeleteUserRequest request);

    public User update(UpdateUserRequest request);
}