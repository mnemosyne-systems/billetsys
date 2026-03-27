package ai.mnemosyne_systems.resource;

import ai.mnemosyne_systems.model.Company;
import ai.mnemosyne_systems.model.Country;
import ai.mnemosyne_systems.model.Timezone;
import ai.mnemosyne_systems.model.User;
import java.util.List;
import java.util.Locale;

public final class UserDirectoryApiModels {

    private UserDirectoryApiModels() {
    }

    public record CompanyOption(Long id, String name) {
    }

    public record CountryOption(Long id, String name, String code) {
    }

    public record TimezoneOption(Long id, String name) {
    }

    public record TypeOption(String value, String label) {
    }

    public record UserReference(Long id, String username, String displayName, String email, String type,
            String typeLabel, String detailPath, String editPath) {
    }

    public record DirectoryListResponse(String title, String description, Long selectedCompanyId,
            boolean showCompanySelector, boolean companyLocked, String createPath, List<CompanyOption> companies,
            List<UserReference> items) {
    }

    public record UserFormData(Long id, String name, String fullName, String email, String social, String phoneNumber,
            String phoneExtension, Long countryId, Long timezoneId, String type, Long companyId) {
    }

    public record UserFormResponse(String title, String submitPath, String cancelPath, Long selectedCompanyId,
            boolean companyLocked, boolean passwordRequired, List<CompanyOption> companies,
            List<CountryOption> countries, List<TimezoneOption> timezones, List<TypeOption> types, UserFormData user) {
    }

    public record UserDetailResponse(Long id, String username, String displayName, String fullName, String email,
            String social, String phoneNumber, String phoneExtension, String type, String typeLabel, String countryName,
            String timezoneName, Long companyId, String companyName, String editPath, String deletePath,
            String backPath) {
    }

    public record CompanyDetailResponse(Long id, String name, String address1, String address2, String city,
            String state, String zip, String phoneNumber, String countryName, String timezoneName,
            List<UserReference> users, List<UserReference> superusers, List<UserReference> tamUsers,
            List<UserReference> supportUsers, String backPath) {
    }

    public static CompanyOption companyOption(Company company) {
        return new CompanyOption(company == null ? null : company.id, company == null ? null : company.name);
    }

    public static CountryOption countryOption(Country country) {
        return new CountryOption(country == null ? null : country.id, country == null ? null : country.name,
                country == null ? null : country.code);
    }

    public static TimezoneOption timezoneOption(Timezone timezone) {
        return new TimezoneOption(timezone == null ? null : timezone.id, timezone == null ? null : timezone.name);
    }

    public static UserReference userReference(User user, String detailPath, String editPath) {
        return new UserReference(user == null ? null : user.id, user == null ? null : user.name,
                user == null ? null : user.getDisplayName(), user == null ? null : user.email,
                user == null ? null : user.type, typeLabel(user == null ? null : user.type), detailPath, editPath);
    }

    public static UserFormData userFormData(User user, Long companyId) {
        return new UserFormData(user == null ? null : user.id, user == null ? null : user.name,
                user == null ? null : user.fullName, user == null ? null : user.email,
                user == null ? null : user.social, user == null ? null : user.phoneNumber,
                user == null ? null : user.phoneExtension,
                user == null || user.country == null ? null : user.country.id,
                user == null || user.timezone == null ? null : user.timezone.id, user == null ? null : user.type,
                companyId);
    }

    public static String typeLabel(String type) {
        if (type == null) {
            return "User";
        }
        return switch (type.trim().toLowerCase(Locale.ENGLISH)) {
            case User.TYPE_ADMIN -> "Admin";
            case User.TYPE_SUPPORT -> "Support";
            case User.TYPE_TAM -> "TAM";
            case User.TYPE_SUPERUSER -> "Superuser";
            default -> "User";
        };
    }
}
