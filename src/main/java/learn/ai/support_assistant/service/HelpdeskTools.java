package learn.ai.support_assistant.service;

import learn.ai.support_assistant.model.Ticket;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class HelpdeskTools {

    private final TicketService ticketService;

    public HelpdeskTools(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Tool(description = "Get the current status and description of a support ticket by its ID")
    public String getTicketStatus(@ToolParam(description = "The ticket ID, e.g. 482") String ticketId) {
        Ticket ticket = ticketService.getTicketStatus(ticketId);
        if (ticket == null) {
            return "No ticket found with ID " + ticketId;
        }
        return "Ticket " + ticket.id() + " - Status: " + ticket.status() + " - Description: " + ticket.description();
    }

    @Tool(description = "Create a new support ticket with a description of the user's issue")
    public String createTicket(@ToolParam(description = "Description of the issue") String description) {
        Ticket ticket = ticketService.createTicket(description);
        return "Created ticket " + ticket.id() + " with status " + ticket.status();
    }

    @Tool(description = "Update the status of an existing support ticket. Valid statuses: Open, In Progress, Resolved, Closed")
    public String updateTicketStatus(
            @ToolParam(description = "The ticket ID to update") String ticketId,
            @ToolParam(description = "The new status: Open, In Progress, Resolved, or Closed") String newStatus) {

        Ticket updated = ticketService.updateStatus(ticketId, newStatus);
        if (updated == null) {
            return "No ticket found with ID " + ticketId;
        }
        return "Ticket " + updated.id() + " status updated to " + updated.status();
    }

    @Tool(description = "Lists all support tickets in the system. Use this whenever the user asks about open tickets, ticket counts, or wants to see multiple tickets at once.")
    public String listTickets(
            @ToolParam(description = "Optional status filter: Open, In Progress, Resolved, Closed. Leave empty or omit to list all tickets.")
            String statusFilter) {
        System.out.println(">>> TOOL CALLED: listTickets(" + statusFilter + ")");
        List<Ticket> tickets = ticketService.listAll();

        if (statusFilter != null && !statusFilter.isBlank()) {
            tickets = tickets.stream()
                    .filter(t -> t.status().equalsIgnoreCase(statusFilter.trim()))
                    .toList();
        }

        if (tickets.isEmpty()) {
            return "No tickets found" + (statusFilter != null ? " with status " + statusFilter : "") + ".";
        }

        return tickets.stream()
                .map(t -> "Ticket " + t.id() + " - " + t.status() + " - " + t.description())
                .collect(Collectors.joining("\n"));
    }
}
