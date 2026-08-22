package learn.ai.support_assistant.service;

import learn.ai.support_assistant.model.Ticket;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TicketService {

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1000);

    public TicketService() {
        // seed some fake data
        tickets.put("482", new Ticket("482", "In Progress", "Password reset not working"));
        tickets.put("501", new Ticket("501", "Resolved", "Refund request for order #99"));
        tickets.put("512", new Ticket("512", "Open", "Cannot access dashboard"));
    }

    public Ticket getTicketStatus(String ticketId) {
        return tickets.get(ticketId);
    }

    public Ticket createTicket(String description) {
        String id = String.valueOf(idCounter.incrementAndGet());
        Ticket ticket = new Ticket(id, "Open", description);
        tickets.put(id, ticket);
        return ticket;
    }

    public Ticket updateStatus(String ticketId, String newStatus) {
        Ticket existing = tickets.get(ticketId);
        if (existing == null) return null;
        Ticket updated = new Ticket(existing.id(), newStatus, existing.description());
        tickets.put(ticketId, updated);
        return updated;
    }

    public List<Ticket> listAll() {
        return List.copyOf(tickets.values());
    }
}
