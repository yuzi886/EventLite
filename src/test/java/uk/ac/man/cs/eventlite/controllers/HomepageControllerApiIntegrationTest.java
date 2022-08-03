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
public class HomepageControllerApiIntegrationTest extends AbstractTransactionalJUnit4SpringContextTests {
	
	@LocalServerPort
	private int port;

	private WebTestClient client;

	@BeforeEach
	public void setup() {
		client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port + "/api").build();
	}
	
//	@Test
//	public void getUpcomingEvents() {
//		client.get().uri("/upcoming_events").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectHeader()
//				.contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$._links.self.href")
//				.value(endsWith("/api/upcoming_events")).jsonPath("$._embedded.events.length()").value(equalTo(3));
//	}
	
	@Test
	public void getPopularVenues() {
		client.get().uri("/popular_venues").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectHeader()
				.contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$._links.self.href")
				.value(endsWith("/api/popular_venues")).jsonPath("$._embedded.venues.length()").value(equalTo(3));
	}
	
	@Test
	public void getLinks() {
		client.get().uri("").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk().expectHeader()
				.contentType(MediaType.APPLICATION_JSON).expectBody()
				.jsonPath("$._links.venues.href").value(endsWith("/api/venues"))
				.jsonPath("$._links.events.href").value(endsWith("/api/events"))
				.jsonPath("$._links.profile.href").value(endsWith("/api/profile"))
				.jsonPath("$.length()").value(equalTo(1));
	}
}
