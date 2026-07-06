package br.com.fullcycle.infrastructure.repositories;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.domain.customer.CustomerId;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TicketDatabaseRepositoryIT extends IntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve buscar os ingressos de um evento")
    public void testTicketsByEventId() throws Exception {
        // given
        final var eventId = EventId.unique();
        final var otherEventId = EventId.unique();

        final var expectedTicketsSize = 2;

        final var ticket1 = ticketRepository.create(Ticket.newTicket(CustomerId.unique(), eventId));
        final var ticket2 = ticketRepository.create(Ticket.newTicket(CustomerId.unique(), eventId));
        ticketRepository.create(Ticket.newTicket(CustomerId.unique(), otherEventId));

        // when
        final var actualTickets = ticketRepository.ticketsByEventId(eventId);

        // then
        Assertions.assertEquals(expectedTicketsSize, actualTickets.size());
        Assertions.assertTrue(actualTickets.stream().anyMatch(it -> it.ticketId().equals(ticket1.ticketId())));
        Assertions.assertTrue(actualTickets.stream().anyMatch(it -> it.ticketId().equals(ticket2.ticketId())));
    }

    @Test
    @DisplayName("Deve retornar vazio quando o evento não possui ingressos")
    public void testTicketsByEventIdWithoutTickets() throws Exception {
        // given
        final var eventId = EventId.unique();

        // when
        final var actualTickets = ticketRepository.ticketsByEventId(eventId);

        // then
        Assertions.assertTrue(actualTickets.isEmpty());
    }
}
