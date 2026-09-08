/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THE ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THE AGREEMENT.
 */

package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.Ticket;
import ai.mnemosyne_systems.model.User;
import ai.mnemosyne_systems.model.UserAvailability;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class UserAvailabilityService {

    @Transactional
    public UserAvailability createPersonal(User user, Company company, LocalDate startDate, LocalDate endDate,
            String reason) {

        validateDates(startDate, endDate);

        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }

        if (company == null) {
            throw new IllegalArgumentException("Company is required");
        }

        validatePersonalPermission(user);

        validateCompanyConsistency(user, company);

        validateNoOverlappingPersonalAvailability(user, startDate, endDate, null);

        UserAvailability availability = new UserAvailability();

        availability.user = user;
        availability.company = company;
        availability.startDate = startDate;
        availability.endDate = endDate;
        availability.scope = UserAvailability.SCOPE_PERSONAL;
        availability.reason = trimOrNull(reason);

        availability.persist();

        return availability;
    }

    @Transactional
    public UserAvailability createCompany(User user, Company company, LocalDate startDate, LocalDate endDate,
            String reason) {

        validateDates(startDate, endDate);

        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }

        if (company == null) {
            throw new IllegalArgumentException("Company is required");
        }

        validateCompanyAvailabilityPermission(user);

        validateCompanyConsistency(user, company);

        UserAvailability availability = new UserAvailability();

        availability.user = null;
        availability.company = company;
        availability.startDate = startDate;
        availability.endDate = endDate;
        availability.scope = UserAvailability.SCOPE_COMPANY;
        availability.reason = trimOrNull(reason);

        availability.persist();

        return availability;
    }

    @Transactional
    public UserAvailability update(User user, Long id, LocalDate startDate, LocalDate endDate, String reason) {

        validateDates(startDate, endDate);

        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }

        if (id == null) {
            throw new IllegalArgumentException("Availability id is required");
        }

        UserAvailability availability = UserAvailability.findById(id);

        if (availability == null) {
            throw new IllegalArgumentException("Availability not found: " + id);
        }

        validateUpdatePermission(user, availability);

        if (UserAvailability.SCOPE_PERSONAL.equals(availability.scope)) {

            validateNoOverlappingPersonalAvailability(availability.user, startDate, endDate, availability.id);
        }

        availability.startDate = startDate;
        availability.endDate = endDate;
        availability.reason = trimOrNull(reason);

        return availability;
    }

    @Transactional
    public void delete(User user, Long id) {

        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }

        if (id == null) {
            throw new IllegalArgumentException("Availability id is required");
        }

        UserAvailability availability = UserAvailability.findById(id);

        if (availability == null) {
            throw new IllegalArgumentException("Availability not found: " + id);
        }

        validateUpdatePermission(user, availability);

        availability.delete();
    }

    public List<UserAvailability> getForUser(User user) {

        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }

        return UserAvailability
                .find("user = ?1 and scope = ?2 " + "order by startDate asc", user, UserAvailability.SCOPE_PERSONAL)
                .list();
    }

    public List<UserAvailability> getForCompany(Company company) {

        if (company == null) {
            throw new IllegalArgumentException("Company is required");
        }

        return UserAvailability.find("company = ?1 and scope = ?2 " + "order by startDate asc", company,
                UserAvailability.SCOPE_COMPANY).list();
    }

    public boolean isUnavailable(User user, Company company, LocalDate date) {

        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }

        if (company == null) {
            throw new IllegalArgumentException("Company is required");
        }

        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }

        boolean personalUnavailable = UserAvailability.count(
                "user = ?1 " + "and scope = ?2 " + "and startDate <= ?3 " + "and endDate >= ?3", user,
                UserAvailability.SCOPE_PERSONAL, date) > 0;

        if (personalUnavailable) {
            return true;
        }

        boolean companyUnavailable = UserAvailability.count(
                "company = ?1 " + "and scope = ?2 " + "and startDate <= ?3 " + "and endDate >= ?3", company,
                UserAvailability.SCOPE_COMPANY, date) > 0;

        return companyUnavailable;
    }

    public List<String> getWarningsForTicket(Ticket ticket) {

        if (ticket == null) {
            throw new IllegalArgumentException("Ticket is required");
        }

        List<String> warnings = new ArrayList<>();

        if (ticket.company == null) {
            return warnings;
        }

        LocalDate today = LocalDate.now();

        List<UserAvailability> companyAvailability = UserAvailability
                .find("company = ?1 " + "and scope = ?2 " + "and startDate <= ?3 " + "and endDate >= ?3",
                        ticket.company, UserAvailability.SCOPE_COMPANY, today)
                .list();

        for (UserAvailability availability : companyAvailability) {

            String message = buildCompanyWarning(availability);

            if (message != null) {
                warnings.add(message);
            }
        }

        Set<Long> checkedUserIds = new HashSet<>();

        for (User user : ticket.supportUsers) {

            if (user == null || user.id == null) {
                continue;
            }

            if (!checkedUserIds.add(user.id)) {
                continue;
            }

            addUserWarning(warnings, user, ticket.company, today);
        }

        for (User user : ticket.tamUsers) {

            if (user == null || user.id == null) {
                continue;
            }

            if (!checkedUserIds.add(user.id)) {
                continue;
            }

            addUserWarning(warnings, user, ticket.company, today);
        }

        return warnings;
    }

    private void addUserWarning(List<String> warnings, User user, Company company, LocalDate date) {

        List<UserAvailability> personalAvailability = UserAvailability
                .find("user = ?1 " + "and scope = ?2 " + "and startDate <= ?3 " + "and endDate >= ?3", user,
                        UserAvailability.SCOPE_PERSONAL, date)
                .list();

        for (UserAvailability availability : personalAvailability) {

            String message = buildUserWarning(availability);

            if (message != null) {
                warnings.add(message);
            }
        }
    }

    private String buildUserWarning(UserAvailability availability) {

        if (availability == null || availability.user == null) {
            return null;
        }

        StringBuilder warning = new StringBuilder();

        warning.append("User ").append(displayUser(availability.user)).append(" is out of office from ")
                .append(availability.startDate).append(" to ").append(availability.endDate);

        if (availability.reason != null && !availability.reason.isBlank()) {

            warning.append(" (").append(availability.reason).append(")");
        }

        return warning.toString();
    }

    private String buildCompanyWarning(UserAvailability availability) {

        if (availability == null || availability.company == null) {
            return null;
        }

        StringBuilder warning = new StringBuilder();

        warning.append("Company ").append(availability.company.name).append(" is out of office from ")
                .append(availability.startDate).append(" to ").append(availability.endDate);

        if (availability.reason != null && !availability.reason.isBlank()) {

            warning.append(" (").append(availability.reason).append(")");
        }

        return warning.toString();
    }

    private void validatePersonalPermission(User user) {

        String type = normalize(user.type);

        if (!User.TYPE_USER.equals(type) && !User.TYPE_TAM.equals(type) && !User.TYPE_SUPPORT.equals(type)
                && !User.TYPE_SUPERUSER.equals(type) && !User.TYPE_ADMIN.equals(type)) {

            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    private void validateCompanyAvailabilityPermission(User user) {

        String type = normalize(user.type);

        if (!User.TYPE_SUPPORT.equals(type) && !User.TYPE_SUPERUSER.equals(type) && !User.TYPE_ADMIN.equals(type)) {

            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    private void validateUpdatePermission(User user, UserAvailability availability) {

        String type = normalize(user.type);

        if (UserAvailability.SCOPE_COMPANY.equals(availability.scope)) {

            validateCompanyAvailabilityPermission(user);

            validateCompanyConsistency(user, availability.company);

            return;
        }

        if (!UserAvailability.SCOPE_PERSONAL.equals(availability.scope)) {

            throw new IllegalStateException("Invalid availability scope");
        }

        if (availability.user == null || user.id == null || !user.id.equals(availability.user.id)) {

            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }

        validatePersonalPermission(user);

        validateCompanyConsistency(user, availability.company);
    }

    private void validateCompanyConsistency(User user, Company company) {

        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }

        if (company == null) {
            throw new IllegalArgumentException("Company is required");
        }

        boolean belongsToCompany = Company
                .count("select count(c) from Company c join c.users u where c = ?1 and u = ?2", company, user) > 0;

        if (!belongsToCompany) {
            throw new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }

    private void validateNoOverlappingPersonalAvailability(User user, LocalDate startDate, LocalDate endDate,
            Long excludedId) {

        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }

        boolean overlaps;

        if (excludedId == null) {

            overlaps = UserAvailability.count(
                    "user = ?1 " + "and scope = ?2 " + "and startDate <= ?3 " + "and endDate >= ?4", user,
                    UserAvailability.SCOPE_PERSONAL, endDate, startDate) > 0;

        } else {

            overlaps = UserAvailability.count(
                    "user = ?1 " + "and scope = ?2 " + "and id <> ?3 " + "and startDate <= ?4 " + "and endDate >= ?5",
                    user, UserAvailability.SCOPE_PERSONAL, excludedId, endDate, startDate) > 0;
        }

        if (overlaps) {
            throw new IllegalArgumentException("Personal availability period overlaps " + "with an existing period");
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {

        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required");
        }

        if (endDate == null) {
            throw new IllegalArgumentException("End date is required");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }

    private String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase();
    }

    private String trimOrNull(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private String displayUser(User user) {

        if (user.fullName != null && !user.fullName.isBlank()) {

            return user.fullName;
        }

        if (user.name != null && !user.name.isBlank()) {

            return user.name;
        }

        if (user.email != null && !user.email.isBlank()) {

            return user.email;
        }

        return String.valueOf(user.id);
    }
}
