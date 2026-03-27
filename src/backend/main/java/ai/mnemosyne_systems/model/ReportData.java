/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

package ai.mnemosyne_systems.model;

import java.util.List;
import java.util.Map;

public class ReportData {
    public int totalTickets;
    public Map<String, Long> ticketsByStatus;
    public Map<String, Long> ticketsByCategory;
    public Map<String, Long> ticketsByCompany;
    public Map<String, Long> ticketsOverTime;
    public Map<String, Double> avgFirstResponseTime;
    public Map<String, Double> avgResolutionTime;
    public Map<String, List<Ticket>> resolutionHistogram;
}
