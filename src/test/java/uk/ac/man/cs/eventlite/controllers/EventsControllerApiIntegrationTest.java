package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.web.reactive.server.WebTestClient;

import uk.ac.man.cs.eventlite.EventLite;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventLite.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class EventsControllerApiIntegrationTest extends AbstractTransactionalJUnit4SpringContextTests {

	@LocalServerPort
	private int port;

	private WebTestClient client;

	@BeforeEach
	public void setup() {
		client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port + "/api").build();
	}

	@Test
	public void testGetAllEvents() {
		client.get().uri("/events").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectHeader()
				.contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$._links.self.href")
				.value(endsWith("/api/events")).jsonPath("$._embedded.events.length()").value(equalTo(6));
	}
	
	@Test
	public void getEvent() {
		client.get().uri("/events/1").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody()
				.jsonPath("$.name").isEqualTo("Event Alpha").jsonPath("$._links.self.href", endsWith("/1"));
	}

	@Test
	public void getEventNotFound() {
		client.get().uri("/events/99").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound()
				.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$.error")
				.value(containsString("event 99")).jsonPath("$.id").isEqualTo(99);
	}
	
	@Test
	public void getEventVenue() {
		client.get().uri("/events/1/venue").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody()
				.jsonPath("$._embedded.venues[0].name").isEqualTo("Venue B").jsonPath("$._links.self.href", endsWith("/1/venue"));
	}

	@Test
	public void getEventVenueNotFound() {
		client.get().uri("/events/99/venue").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound()
				.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$.error")
				.value(containsString("event 99")).jsonPath("$.id").isEqualTo(99);
	}

	@Test
	public void getNewEvent() {
		client.get().uri("/events/add_event").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isEqualTo(406);
	}
	
	@Test
	public void postEventNoUser() {
		int currentRows = countRowsInTable("events");
		
		// Attempt to POST a valid event.
		client.post().uri("/events").accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{ \"name\": \"Event Test\", \"date\": \"2023-05-13\", \"venue\": { \"id\": \"1\" } }")
				.exchange().expectStatus().isUnauthorized();

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}

	@Test
	public void postEventBadUser() {
		int currentRows = countRowsInTable("events");
		
		// Attempt to POST a valid event.
		client.mutate().filter(basicAuthentication("Bad", "Person")).build().post().uri("/events")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{ \"name\": \"Event Test\", \"date\": \"2023-05-13\", \"venue\": { \"id\": \"1\" } }")
				.exchange().expectStatus().isUnauthorized();

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}

	@Test
	@DirtiesContext
	public void postEventWithUser() {
		int currentRows = countRowsInTable("events");
		
		// Attempt to POST a valid event.
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().post().uri("/events")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{ \"name\": \"Event Test\", \"date\": \"2023-05-13\", \"venue\": { \"id\": \"1\" } }")
				.exchange().expectStatus().isCreated().expectHeader().value("Location", containsString("/api/events")).expectBody().isEmpty();

		// Check one row is added to the database.
		assertThat(currentRows + 1, equalTo(countRowsInTable("events")));
	}
	
	@Test
	public void postBadEvent() {
		int currentRows = countRowsInTable("events");
		
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().post().uri("/events")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{ \"name\": \"Event Test\", \"date\": \"2021-05-13\", \"venue\": { \"id\": \"1\" } }")
				.exchange().expectStatus().isEqualTo(422);

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}
	
	@Test
	public void postEmptyEvent() {
		int currentRows = countRowsInTable("events");

		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().post().uri("/events")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{}").exchange().expectStatus().isEqualTo(422);

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}
	
	@Test
	public void deleteEventNoUser() {
		int currentRows = countRowsInTable("events");

		client.delete().uri("/events/1").accept(MediaType.APPLICATION_JSON).exchange().expectStatus()
				.isUnauthorized();

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}

	@Test
	public void deleteEventBadUser() {
		int currentRows = countRowsInTable("events");

		client.mutate().filter(basicAuthentication("Bad", "Person")).build().delete().uri("/events/1")
				.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isUnauthorized();

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}

	@Test
	@DirtiesContext
	public void deleteEventWithUser() {
		int currentRows = countRowsInTable("events");

		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().delete().uri("/events/1")
				.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNoContent().expectBody().isEmpty();

		// Check that one row is removed from the database.
		assertThat(currentRows - 1, equalTo(countRowsInTable("events")));
	}

	@Test
	public void deleteEventNotFound() {
		int currentRows = countRowsInTable("events");

		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().delete().uri("/events/99")
				.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound().expectBody()
				.jsonPath("$.error").value(containsString("event 99")).jsonPath("$.id").isEqualTo("99");

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}
}
