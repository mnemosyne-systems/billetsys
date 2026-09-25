/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.User;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared directory lookups with company-scoped caching.
 * <p>
 * Two cached atoms: the per-actor allowed-company options (keyed by actor id — an actor's scope <em>is</em> their
 * companies) and the per-company user entries (keyed by company id). Both hold detached DTOs only; role-specific
 * response assembly (detail paths, flags, company locking) stays in the resources, which map these atoms. Callers must
 * never pass {@code null} keys (Caffeine forbids null keys); resources keep their {@code null}-company ternaries for
 * that.
 * <p>
 * Invalidation is targeted: user writes invalidate the user's own set plus every user list they belong to; company
 * writes invalidate the company's list plus every member's set. See {@link #invalidateUserEverywhere} and
 * {@link #invalidateCompaniesForMembers}.
 */
@ApplicationScoped
public class DirectoryService {

    public record CompanyOption(Long id, String name) {
    }

    public record UserDirectoryEntry(Long id, String username, String displayName, String email, String type,
            boolean active) {
    }

    @CacheResult(cacheName = "directory-companies")
    public List<CompanyOption> companyOptionsForActor(@CacheKey Long actorId) {
        User actor = actorId == null ? null : User.findById(actorId);
        if (actor == null) {
            return List.of();
        }
        return allowedCompanies(actor).stream().map(company -> new CompanyOption(company.id, company.name)).toList();
    }

    @CacheResult(cacheName = "directory-users")
    public List<UserDirectoryEntry> userEntriesForCompany(@CacheKey Long companyId) {
        Company company = companyId == null ? null : Company.findById(companyId);
        if (company == null) {
            return List.of();
        }
        return usersForCompany(company).stream().map(user -> new UserDirectoryEntry(user.id, user.name,
                user.getDisplayName(), user.email, user.type, user.active)).toList();
    }

    @CacheInvalidate(cacheName = "directory-users")
    public void invalidateUsers(@CacheKey Long companyId) {
    }

    @CacheInvalidate(cacheName = "directory-companies")
    public void invalidateCompanies(@CacheKey Long actorId) {
    }

    @CacheInvalidateAll(cacheName = "directory-companies")
    public void invalidateAllCompanies() {
    }

    /**
     * Invalidates a user's own company set plus every user list of every company they belong to (resolved fresh) plus
     * any additionally known company ids (e.g. captured before a membership move). Covers profile edits, deletes and
     * membership changes with one call.
     */
    public void invalidateUserEverywhere(Long userId, List<Long> knownCompanyIds) {
        if (userId != null) {
            invalidateCompanies(userId);
        }
        Set<Long> companyIds = new LinkedHashSet<>(knownCompanyIds == null ? List.of() : knownCompanyIds);
        companyIds.addAll(companyIdsOfUser(userId));
        for (Long companyId : companyIds) {
            if (companyId != null) {
                invalidateUsers(companyId);
            }
        }
    }

    /**
     * Invalidates the company sets of every member of the given company (for renames and membership changes). Falls
     * back to evict-all when the company can no longer be resolved.
     */
    public void invalidateCompaniesForMembers(Long companyId) {
        Company company = companyId == null ? null : Company.findById(companyId);
        if (company == null || company.users == null) {
            invalidateAllCompanies();
            return;
        }
        for (User member : company.users) {
            if (member != null && member.id != null) {
                invalidateCompanies(member.id);
            }
        }
    }

    public List<Long> companyIdsOfUser(Long userId) {
        User user = userId == null ? null : User.findById(userId);
        if (user == null) {
            return List.of();
        }
        return Company.<Company> find("select c from Company c join c.users u where u = ?1", user).list().stream()
                .map(company -> company.id).toList();
    }

    public List<Company> allowedCompanies(User actor) {
        if (actor == null) {
            return List.of();
        }
        return Company.find("select distinct c from Company c join c.users u where u = ?1 order by c.name", actor)
                .list();
    }

    /**
     * Selects the requested company id from already-loaded options, falling back to the first option — mirroring the
     * per-resource {@code selectCompany} helpers this replaces. Returns {@code null} only when there are no options at
     * all.
     */
    public static Long selectCompanyId(List<CompanyOption> companies, Long companyId) {
        if (companies == null || companies.isEmpty()) {
            return null;
        }
        if (companyId == null) {
            return companies.get(0).id();
        }
        return companies.stream().filter(company -> company.id() != null && company.id().equals(companyId)).findFirst()
                .orElse(companies.get(0)).id();
    }

    public List<User> usersForCompany(Company company) {
        if (company == null) {
            return List.of();
        }
        return Company.find("select u from Company c join c.users u where c = ?1 order by u.name", company).list();
    }

    public List<User> externalsForCompany(Company company) {
        if (company == null) {
            return List.of();
        }
        return Company.find("select u from Company c join c.users u where c = ?1 and u.type = ?2 order by u.fullName",
                company, User.TYPE_EXTERNAL).list();
    }
}
