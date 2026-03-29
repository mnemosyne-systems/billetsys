export const SUPPORT_TICKET_STATUSES = ['Open', 'Assigned', 'In Progress', 'Pending', 'Closed', 'Resolved'] as const;

export type SupportTicketStatus = (typeof SUPPORT_TICKET_STATUSES)[number];
