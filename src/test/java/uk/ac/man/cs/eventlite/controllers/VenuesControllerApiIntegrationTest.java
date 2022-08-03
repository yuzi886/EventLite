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
public class VenuesControllerApiIntegrationTest extends AbstractTransactionalJUnit4SpringContextTests {
	
	@LocalServerPort
	private int port;

	private WebTestClient client;

	@BeforeEach
	public void setup() {
		client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port + "/api").build();
	}
	
	@Test
	public void testGetAllVenues() {
		client.get().uri("/venues").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectHeader()
				.contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$._links.self.href")
				.value(endsWith("/api/venues")).jsonPath("$._embedded.venues.length()").value(equalTo(3));
	}
	
	@Test
	public void getVenue() {
		client.get().uri("/venues/1").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody()
				.jsonPath("$.name").isEqualTo("Venue A").jsonPath("$._links.self.href", endsWith("/1"));
	}

	@Test
	public void getVenueNotFound() {
		client.get().uri("/venues/99").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound()
				.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$.error")
				.value(containsString("venue 99")).jsonPath("$.id").isEqualTo(99);
	}
	
	@Test
	public void getNext3Events() {
		client.get().uri("/venues/1/next3events").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody()
				.jsonPath("$._embedded.events[0].name").isEqualTo("Event Beta")
				.jsonPath("$._embedded.events[1].name").isEqualTo("Event Apple")
				.jsonPath("$._links.self.href", endsWith("/1/next3events"));
	}
	
	@Test
	public void getNext3EventsWhenNoEvents() {
		client.get().uri("/venues/3/next3events").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody()
				.jsonPath("$._embedded.events").doesNotExist()
				.jsonPath("$._links.self.href", endsWith("/3/next3events"));
	}

	@Test
	public void getNext3EventsNotFound() {
		client.get().uri("/venues/99/next3events").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound()
				.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$.error")
				.value(containsString("venue 99")).jsonPath("$.id").isEqualTo(99);
	}
	
	@Test
	public void getVenueEvents() {
		client.get().uri("/venues/1/events").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody()
				.jsonPath("$._embedded.events[0].name").isEqualTo("Event Past")
				.jsonPath("$._links.self.href", endsWith("/1/events"));
	}
	
	@Test
	public void getVenueEventsWhenNoEvents() {
		client.get().uri("/venues/3/events").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody()
				.jsonPath("$._embedded.events").doesNotExist()
				.jsonPath("$._links.self.href", endsWith("/3/events"));
	}

	@Test
	public void getVenueEventsNotFound() {
		client.get().uri("/venues/99/events").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound()
				.expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$.error")
				.value(containsString("venue 99")).jsonPath("$.id").isEqualTo(99);
	}
	
	@Test
	public void getNewVenue() {
		client.get().uri("/venues/add_venue").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isEqualTo(406);
	}
	
	@Test
	public void postVenueNoUser() {
		int currentRows = countRowsInTable("venues");
		
		// Attempt to POST a valid venue.
		client.post().uri("/venues").accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{ \"name\": \"Venue Test\", \"address\": \"1 Some Street\", \"postcode\": \"X11 1XX\", \"capacity\": \"100\" }")
				.exchange().expectStatus().isUnauthorized();

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}

	@Test
	public void postVenueBadUser() {
		// Attempt to POST a valid venue.
		client.mutate().filter(basicAuthentication("Bad", "Person")).build().post().uri("/venues")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{ \"name\": \"Venue Test\", \"address\": \"1 Some Street\", \"postcode\": \"X11 1XX\", \"capacity\": \"100\" }")
				.exchange().expectStatus().isUnauthorized();
	}

	@Test
	@DirtiesContext
	public void postVenueWithUser() {
		int currentRows = countRowsInTable("venues");
		
		// Attempt to POST a valid venue.
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().post().uri("/venues")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{ \"name\": \"Venue Test\", \"address\": \"1 Some Street\", \"postcode\": \"X11 1XX\", \"capacity\": \"100\" }")
				.exchange().expectStatus().isCreated().expectHeader().value("Location", containsString("/api/venues")).expectBody().isEmpty();

		// Check one row is added to the database.
		assertThat(currentRows + 1, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void postBadVenue() {
		int currentRows = countRowsInTable("venues");
		
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().post().uri("/venues")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{ \"name\": \"Venue Test\", \"address\": \"1 Some Street\", \"postcode\": \"X11 1XX\", \"capacity\": \"-1\" }")
				.exchange().expectStatus().isEqualTo(422);

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void postEmptyVenue() {
		int currentRows = countRowsInTable("venues");

		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().post().uri("/venues")
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{}").exchange().expectStatus().isEqualTo(422);

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void deleteVenueNoUser() {
		int currentRows = countRowsInTable("venues");

		client.delete().uri("/venues/3").accept(MediaType.APPLICATION_JSON).exchange().expectStatus()
				.isUnauthorized();

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}

	@Test
	public void deleteVenueBadUser() {
		int currentRows = countRowsInTable("venues");

		client.mutate().filter(basicAuthentication("Bad", "Person")).build().delete().uri("/venues/3")
				.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isUnauthorized();

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}

	@Test
	@DirtiesContext
	public void deleteVenueWithUser() {
		int currentRows = countRowsInTable("venues");

		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().delete().uri("/venues/3")
				.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNoContent().expectBody().isEmpty();

		// Check that one row is removed from the database.
		assertThat(currentRows - 1, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void deleteVenueConflict() {
		int currentRows = countRowsInTable("venues");

		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().delete().uri("/venues/1")
				.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isEqualTo(409).expectBody()
				.jsonPath("$.error").value(containsString("venue 1")).jsonPath("$.id").isEqualTo("1");

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}

	@Test
	public void deleteVenueNotFound() {
		int currentRows = countRowsInTable("venues");

		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().delete().uri("/venues/99")
				.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isNotFound().expectBody()
				.jsonPath("$.error").value(containsString("venue 99")).jsonPath("$.id").isEqualTo("99");

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}
	
	
}
