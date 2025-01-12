package co.blueguardian.cerebralstratum.backend.repositories.users;

import co.blueguardian.cerebralstratum.backend.controllers.users.User;
import co.blueguardian.cerebralstratum.backend.controllers.users.CreateUserRequest;
import co.blueguardian.cerebralstratum.backend.controllers.users.DeleteUserRequest;
import co.blueguardian.cerebralstratum.backend.controllers.users.UpdateUserRequest;
import co.blueguardian.cerebralstratum.backend.repositories.organisations.OrganisationEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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
            user.getKeycloakUserId(),
            user.getOrganisation().getKeycloakOrgId(),
            user.getCreated(),
            user.getSubscriptionActive(),
            user.getSubscriptionDiscount()
        );
    }

    private UserEntity mapCreateRequestToEntity (CreateUserRequest request) {
        OrganisationEntity organisation;
        if (request.keycloak_org_id != null) {
           organisation = entityManager.find(OrganisationEntity.class, request.keycloak_org_id);
        } else {
           organisation = null;
        }
        LocalDateTime created = LocalDateTime.now();
        Boolean subscription_active = true;
        Integer subscription_discount = 0;
        return new UserEntity(
                request.keycloak_user_id,
                organisation,
                created,
                subscription_active,
                subscription_discount
        );
    }

    private void mapUpdateRequestToEntity (UserEntity user, UpdateUserRequest request) {
        if (request.keycloak_org_id != null) {
            OrganisationEntity organisation = entityManager.find(OrganisationEntity.class, request.keycloak_org_id);
            user.setOrganisation(organisation);
        }
    }

    public List<User> findAll() {
        return entityManager.createNamedQuery("UserEntity.findAll", UserEntity.class)
            .getResultList().stream().map(EntityManagerUserRepository::mapEntityToUser).collect(Collectors.toList());          
    }

    public User getById(int id) {
        UserEntity user = entityManager.find(UserEntity.class, id);
        return mapEntityToUser(user);
    }

    public User getByKeycloakUserId(UUID keycloak_user_id) {
        try {
            UserEntity user = entityManager.createNamedQuery("UserEntity.getByKeycloakUserId", UserEntity.class)
                    .setParameter("keycloak_user_id", keycloak_user_id)
                    .getSingleResult();
            return mapEntityToUser(user);
        } catch (jakarta.persistence.NoResultException e) {
            return null;
        }
    }

    @Transactional
    public User create(CreateUserRequest request) {
        User user = getByKeycloakUserId(request.keycloak_user_id);
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
        UserEntity user = entityManager.find(UserEntity.class, request.keycloak_user_id);
        entityManager.remove(user);
        return mapEntityToUser(user);
    }

    @Transactional
    public User update(UpdateUserRequest request) {
        UserEntity user = entityManager.find(UserEntity.class, request.keycloak_user_id);
        mapUpdateRequestToEntity(user, request);
        entityManager.merge(user);
        return mapEntityToUser(user);
    }
    
}
