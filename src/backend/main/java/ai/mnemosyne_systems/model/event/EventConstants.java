/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.model.event;

public class EventConstants {
    private EventConstants() {
    }

    // Ticket lifecycle and changes
    public static final long TICKET_STATUS_CHANGED = 1L;
    public static final long TICKET_CATEGORY_CHANGED = 2L;
    public static final long TICKET_OPENED = 3L;
    public static final long TICKET_CREATED = TICKET_OPENED;
    public static final long TICKET_ASSIGNED = 4L;
    public static final long TICKET_RESOLVED = 5L;
    public static final long TICKET_CLOSED = 6L;
    public static final long TICKET_DELETED = 7L;
    public static final long TICKET_SUPPORT_USER_ADDED = 8L;
    public static final long TICKET_SUPPORT_USER_REMOVED = 9L;
    public static final long TICKET_TAM_ADDED = 10L;
    public static final long TICKET_TAM_REMOVED = 11L;
    public static final long TICKET_EXTERNAL_USER_ADDED = 12L;
    public static final long TICKET_EXTERNAL_USER_REMOVED = 13L;
    public static final long TICKET_PARTICIPANT_ADDED = 14L;
    public static final long TICKET_PARTICIPANT_REMOVED = 15L;

    // Entity lifecycle events. The event key is the primary key of the named entity.
    public static final long ARTICLE_CREATED = 100L;
    public static final long ARTICLE_DELETED = 101L;
    public static final long ATTACHMENT_CREATED = 102L;
    public static final long ATTACHMENT_DELETED = 103L;
    public static final long CATEGORY_CREATED = 104L;
    public static final long CATEGORY_DELETED = 105L;
    public static final long COMPANY_CREATED = 106L;
    public static final long COMPANY_DELETED = 107L;
    public static final long COMPANY_ENTITLEMENT_CREATED = 108L;
    public static final long COMPANY_ENTITLEMENT_DELETED = 109L;
    public static final long COUNTRY_CREATED = 110L;
    public static final long COUNTRY_DELETED = 111L;
    public static final long CROSS_REFERENCE_CREATED = 112L;
    public static final long CROSS_REFERENCE_DELETED = 113L;
    public static final long ENTITLEMENT_CREATED = 114L;
    public static final long ENTITLEMENT_DELETED = 115L;
    public static final long INSTALLATION_CREATED = 116L;
    public static final long INSTALLATION_DELETED = 117L;
    public static final long LEVEL_CREATED = 118L;
    public static final long LEVEL_DELETED = 119L;
    public static final long MESSAGE_CREATED = 120L;
    public static final long MESSAGE_DELETED = 121L;
    public static final long PASSWORD_RESET_TOKEN_CREATED = 122L;
    public static final long PASSWORD_RESET_TOKEN_DELETED = 123L;
    public static final long TICKET_IMPORT_BATCH_CREATED = 124L;
    public static final long TICKET_IMPORT_BATCH_DELETED = 125L;
    public static final long TICKET_IMPORT_RECORD_CREATED = 126L;
    public static final long TICKET_IMPORT_RECORD_DELETED = 127L;
    public static final long TIMEZONE_CREATED = 128L;
    public static final long TIMEZONE_DELETED = 129L;
    public static final long USER_CREATED = 130L;
    public static final long USER_DELETED = 131L;
    public static final long VERSION_CREATED = 132L;
    public static final long VERSION_DELETED = 133L;
    public static final long USER_ACTIVATED = 134L;
    public static final long USER_DEACTIVATED = 135L;
    public static final long CUSTOM_FIELD_CREATED = 136L;
    public static final long CUSTOM_FIELD_UPDATED = 137L;
    public static final long CUSTOM_FIELD_DEACTIVATED = 138L;
    public static final long CUSTOM_FIELD_DELETED = 139L;
}
