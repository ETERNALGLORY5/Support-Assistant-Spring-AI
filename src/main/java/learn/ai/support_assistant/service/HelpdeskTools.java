package learn.ai.support_assistant.service;

import learn.ai.support_assistant.model.Ticket;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

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
}