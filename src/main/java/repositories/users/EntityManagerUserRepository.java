package repositories.users;

import controllers.users.User;
import controllers.users.CreateUserRequest;
import controllers.users.DeleteUserRequest;
import controllers.users.UpdateUserRequest;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Default
    @ApplicationScoped
public class EntityManagerUserRepository implements UserRepository {

    @Inject
    EntityManager entityManager;

    private static User mapEntityToUser (UserEntity user) {
        return new User(
            user.getId(),
            user.getUsername(),
            user.getFirst_name(),
            user.getLast_name(),
            user.getTable_number()
        );
    }

    private static UserEntity mapCreateRequestToEntity (CreateUserRequest request) {
        return new UserEntity(
                request.username,
                request.first_name,
                request.last_name,
                request.table_number
        );
    }

    private static UserEntity mapUpdateRequestToEntity (UpdateUserRequest request) {
        return new UserEntity(
            request.username,
            request.first_name,
            request.last_name,
            request.table_number
        );
    }

    public List<User> findAll() {
        return entityManager.createNamedQuery("UserEntity.findAll", UserEntity.class)
            .getResultList().stream().map(EntityManagerUserRepository::mapEntityToUser).collect(Collectors.toList());          
    }

    public User getById(int id) {
        UserEntity user = entityManager.find(UserEntity.class, id);
        return mapEntityToUser(user);
    }

    public User getByUsername(String username) {
        try {
            UserEntity user = entityManager.createNamedQuery("UserEntity.getUser", UserEntity.class)
                    .setParameter("username", username)
                    .getSingleResult();
            return mapEntityToUser(user);
        } catch (jakarta.persistence.NoResultException e) {
            return null;
        }
    }

    @Transactional
    public User create(CreateUserRequest request) {
        User user = getByUsername(request.username);
        if (user != null) {
            return user;
        } else {
            UserEntity newUser = mapCreateRequestToEntity(request);
            entityManager.persist(newUser);
            return mapEntityToUser(newUser);
        }
    }

    @Transactional
    public User delete(DeleteUserRequest request) {
        UserEntity user = entityManager.find(UserEntity.class, request.user_id);
        entityManager.remove(user);
        return mapEntityToUser(user);
    }

    @Transactional
    public User update(UpdateUserRequest request) {
        UserEntity updateUser = mapUpdateRequestToEntity(request);
        entityManager.merge(updateUser);
        return mapEntityToUser(updateUser);
    }
    
}
