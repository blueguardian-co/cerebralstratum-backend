package co.blueguardian.cerebralstratum.backend.repositories.organisations;

import co.blueguardian.cerebralstratum.backend.controllers.organisations.*;

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
public class EntityManagerOrganisationRepository implements OrganisationRepository {

    @Inject
    EntityManager entityManager;

    private static Organisation mapEntityToOrganisation (OrganisationEntity organisation) {
        return new Organisation(
            organisation.getKeycloakOrgId(),
            organisation.getKeycloakUserId(),
            organisation.getCreated()
        );
    }

    private static GetOrganisationRequest mapEntityToGetOrganisationRequest (OrganisationEntity organisation) {
        return new GetOrganisationRequest(
                organisation.getKeycloakOrgId(),
                organisation.getKeycloakUserId(),
                organisation.getCreated()
        );
    }

    private static OrganisationEntity mapCreateRequestToEntity (CreateOrganisationRequest request) {
        return new OrganisationEntity(
                request.keycloak_org_id,
                request.keycloak_user_id,
                request.created
        );
    }

    private static void mapUpdateRequestToEntity (OrganisationEntity organisation, UpdateOrganisationRequest request) {
        if (request.keycloak_org_id != null) {
            organisation.setKeycloakOrgId(request.keycloak_org_id);
        }
        if (request.keycloak_user_id != null) {
            organisation.setKeycloakUserId(request.keycloak_user_id);
        }
    }

    public List<Organisation> findAll() {
        return entityManager.createNamedQuery("OrganisationEntity.findAll", OrganisationEntity.class)
            .getResultList().stream().map(EntityManagerOrganisationRepository::mapEntityToOrganisation).collect(Collectors.toList());
    }

    public Organisation getById(int organisation_id) {
        OrganisationEntity organisation = entityManager.find(OrganisationEntity.class, organisation_id);
        return mapEntityToOrganisation(organisation);
    }

    public Organisation getByKeycloakOrgId(UUID keycloak_org_id) {
        if (keycloak_org_id == null) {
            return null;
        }
        try {
            OrganisationEntity organisation = entityManager.createNamedQuery("OrganisationEntity.getOrganisationByKeycloakOrgId", OrganisationEntity.class)
                    .setParameter("keycloak_org_id", keycloak_org_id)
                    .getSingleResult();
            return mapEntityToOrganisation(organisation);
        } catch (jakarta.persistence.NoResultException e) {
            return null;
        }
    }

    @Transactional
    public Organisation create(CreateOrganisationRequest request) {
        Organisation organisation = getByKeycloakOrgId(request.keycloak_org_id);
        if (organisation != null) {
            return organisation;
        } else {
            OrganisationEntity newOrganisation = mapCreateRequestToEntity(request);
            entityManager.persist(newOrganisation);
            return mapEntityToOrganisation(newOrganisation);
        }
    }

    @Transactional
    public Organisation delete(DeleteOrganisationRequest request) {
        OrganisationEntity organisation = entityManager.find(OrganisationEntity.class, request.keycloak_org_id);
        entityManager.remove(organisation);
        return mapEntityToOrganisation(organisation);
    }

    @Transactional
    public Organisation update(UpdateOrganisationRequest request) {
        OrganisationEntity organisation = entityManager.find(OrganisationEntity.class, request.keycloak_org_id);
        mapUpdateRequestToEntity(organisation, request);
        entityManager.merge(organisation);
        return mapEntityToOrganisation(organisation);
    }
    
}
