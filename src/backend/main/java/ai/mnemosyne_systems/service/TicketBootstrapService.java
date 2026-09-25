/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.service;

import ai.mnemosyne_systems.model.Category;
import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.CompanyEntitlement;
import ai.mnemosyne_systems.model.Version;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared ticket-form lookups, previously duplicated across the ticket workbench, support/user/superuser ticket
 * resources and their view-support helpers.
 * <p>
 * Two entitlement query variants are kept deliberately: the ticket workbench historically loads entitlements without
 * ordering or de-duplication (its default selection depends on that order), while the role bootstraps use the ordered,
 * de-duplicated variant. Merging them would silently change the workbench's preselected entitlement, so both stay until
 * the caching step revisits the shape with explicit review.
 */
@ApplicationScoped
public class TicketBootstrapService {

    public record CompanyEntitlementOption(Long id, Long entitlementId, String entitlementName, Long supportLevelId,
            String supportLevelName) {
    }

    public record VersionOption(Long id, String name, LocalDate date) {
    }

    public record CategoryOption(Long id, String name, boolean isDefault) {
    }

    public record CompanyOption(Long id, String name) {
    }

    /**
     * Entitlement options for a company in database order (ticket workbench contract — its default selection depends on
     * this order, so it stays separate from {@link #orderedEntitlementOptionsForCompany}).
     */
    @CacheResult(cacheName = "bootstrap-entitlements")
    public List<CompanyEntitlementOption> entitlementOptionsForCompany(@CacheKey Long companyId) {
        Company company = companyId == null ? null : Company.findById(companyId);
        return toOptions(loadEntitlements(company));
    }

    /**
     * Entitlement options for a company, ordered and de-duplicated (role ticket-form contract).
     */
    @CacheResult(cacheName = "bootstrap-entitlements-ordered")
    public List<CompanyEntitlementOption> orderedEntitlementOptionsForCompany(@CacheKey Long companyId) {
        Company company = companyId == null ? null : Company.findById(companyId);
        return toOptions(orderedUniqueEntitlements(company));
    }

    private List<CompanyEntitlementOption> toOptions(List<CompanyEntitlement> entries) {
        return entries.stream()
                .map(entry -> new CompanyEntitlementOption(entry.id,
                        entry.entitlement == null ? null : entry.entitlement.id,
                        entry.entitlement == null ? null : entry.entitlement.name,
                        entry.supportLevel == null ? null : entry.supportLevel.id,
                        entry.supportLevel == null ? null : entry.supportLevel.name))
                .toList();
    }

    @CacheResult(cacheName = "bootstrap-versions")
    public List<VersionOption> versionOptionsForEntitlement(@CacheKey Long entitlementId) {
        if (entitlementId == null) {
            return List.of();
        }
        return Version.<Version> list("entitlement.id = ?1 order by date asc, id asc", entitlementId).stream()
                .map(version -> new VersionOption(version.id, version.name, version.date)).toList();
    }

    /**
     * Selects the requested company id from already-loaded options, falling back to the first option — mirroring the
     * per-resource {@code selectCompany} helpers. Returns {@code null} only when there are no options at all.
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

    /**
     * Selects the requested entitlement id from already-loaded options, falling back to the first option — mirroring
     * the per-resource {@code selectEntitlement} helpers. Returns {@code null} only when there are no options at all.
     */
    public static Long selectEntitlementId(List<CompanyEntitlementOption> entitlements, Long companyEntitlementId) {
        if (entitlements == null || entitlements.isEmpty()) {
            return null;
        }
        if (companyEntitlementId == null) {
            return entitlements.get(0).id();
        }
        return entitlements.stream()
                .filter(entitlement -> entitlement.id() != null && entitlement.id().equals(companyEntitlementId))
                .findFirst().orElse(entitlements.get(0)).id();
    }

    /**
     * Default affects-version selected from already-loaded options ("1.0.0" first, else earliest). Pure function over
     * the cached list — static so callers cannot accidentally bypass the cache via self-invocation.
     */
    public static VersionOption selectDefaultVersionOption(List<VersionOption> versions) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }
        return versions.stream().filter(version -> "1.0.0".equals(version.name())).findFirst().orElse(versions.get(0));
    }

    @CacheResult(cacheName = "bootstrap-categories")
    public List<CategoryOption> allCategoryOptions() {
        return Category.<Category> list("order by name").stream()
                .map(category -> new CategoryOption(category.id, category.name, category.isDefault)).toList();
    }

    @CacheResult(cacheName = "bootstrap-companies")
    public List<CompanyOption> allCompanyOptions() {
        return Company.<Company> list("order by name").stream()
                .map(company -> new CompanyOption(company.id, company.name)).toList();
    }

    @CacheInvalidate(cacheName = "bootstrap-entitlements")
    public void invalidateEntitlements(@CacheKey Long companyId) {
    }

    @CacheInvalidate(cacheName = "bootstrap-entitlements-ordered")
    public void invalidateOrderedEntitlements(@CacheKey Long companyId) {
    }

    /**
     * Evicts both entitlement option caches for one company (they share the underlying rows but keep different order
     * contracts).
     */
    public void invalidateEntitlementsForCompany(Long companyId) {
        if (companyId != null) {
            invalidateEntitlements(companyId);
            invalidateOrderedEntitlements(companyId);
        }
    }

    @CacheInvalidate(cacheName = "bootstrap-versions")
    public void invalidateVersions(@CacheKey Long entitlementId) {
    }

    @CacheInvalidateAll(cacheName = "bootstrap-entitlements")
    public void invalidateAllEntitlements() {
    }

    @CacheInvalidateAll(cacheName = "bootstrap-entitlements-ordered")
    public void invalidateAllOrderedEntitlements() {
    }

    /**
     * Evicts entitlement options for every company (entitlement/level names appear in all of them). Rare admin writes
     * only.
     */
    public void invalidateAllEntitlementOptions() {
        invalidateAllEntitlements();
        invalidateAllOrderedEntitlements();
    }

    @CacheInvalidateAll(cacheName = "bootstrap-categories")
    public void invalidateAllCategories() {
    }

    @CacheInvalidateAll(cacheName = "bootstrap-companies")
    public void invalidateAllCompanies() {
    }

    public List<CompanyEntitlement> loadEntitlements(Company company) {
        if (company == null) {
            return List.of();
        }
        return CompanyEntitlement.find(
                "select distinct ce from CompanyEntitlement ce join fetch ce.entitlement join fetch ce.supportLevel where ce.company = ?1",
                company).list();
    }

    public List<CompanyEntitlement> orderedUniqueEntitlements(Company company) {
        if (company == null) {
            return List.of();
        }
        return uniqueEntitlements(CompanyEntitlement.find(
                "select distinct ce from CompanyEntitlement ce join fetch ce.entitlement join fetch ce.supportLevel where ce.company = ?1 order by ce.entitlement.name, ce.supportLevel.level, ce.supportLevel.id",
                company).list());
    }

    public List<CompanyEntitlement> uniqueEntitlements(List<CompanyEntitlement> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        List<CompanyEntitlement> unique = new ArrayList<>();
        Set<Long> seenEntitlementIds = new LinkedHashSet<>();
        for (CompanyEntitlement entry : entries) {
            if (entry == null || entry.entitlement == null || entry.entitlement.id == null) {
                continue;
            }
            if (seenEntitlementIds.add(entry.entitlement.id)) {
                unique.add(entry);
            }
        }
        return unique;
    }

    public List<Version> availableVersions(CompanyEntitlement companyEntitlement) {
        if (companyEntitlement == null || companyEntitlement.entitlement == null) {
            return List.of();
        }
        return Version.list("entitlement = ?1 order by date asc, id asc", companyEntitlement.entitlement);
    }

    public Version defaultAffectsVersion(CompanyEntitlement companyEntitlement) {
        if (companyEntitlement == null || companyEntitlement.entitlement == null) {
            return null;
        }
        Version version = Version.find("entitlement = ?1 and name = ?2 order by date asc, id asc",
                companyEntitlement.entitlement, "1.0.0").firstResult();
        if (version != null) {
            return version;
        }
        return Version.find("entitlement = ?1 order by date asc, id asc", companyEntitlement.entitlement).firstResult();
    }
}
