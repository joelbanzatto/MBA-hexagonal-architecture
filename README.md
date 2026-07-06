# MBA Hexagonal Architecture

Plataforma de ingressos usada como projeto final do curso de Arquitetura Hexagonal & Clean Architecture. O projeto é multi-módulo (Java 17 + Gradle), com `domain`, `application` e `infrastructure` segregados.

## Como subir o projeto

Pré-requisitos: Java 17 e Docker (para o MySQL).

```shell
docker-compose up -d
./gradlew :infrastructure:bootRun
```

A API sobe em `http://localhost:8080`, com REST em `/customers`, `/partners`, `/events` e um endpoint GraphQL em `/graphql` (GraphiQL habilitado).

## Como rodar a suíte de testes

Os testes de infraestrutura usam H2 em memória, não dependem do MySQL do `docker-compose`.

```shell
./gradlew test
```

Para rodar um módulo isoladamente:

```shell
./gradlew :domain:test
./gradlew :application:test
./gradlew :infrastructure:test
```

## Cancelamento de evento: onde a cascata acontece

Quando um parceiro cancela um evento, o agregado `Event` (`domain/.../event/Event.java`) transita para `EventStatus.CANCELLED` e registra o evento de domínio `EventCancelled` (`type = event.cancelled`). O `CancelEventUseCase` (`application/.../event/CancelEventUseCase.java`) só faz isso: carrega o evento, chama `event.cancel()` e persiste. Ele nunca toca em ingressos.

O `EventDatabaseRepository` grava os eventos de domínio do agregado na tabela `outbox` ao persistir o evento. O `OutboxRelay` publica esse conteúdo na fila através do `QueueGateway`, e o `ConsumerQueueGateway` roteia mensagens `event.cancelled` para o `CancelEventTicketsUseCase` (`application/.../ticket/CancelEventTicketsUseCase.java`), que busca os ingressos do evento (`TicketRepository.ticketsByEventId`) e cancela cada um. É o mesmo caminho que `EventTicketReserved` já percorre até o `CreateTicketForCustomerUseCase` — o agregado `Event` nunca chama o agregado `Ticket` diretamente, a cascata acontece inteiramente reagindo ao evento de domínio pela fila.
