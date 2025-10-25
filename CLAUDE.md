# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 3.3.4 + MongoDB appointment management system for healthcare professionals and patients. Uses event sourcing (outbox pattern) for notifications and Server-Sent Events (SSE) for real-time updates.

## Essential Commands

### Build & Run
```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run

# Run on specific port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### Testing
```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=AppointmentServiceTest

# Run a specific test method
mvn test -Dtest=AppointmentServiceTest#testBookAppointment
```

### MongoDB Setup
MongoDB must be running locally with these credentials:
- URI: `mongodb://appuser:apppass@localhost:27017/appointmentsdb?authSource=appointmentsdb`
- Database: `appointmentsdb`
- User: `appuser` / Password: `apppass`

### API Documentation
When the application is running, Swagger UI is available at:
- http://localhost:8080/swagger-ui/index.html

## Architecture

### Package Structure

- **`config/`** - Timezone configuration (America/Argentina/Buenos_Aires)
- **`domain/`** - MongoDB document models (entities)
- **`dto/`** - Data Transfer Objects for API contracts
- **`repo/`** - Spring Data MongoDB repositories
- **`service/`** - Business logic layer
- **`web/`** - REST controllers

### Core Domain Model

**Person** (abstract) → **Professional** / **Patient**
- `Professional`: Has specialty, can create availability blocks
- `Patient`: Has diagnostic, prescription, appointment requests

**Appointment**:
- Links professional + patient with time range (`startTs`, `endTs` as `Instant`)
- Status: `SCHEDULED` or `CANCELLED`
- Indexed: `{ professionalId, startTs, endTs, status }`

**AvailabilityBlock**:
- Defines open windows when professional is available
- Fields: `professionalId`, time range, `open` flag, `locationType`
- Indexed: `{ professionalId, startTs, endTs }`

**OutboxEvent**:
- Event sourcing for guaranteed delivery
- Processed by `OutboxPump` (scheduled every 30s)
- Currently broadcasts via SSE; intended for email/SMS/WhatsApp

### Key Business Flows

#### Slot Computation Algorithm
Available slots = OPEN blocks - SCHEDULED appointments (discretized into configurable increments, default 30min)

Location: `SlotsService.professionalSlots()` (`src/main/java/com/gerardo/appointments/service/SlotsService.java`)

The system uses an "OPEN model": no availability by default. Professionals must explicitly create availability blocks.

#### Appointment Booking
1. Patient queries slots via `/api/slots/professionals/{id}?from=...&to=...`
2. Patient books via `POST /api/appointments` with `BookRequest`
3. `AppointmentService.book()` validates no overlapping SCHEDULED appointments
4. Appointment saved with status SCHEDULED

Validation: No double-booking for same professional at same time.

#### Cancellation & Notifications
1. Call `POST /api/appointments/{id}/cancel` with `CancelRequest`
2. `AppointmentService.cancel()`:
   - Sets status to CANCELLED
   - Creates `OutboxEvent` (type: "AppointmentCancelled")
   - Broadcasts immediate SSE via `NotificationHub`
3. `OutboxPump` processes events for external delivery (TODO: email/SMS)

### Time Handling

- **Storage**: All timestamps stored as `Instant` (UTC) in MongoDB
- **API**: Accepts/returns `OffsetDateTime` (ISO-8601 with timezone)
- **Configuration**: `app.tz-availability` sets business timezone (default: America/Argentina/Buenos_Aires)
- **Jackson**: Automatic serialization via `jackson-datatype-jsr310`

### ID Strategy

All entities use String-based MongoDB ObjectIds (migrated from Long). When working with IDs:
- Use `String` type in DTOs and API contracts
- MongoDB auto-generates ObjectId strings on creation
- Recent fix (commit 681c4e1): converted long ids to string ids

### Repository Naming Convention

Note: Some repositories have duplicate interfaces (legacy):
- **Primary (use these)**: `AppointmentRepo`, `AvailabilityBlockRepo`, `PatientRepo`, `ProfessionalRepo`
- **Legacy**: `AppointmentRepository`, `AvailabilityBlockRepository`

Always use the shorter-named primary repositories.

### Critical Indexes

Ensure these compound indexes exist for performance:
- **appointments**: `{ professionalId: 1, startTs: 1, endTs: 1, status: 1 }`
- **availability_blocks**: `{ professionalId: 1, startTs: 1, endTs: 1 }`

These enable efficient range queries in slot computation.

## Configuration

Main config: `src/main/resources/application.yml`

Key properties:
```yaml
app:
  overbook-limit: 3  # Max overlapping bookings per slot (optional)
  tz-availability: America/Argentina/Buenos_Aires

server:
  address: 0.0.0.0  # Important for WSL environments
```

## Testing Strategy

No tests currently exist. When creating tests:

- **Service layer**: Unit tests with Mockito-mocked repositories
- **Slot computation**: Algorithm tests with various time ranges and edge cases
- **Controllers**: Use `@WebMvcTest` with MockMvc
- **Repositories**: Integration tests with `@DataMongoTest`

Dependencies already configured: JUnit 5, Mockito, Spring Boot Test

## Development Notes

### Current Limitations
- No authentication/authorization implemented
- OutboxPump creates events but external delivery (email/SMS) is TODO
- Deleting availability blocks doesn't cascade to appointments

### Slot Query Parameters
When working with slot endpoints:
- `from` / `to`: ISO-8601 timestamps (OffsetDateTime)
- `slotMinutes`: Duration of each slot (default 30)
- `include`: `available` | `booked` | `all` (professional view only)

### Real-time Notifications
SSE endpoint: `GET /api/notifications/stream?patientId={id}`
- Returns `text/event-stream`
- Broadcasts via `NotificationHub` (in-memory)
- Currently only supports patient notifications

### Lombok
Heavy use of Lombok annotations:
- `@Data` on DTOs (implies @Getter, @Setter, @ToString, @EqualsAndHashCode)
- `@RequiredArgsConstructor` for service constructors (field injection)
- `@Builder` for complex object construction

When adding fields to entities, ensure Lombok annotations generate correct code.
