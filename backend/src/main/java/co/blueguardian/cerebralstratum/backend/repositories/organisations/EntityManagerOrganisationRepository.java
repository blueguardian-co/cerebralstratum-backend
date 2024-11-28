package co.blueguardian.cerebralstratum.backend.repositories.organisations;

import co.blueguardian.cerebralstratum.backend.controllers.organisations.Organisation;
import co.blueguardian.cerebralstratum.backend.controllers.organisations.CreateOrganisationRequest;
import co.blueguardian.cerebralstratum.backend.controllers.organisations.DeleteOrganisationRequest;
import co.blueguardian.cerebralstratum.backend.controllers.organisations.UpdateOrganisationRequest;

import java.util.List;
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
            organisation.getId(),
            organisation.getName(),
            organisation.getOwnerId(),
            organisation.getCreated()
        );
    }

    private static OrganisationEntity mapCreateRequestToEntity (CreateOrganisationRequest request) {
        return new OrganisationEntity(
                request.name,
                request.owner,
                request.created
        );
    }

    private static void mapUpdateRequestToEntity (OrganisationEntity organisation, UpdateOrganisationRequest request) {
        if (!request.name.isEmpty()) {
            organisation.setName(request.name);
        }
        if (request.owner_id != null) {
            organisation.setOwnerId(request.owner_id);
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

    public Organisation getByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        try {
            OrganisationEntity organisation = entityManager.createNamedQuery("OrganisationEntity.getOrganisationByName", OrganisationEntity.class)
                    .setParameter("name", name)
                    .getSingleResult();
            return mapEntityToOrganisation(organisation);
        } catch (jakarta.persistence.NoResultException e) {
            return null;
        }
    }

    @Transactional
    public Organisation create(CreateOrganisationRequest request) {
        Organisation organisation = getByName(request.name);
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
        OrganisationEntity organisation = entityManager.find(OrganisationEntity.class, request.id);
        entityManager.remove(organisation);
        return mapEntityToOrganisation(organisation);
    }

    @Transactional
    public Organisation update(UpdateOrganisationRequest request) {
        OrganisationEntity organisation = entityManager.find(OrganisationEntity.class, request.id);
        mapUpdateRequestToEntity(organisation, request);
        entityManager.merge(organisation);
        return mapEntityToOrganisation(organisation);
    }
    
}
