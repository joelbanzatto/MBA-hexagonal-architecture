package br.com.fullcycle.application.ticket;

import br.com.fullcycle.application.repository.InMemoryTicketRepository;
import br.com.fullcycle.domain.customer.CustomerId;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CancelEventTicketsUseCaseTest {

    @Test
    @DisplayName("Deve cancelar todos os ingressos de um evento")
    public void testCancelEventTickets() throws Exception {
        // given
        final var eventId = EventId.unique();
        final var otherEventId = EventId.unique();

        final var expectedTicketsCancelled = 2L;

        final var ticketRepository = new InMemoryTicketRepository();

        final var ticket1 = ticketRepository.create(Ticket.newTicket(CustomerId.unique(), eventId));
        final var ticket2 = ticketRepository.create(Ticket.newTicket(CustomerId.unique(), eventId));
        final var ticketFromOtherEvent = ticketRepository.create(Ticket.newTicket(CustomerId.unique(), otherEventId));

        final var cancelInput = new CancelEventTicketsUseCase.Input(eventId.value());

        // when
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        final var output = useCase.execute(cancelInput);

        // then
        Assertions.assertEquals(eventId.value(), output.eventId());
        Assertions.assertEquals(expectedTicketsCancelled, output.ticketsCancelled());

        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(ticket1.ticketId()).get().status());
        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(ticket2.ticketId()).get().status());
        Assertions.assertEquals(TicketStatus.PENDING, ticketRepository.ticketOfId(ticketFromOtherEvent.ticketId()).get().status());
    }

    @Test
    @DisplayName("Deve ser idempotente ao reprocessar o cancelamento dos ingressos de um evento")
    public void testCancelEventTicketsTwice() throws Exception {
        // given
        final var eventId = EventId.unique();

        final var ticketRepository = new InMemoryTicketRepository();
        final var ticket = ticketRepository.create(Ticket.newTicket(CustomerId.unique(), eventId));

        final var cancelInput = new CancelEventTicketsUseCase.Input(eventId.value());

        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        useCase.execute(cancelInput);

        // when
        final var output = useCase.execute(cancelInput);

        // then
        Assertions.assertEquals(1L, output.ticketsCancelled());
        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(ticket.ticketId()).get().status());
    }

    @Test
    @DisplayName("Não deve falhar quando o evento não possui ingressos")
    public void testCancelEventTicketsWithoutTickets() throws Exception {
        // given
        final var eventId = EventId.unique();

        final var ticketRepository = new InMemoryTicketRepository();

        final var cancelInput = new CancelEventTicketsUseCase.Input(eventId.value());

        // when
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        final var output = useCase.execute(cancelInput);

        // then
        Assertions.assertEquals(0L, output.ticketsCancelled());
    }
}
