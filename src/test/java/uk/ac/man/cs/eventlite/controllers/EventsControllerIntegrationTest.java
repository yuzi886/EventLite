package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.IsNot.not;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import uk.ac.man.cs.eventlite.EventLite;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventLite.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class EventsControllerIntegrationTest extends AbstractTransactionalJUnit4SpringContextTests {
	
	private static Pattern CSRF = Pattern.compile("(?s).*name=\"_csrf\".*?value=\"([^\"]+).*");
	private static String CSRF_HEADER = "X-CSRF-TOKEN";
	private static String SESSION_KEY = "JSESSIONID";

	@LocalServerPort
	private int port;

	private WebTestClient client;

	@BeforeEach
	public void setup() {
		client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
	}

	@Test
	public void testGetAllEvents() {
		client.get().uri("/events").accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectHeader()
				.contentTypeCompatibleWith(MediaType.TEXT_HTML).expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("Event"));
				});
	}

	@Test
	public void testGetAllEventsWithSearch1() {
		client.get().uri("/events?keyword=apple").accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectHeader()
				.contentTypeCompatibleWith(MediaType.TEXT_HTML).expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("Event Apple"));
					assertThat(result.getResponseBody(), not(containsString("Event Alpha")));
					assertThat(result.getResponseBody(), not(containsString("Event Beta")));
					assertThat(result.getResponseBody(), not(containsString("Event Former")));
					assertThat(result.getResponseBody(), not(containsString("Event Previous")));
					assertThat(result.getResponseBody(), not(containsString("Event Past")));
				});
	}
	
	@Test
	public void testGetAllEventsWithSearch2() {
		client.get().uri("/events?keyword=event").accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectHeader()
				.contentTypeCompatibleWith(MediaType.TEXT_HTML).expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("Event Alpha"));
					assertThat(result.getResponseBody(), containsString("Event Beta"));
					assertThat(result.getResponseBody(), containsString("Event Apple"));
					assertThat(result.getResponseBody(), containsString("Event Former"));
					assertThat(result.getResponseBody(), containsString("Event Previous"));
					assertThat(result.getResponseBody(), containsString("Event Past"));
				});
	}
	
	@Test
	public void testGetEvent() {
		client.get().uri("/events/1").accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectHeader()
				.contentTypeCompatibleWith(MediaType.TEXT_HTML).expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("Event Alpha"));
				});
	}

	@Test
	public void getEventNotFound() {
		client.get().uri("/events/99").accept(MediaType.TEXT_HTML).exchange().expectStatus().isNotFound().expectHeader()
				.contentTypeCompatibleWith(MediaType.TEXT_HTML).expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("99"));
				});
	}
	
	@Test
	public void getNewEventNoUser() {
		// Should redirect to the sign-in page.
		client.get().uri("/events/add_event").accept(MediaType.TEXT_HTML).exchange().expectStatus().isFound()
				.expectHeader().value("Location", endsWith("/sign-in"));
	}

	@Test
	public void getNewEventWithUser() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get().uri("/events/add_event")
				.accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectBody(String.class)
				.consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("_csrf"));
				});
	}
	
	@Test
	public void getUpdateEventNoUser() {
		// Should redirect to the sign-in page.
		client.get().uri("/events/updateEvents/1").accept(MediaType.TEXT_HTML).exchange().expectStatus().isFound()
				.expectHeader().value("Location", endsWith("/sign-in"));
	}

	@Test
	public void getUpdateEventWithUser() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get().uri("/events/updateEvents/1")
				.accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectBody(String.class)
				.consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("_csrf"));
				});
	}
	
	@Test
	public void getUpdateEventNotFound() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get().uri("/events/updateEvents/99")
				.accept(MediaType.TEXT_HTML).exchange().expectStatus().isNotFound().expectHeader()
				.contentTypeCompatibleWith(MediaType.TEXT_HTML).expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("99"));
				});
	}
	
	@Test
	public void postNewEventNoUser() {
		int currentRows = countRowsInTable("events");
		String[] tokens = login();

		// Attempt to POST a valid event.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Event Test");
		form.add("date", "2023-05-13");
		form.add("venue.id", "1");
		
		// We don't set the session ID, so have no credentials.
		// This should redirect to the sign-in page.
		client.post().uri("/events/newEvent").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).exchange().expectStatus().isFound().expectHeader()
				.value("Location", endsWith("/sign-in"));

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}

	@Test
	@DirtiesContext
	public void postNewEventWithUser() {
		int currentRows = countRowsInTable("events");
		String[] tokens = login();

		// Attempt to POST a valid event.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Event Test");
		form.add("date", "2023-05-13");
		form.add("venue.id", "1");

		// The session ID cookie holds our login credentials.
		client.post().uri("/events/newEvent").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isFound().expectHeader().value("Location", endsWith("/events"));

		// Check one row is added to the database.
		assertThat(currentRows + 1, equalTo(countRowsInTable("events")));
	}
	
	@Test
	public void postNewBadEvent() {
		int currentRows = countRowsInTable("events");
		String[] tokens = login();

		// Attempt to POST a valid event.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Event Test");
		form.add("date", "2021-05-13");
		form.add("venue.id", "1");

		// The session ID cookie holds our login credentials.
		client.post().uri("/events/newEvent").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isOk();
		// Location should be /events/add_event

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}
	
	@Test
	public void postNewEmptyEvent() {
		int currentRows = countRowsInTable("events");
		String[] tokens = login();

		// Attempt to POST a valid event.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);

		// The session ID cookie holds our login credentials.
		client.post().uri("/events/newEvent").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isOk();
		// Location should be /events/add_event

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}
	
	@Test
	public void postUpdateEventNoUser() {
		int currentRows = countRowsInTable("events");
		String[] tokens = login();

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Event Test Updated");
		form.add("date", "2024-05-13");
		form.add("venue.id", "2");

		client.post().uri("/events/updateEvents/1").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).exchange().expectStatus().isFound().expectHeader()
				.value("Location", endsWith("/sign-in"));

		// Check not updated
		assertThat(currentRows, equalTo(countRowsInTable("events")));
		assertThat(1, equalTo(countRowsInTableWhere("events", "name = 'Event Alpha'")));
		assertThat(0, equalTo(countRowsInTableWhere("events", "name = 'Event Test Updated'")));
	}

	@Test
	@DirtiesContext
	public void postUpdateEventWithUser() {
		int currentRows = countRowsInTable("events");
		String[] tokens = login();

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Event Test Updated");
		form.add("date", "2024-05-13");
		form.add("venue.id", "2");

		client.post().uri("/events/updateEvents/1").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isFound().expectHeader().value("Location", endsWith("/events"));

		// Check updated
		assertThat(currentRows, equalTo(countRowsInTable("events")));
		assertThat(0, equalTo(countRowsInTableWhere("events", "name = 'Event Alpha'")));
		assertThat(1, equalTo(countRowsInTableWhere("events", "name = 'Event Test Updated'")));
	}
	
	@Test
	public void postUpdateBadEvent() {
		int currentRows = countRowsInTable("events");
		String[] tokens = login();

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Event Test Updated");
		form.add("date", "2021-05-13");
		form.add("venue.id", "2");

		client.post().uri("/events/updateEvents/1").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isOk();

		// Check not updated
		assertThat(currentRows, equalTo(countRowsInTable("events")));
		assertThat(1, equalTo(countRowsInTableWhere("events", "name = 'Event Alpha'")));
		assertThat(0, equalTo(countRowsInTableWhere("events", "name = 'Event Test Updated'")));
	}
	
	@Test
	public void postUpdateEmptyEvent() {
		int currentRows = countRowsInTable("events");
		String[] tokens = login();

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "");
		form.add("date", "");
		form.add("venue.id", "");

		client.post().uri("/events/updateEvents/1").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isOk();

		// Check not updated
		assertThat(currentRows, equalTo(countRowsInTable("events")));
		assertThat(1, equalTo(countRowsInTableWhere("events", "name = 'Event Alpha'")));
	}
	
	@Test
	public void deleteEventNoUser() {
		int currentRows = countRowsInTable("events");

		// Should redirect to the sign-in page.
		client.delete().uri("/events/1").accept(MediaType.TEXT_HTML).exchange().expectStatus().isFound()
				.expectHeader().value("Location", endsWith("/sign-in"));

		// Check that nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}

	@Test
	@DirtiesContext
	public void deleteEventWithUser() {
		int currentRows = countRowsInTable("events");
		String[] tokens = login();

		// The session ID cookie holds our login credentials.
		// And for a DELETE we have no body, so we pass the CSRF token in the headers.
		client.delete().uri("/events/1").accept(MediaType.TEXT_HTML).header(CSRF_HEADER, tokens[0])
				.cookie(SESSION_KEY, tokens[1]).exchange().expectStatus().isFound().expectHeader()
				.value("Location", endsWith("/events"));

		// Check that one row is removed from the database.
		assertThat(currentRows - 1, equalTo(countRowsInTable("events")));
	}

	@Test
	public void deleteEventNotFound() {
		int currentRows = countRowsInTable("events");
		String[] tokens = login();

		// The session ID cookie holds our login credentials.
		// And for a DELETE we have no body, so we pass the CSRF token in the headers.
		client.delete().uri("/events/99").accept(MediaType.TEXT_HTML).header(CSRF_HEADER, tokens[0])
				.cookie(SESSION_KEY, tokens[1]).exchange().expectStatus().isNotFound();

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("events")));
	}

	private String[] login() {
		String[] tokens = new String[2];

		// Although this doesn't POST the log in form it effectively logs us in.
		// If we provide the correct credentials here, we get a session ID back which
		// keeps us logged in.
		EntityExchangeResult<String> result = client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get()
				.uri("/events").accept(MediaType.TEXT_HTML).exchange().expectBody(String.class).returnResult();
		tokens[0] = getCsrfToken(result.getResponseBody());
		tokens[1] = result.getResponseCookies().getFirst(SESSION_KEY).getValue();

		return tokens;
	}

	private String getCsrfToken(String body) {
		Matcher matcher = CSRF.matcher(body);

		// matcher.matches() must be called; might as well assert something as well...
		assertThat(matcher.matches(), equalTo(true));

		return matcher.group(1);
	}
}
