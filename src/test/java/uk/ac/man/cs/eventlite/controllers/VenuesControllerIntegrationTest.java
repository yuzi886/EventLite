package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.IsNot.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
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
import uk.ac.man.cs.eventlite.config.Security;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventLite.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class VenuesControllerIntegrationTest extends AbstractTransactionalJUnit4SpringContextTests {
	
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
	public void testGetAllVenues() {
		client.get().uri("/venues").accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectHeader()
			.contentTypeCompatibleWith(MediaType.TEXT_HTML).expectBody(String.class).consumeWith(result -> {
				assertThat(result.getResponseBody(), containsString("Venue"));
			});
	}
	
	@Test
	public void getAllVenuesWithSearch1() {
		client.get().uri("/venues?keyword=a").accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectHeader()
			.contentTypeCompatibleWith(MediaType.TEXT_HTML).expectBody(String.class).consumeWith(result -> {
				assertThat(result.getResponseBody(), containsString("Venue A"));
				assertThat(result.getResponseBody(), not(containsString("Venue B")));
				assertThat(result.getResponseBody(), not(containsString("Venue C")));
			});
	}
	
	@Test
	public void getAllVenuesWithSearch2() {
		client.get().uri("/venues?keyword=venue").accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectHeader()
			.contentTypeCompatibleWith(MediaType.TEXT_HTML).expectBody(String.class).consumeWith(result -> {
				assertThat(result.getResponseBody(), containsString("Venue A"));
				assertThat(result.getResponseBody(), containsString("Venue B"));
				assertThat(result.getResponseBody(), containsString("Venue C"));
			});
	}
	
	@Test
	public void testGetVenue() {
		client.get().uri("/venues/1").accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectHeader()
		.contentTypeCompatibleWith(MediaType.TEXT_HTML).expectBody(String.class).consumeWith(result -> {
			assertThat(result.getResponseBody(), containsString("Venue A"));
		});
			
	}

	@Test
	public void getVenueNotFound() {
		client.get().uri("/venues/99").accept(MediaType.TEXT_HTML).exchange().expectStatus().isNotFound().expectHeader()
				.contentTypeCompatibleWith(MediaType.TEXT_HTML).expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("99"));
				});
	}
	
	@Test
	public void getNewVenueNoUser() {
		// Should redirect to the sign-in page.
		client.get().uri("/venues/add_venue").accept(MediaType.TEXT_HTML).exchange().expectStatus().isFound()
				.expectHeader().value("Location", endsWith("/sign-in"));
	}

	@Test
	public void getNewVenueWithUser() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get().uri("/venues/add_venue")
				.accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectBody(String.class)
				.consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("_csrf"));
				});
	}
	
	@Test
	public void getUpdateVenueNoUser() {
		// Should redirect to the sign-in page.
		client.get().uri("/venues/updateVenues/1").accept(MediaType.TEXT_HTML).exchange().expectStatus().isFound()
				.expectHeader().value("Location", endsWith("/sign-in"));
	}

	@Test
	public void getUpdateVenueWithUser() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get().uri("/venues/updateVenues/1")
				.accept(MediaType.TEXT_HTML).exchange().expectStatus().isOk().expectBody(String.class)
				.consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("_csrf"));
				});
	}
	
	@Test
	public void getUpdateVenueNotFound() {
		client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get().uri("/venues/updateVenues/99")
				.accept(MediaType.TEXT_HTML).exchange().expectStatus().isNotFound().expectHeader()
				.contentTypeCompatibleWith(MediaType.TEXT_HTML).expectBody(String.class).consumeWith(result -> {
					assertThat(result.getResponseBody(), containsString("99"));
				});
	}
	
	@Test
	public void postNewVenueNoUser() {
		int currentRows = countRowsInTable("venues");
		String[] tokens = login();

		// Attempt to POST a valid venue.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Venue Test");
		form.add("address", "1 Some Street");
		form.add("postcode", "X11 1XX");
		form.add("capacity", "100");
		
		// We don't set the session ID, so have no credentials.
		// This should redirect to the sign-in page.
		client.post().uri("/venues/newVenue").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).exchange().expectStatus().isFound().expectHeader()
				.value("Location", endsWith("/sign-in"));

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}

	@Test
	@DirtiesContext
	public void postNewVenueWithUser() {
		int currentRows = countRowsInTable("venues");
		String[] tokens = login();

		// Attempt to POST a valid venue.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Venue Test");
		form.add("address", "1 Some Street");
		form.add("postcode", "X11 1XX");
		form.add("capacity", "100");

		// The session ID cookie holds our login credentials.
		client.post().uri("/venues/newVenue").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isFound().expectHeader().value("Location", endsWith("/venues"));

		// Check one row is added to the database.
		assertThat(currentRows + 1, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void postNewBadVenue() {
		int currentRows = countRowsInTable("venues");
		String[] tokens = login();

		// Attempt to POST a valid venue.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Venue Test");
		form.add("address", "1 Some Street");
		form.add("postcode", "X11 1XX");
		form.add("capacity", "-1");

		// The session ID cookie holds our login credentials.
		client.post().uri("/venues/newVenue").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isOk();
		// Location should be /venues/add_venue

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void postNewEmptyVenue() {
		int currentRows = countRowsInTable("venues");
		String[] tokens = login();

		// Attempt to POST a valid venue.
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);

		// The session ID cookie holds our login credentials.
		client.post().uri("/venues/newVenue").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isOk();
		// Location should be /venues/add_venue

		// Check nothing added to the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void postUpdateVenueNoUser() {
		int currentRows = countRowsInTable("venues");
		String[] tokens = login();

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Venue Test Updated");
		form.add("address", "2 Some Street");
		form.add("postcode", "X22 2XX");
		form.add("capacity", "200");
		
		client.post().uri("/venues/updateVenues/1").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).exchange().expectStatus().isFound().expectHeader()
				.value("Location", endsWith("/sign-in"));

		// Check not updated
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
		assertThat(1, equalTo(countRowsInTableWhere("venues", "name = 'Venue A'")));
		assertThat(0, equalTo(countRowsInTableWhere("venues", "name = 'Venue Test Updated'")));
	}

	@Test
	@DirtiesContext
	public void postUpdateVenueWithUser() {
		int currentRows = countRowsInTable("venues");
		String[] tokens = login();

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Venue Test Updated");
		form.add("address", "2 Some Street");
		form.add("postcode", "X22 2XX");
		form.add("capacity", "200");

		client.post().uri("/venues/updateVenues/1").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isFound().expectHeader().value("Location", endsWith("/venues"));

		// Check updated
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
		assertThat(0, equalTo(countRowsInTableWhere("venues", "name = 'Venue A'")));
		assertThat(1, equalTo(countRowsInTableWhere("venues", "name = 'Venue Test Updated'")));
	}
	
	@Test
	public void postUpdateBadVenue() {
		int currentRows = countRowsInTable("venues");
		String[] tokens = login();

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);
		form.add("name", "Venue Test Updated");
		form.add("address", "2 Some Street");
		form.add("postcode", "X22 2XX");
		form.add("capacity", "-1");

		client.post().uri("/venues/updateVenues/1").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isOk();

		// Check not updated
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
		assertThat(1, equalTo(countRowsInTableWhere("venues", "name = 'Venue A'")));
		assertThat(0, equalTo(countRowsInTableWhere("venues", "name = 'Venue Test Updated'")));
	}
	
	@Test
	public void postUpdateEmptyVenue() {
		int currentRows = countRowsInTable("venues");
		String[] tokens = login();

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("_csrf", tokens[0]);

		client.post().uri("/venues/updateVenues/1").accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.bodyValue(form).cookies(cookies -> {
					cookies.add(SESSION_KEY, tokens[1]);
				}).exchange().expectStatus().isOk();

		// Check not updated
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
		assertThat(1, equalTo(countRowsInTableWhere("venues", "name = 'Venue A'")));
	}

	
	@Test
	public void deleteVenueNoUser() {
		int currentRows = countRowsInTable("venues");

		// Should redirect to the sign-in page.
		client.delete().uri("/venues/3").accept(MediaType.TEXT_HTML).exchange().expectStatus().isFound()
				.expectHeader().value("Location", endsWith("/sign-in"));

		// Check that nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}

	@Test
	@DirtiesContext
	public void deleteVenueWithUser() {
		int currentRows = countRowsInTable("venues");
		String[] tokens = login();

		// The session ID cookie holds our login credentials.
		// And for a DELETE we have no body, so we pass the CSRF token in the headers.
		client.delete().uri("/venues/3").accept(MediaType.TEXT_HTML).header(CSRF_HEADER, tokens[0])
				.cookie(SESSION_KEY, tokens[1]).exchange().expectStatus().isFound().expectHeader()
				.value("Location", endsWith("/venues"));

		// Check that one row is removed from the database.
		assertThat(currentRows - 1, equalTo(countRowsInTable("venues")));
	}
	
	@Test
	public void deleteVenueConflict() {
		int currentRows = countRowsInTable("venues");
		String[] tokens = login();

		// The session ID cookie holds our login credentials.
		// And for a DELETE we have no body, so we pass the CSRF token in the headers.
		client.delete().uri("/venues/1").accept(MediaType.TEXT_HTML).header(CSRF_HEADER, tokens[0])
				.cookie(SESSION_KEY, tokens[1]).exchange().expectStatus().isFound().expectHeader()
				.value("Location", endsWith("/venues/1"));

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}

	@Test
	public void deleteVenueNotFound() {
		int currentRows = countRowsInTable("venues");
		String[] tokens = login();

		// The session ID cookie holds our login credentials.
		// And for a DELETE we have no body, so we pass the CSRF token in the headers.
		client.delete().uri("/venues/99").accept(MediaType.TEXT_HTML).header(CSRF_HEADER, tokens[0])
				.cookie(SESSION_KEY, tokens[1]).exchange().expectStatus().isNotFound();

		// Check nothing is removed from the database.
		assertThat(currentRows, equalTo(countRowsInTable("venues")));
	}
	
	private String[] login() {
		String[] tokens = new String[2];

		// Although this doesn't POST the log in form it effectively logs us in.
		// If we provide the correct credentials here, we get a session ID back which
		// keeps us logged in.
		EntityExchangeResult<String> result = client.mutate().filter(basicAuthentication("Rob", "Haines")).build().get()
				.uri("/venues").accept(MediaType.TEXT_HTML).exchange().expectBody(String.class).returnResult();
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
