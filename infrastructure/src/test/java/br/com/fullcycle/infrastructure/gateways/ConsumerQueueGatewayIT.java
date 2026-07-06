package br.com.fullcycle.infrastructure.gateways;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.domain.customer.CustomerId;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventCancelled;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ConsumerQueueGatewayIT extends IntegrationTest {

    @Autowired
    private ConsumerQueueGateway consumerQueueGateway;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        partnerRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve cancelar os ingressos de um evento ao consumir o evento de dominio EventCancelled")
    public void testCancelEventTicketsWhenEventCancelledIsPublished() throws Exception {
        // given
        final var aPartner = partnerRepository.create(
                Partner.newPartner("Disney", "45.123.123/0001-12", "disney@gmail.com")
        );

        final var anEvent = eventRepository.create(
                Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner)
        );

        final var eventId = anEvent.eventId();

        final var ticket1 = ticketRepository.create(Ticket.newTicket(CustomerId.unique(), eventId));
        final var ticket2 = ticketRepository.create(Ticket.newTicket(CustomerId.unique(), eventId));

        final var eventCancelled = new EventCancelled(eventId);
        final var content = mapper.writeValueAsString(eventCancelled);

        // when
        consumerQueueGateway.publish(content);

        // then
        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(ticket1.ticketId()).get().status());
        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(ticket2.ticketId()).get().status());
    }

    @Test
    @DisplayName("Deve ser idempotente ao reprocessar a mesma mensagem de EventCancelled")
    public void testCancelEventTicketsIsIdempotent() throws Exception {
        // given
        final var aPartner = partnerRepository.create(
                Partner.newPartner("Disney", "45.123.123/0001-12", "disney@gmail.com")
        );

        final var anEvent = eventRepository.create(
                Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner)
        );

        final var eventId = anEvent.eventId();

        final var ticket = ticketRepository.create(Ticket.newTicket(CustomerId.unique(), eventId));

        final var eventCancelled = new EventCancelled(eventId);
        final var content = mapper.writeValueAsString(eventCancelled);

        // when
        consumerQueueGateway.publish(content);
        consumerQueueGateway.publish(content);

        // then
        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(ticket.ticketId()).get().status());
    }
}
