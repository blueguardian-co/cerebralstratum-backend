package co.blueguardian.cerebralstratum.backend.controllers.users;

import co.blueguardian.cerebralstratum.backend.repositories.organisations.OrganisationEntity;

import java.time.LocalDateTime;

public class User {

    public int id;
    public String username;
    public OrganisationEntity organisation;
    public LocalDateTime created;
    public Boolean subscription_active;
    public Integer subscription_discount;

    public User (
            int id,
            String username,
            OrganisationEntity organisation,
            LocalDateTime created,
            Boolean subscription_active,
            Integer subscription_discount
    ) {
        this.id = id;
        this.username = username;
        this.organisation = organisation;
        this.created = created;
        this.subscription_active = subscription_active;
        this.subscription_discount = subscription_discount;
    }
}
