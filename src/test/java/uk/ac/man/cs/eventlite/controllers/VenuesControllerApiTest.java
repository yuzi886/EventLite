package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import uk.ac.man.cs.eventlite.assemblers.VenueModelAssembler;
import uk.ac.man.cs.eventlite.assemblers.EventModelAssembler;
import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@ExtendWith(SpringExtension.class)
@WebMvcTest(VenuesControllerApi.class)
@Import({ Security.class, VenueModelAssembler.class, EventModelAssembler.class })
public class VenuesControllerApiTest {
	
	private final static String BAD_ROLE = "USER";
	
	@Autowired
	private MockMvc mvc;

	@MockBean
	private VenueService venueService;
	
	@MockBean
	private EventService eventService;

	@Test
	public void getIndexWhenNoVenues() throws Exception {
		when(venueService.findAllByOrderByalphabet()).thenReturn(Collections.<Venue>emptyList());

		mvc.perform(get("/api/venues").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getAllVenue")).andExpect(jsonPath("$.length()", equalTo(1)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues")));

		verify(venueService).findAllByOrderByalphabet();
	}

	@Test
	public void getIndexWithVenues() throws Exception {
		Venue v = new Venue();
		v.setId(1);
		v.setName("Kilburn Building");
		v.setAddress("Oxford Road");
		v.setPostcode("M139PL");
		v.setCapacity(120);
		when(venueService.findAllByOrderByalphabet()).thenReturn(Collections.<Venue>singletonList(v));

		mvc.perform(get("/api/venues").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getAllVenue")).andExpect(jsonPath("$.length()", equalTo(2)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues")))
				.andExpect(jsonPath("$._links.profile.href", endsWith("/api/profile/venues")))
				.andExpect(jsonPath("$._embedded.venues.length()", equalTo(1)));

		verify(venueService).findAllByOrderByalphabet();
	}
	
	@Test
	public void getVenue() throws Exception {
		Venue v = new Venue();
		v.setId(1);
		when(venueService.existsById(1)).thenReturn(true);
		when(venueService.findByIdV(1)).thenReturn(v);
		
		mvc.perform(get("/api/venues/1").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(handler().methodName("getVenue"))
				.andExpect(jsonPath("$.id", equalTo(1))).andExpect(jsonPath("$._links.self.href", endsWith("/api/venues/1")))
				.andExpect(jsonPath("$._links.venue.href", endsWith("/api/venues/1")))
				.andExpect(jsonPath("$._links.events.href", endsWith("/api/venues/1/events")))
				.andExpect(jsonPath("$._links.next3events.href", endsWith("/api/venues/1/next3events")));
	}

	@Test
	public void getVenueNotFound() throws Exception {
		mvc.perform(get("/api/venues/99").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("venue 99"))).andExpect(jsonPath("$.id", equalTo(99)))
				.andExpect(handler().methodName("getVenue"));
	}
	
	@Test
	public void getNext3EventsWhenNoEvents() throws Exception {
		Venue v = new Venue();
		v.setId(1);
		when(venueService.existsById(1)).thenReturn(true);
		when(eventService.getnext3events(1)).thenReturn(Collections.<Event>emptyList());

		mvc.perform(get("/api/venues/1/next3events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getnext3events")).andExpect(jsonPath("$.length()", equalTo(1)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues/1/next3events")));

		verify(eventService).getnext3events(1);
	}
	
	@Test
	public void getNext3EventsWithEvents() throws Exception {
		Venue v = new Venue();
		Event e = new Event();
		v.setId(1);
		e.setVenue(v);
		when(venueService.existsById(1)).thenReturn(true);
		when(eventService.getnext3events(1)).thenReturn(Collections.<Event>singletonList(e));

		mvc.perform(get("/api/venues/1/next3events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getnext3events")).andExpect(jsonPath("$.length()", equalTo(2)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues/1/next3events")))
				.andExpect(jsonPath("$._embedded.events.length()", equalTo(1)));
		
		verify(eventService).getnext3events(1);
	}
	
	@Test
	public void getNext3EventsNotFound() throws Exception {
		mvc.perform(get("/api/venues/99/next3events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("venue 99"))).andExpect(jsonPath("$.id", equalTo(99)))
				.andExpect(handler().methodName("getnext3events"));
	}
	
	@Test
	public void getVenueEventsWhenNoEvents() throws Exception {
		Venue v = new Venue();
		v.setId(1);
		when(venueService.existsById(1)).thenReturn(true);
		when(venueService.findByIdV(1)).thenReturn(v);
		when(eventService.findAllByOrderByDateAndalphabet()).thenReturn(Collections.<Event>emptyList());

		mvc.perform(get("/api/venues/1/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getVenueEvents")).andExpect(jsonPath("$.length()", equalTo(1)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues/1/events")));

		verify(venueService).existsById(1);
		verify(venueService).findByIdV(1);
		verify(eventService).findAllByOrderByDateAndalphabet();
	}
	
	@Test
	public void getVenueEventsWithEventsNotAtVenue() throws Exception {
		Venue v = new Venue();
		Event e = new Event();
		v.setId(1);
		e.setVenue(new Venue());
		when(venueService.existsById(1)).thenReturn(true);
		when(venueService.findByIdV(1)).thenReturn(v);
		when(eventService.findAllByOrderByDateAndalphabet()).thenReturn(Collections.<Event>singletonList(e));

		mvc.perform(get("/api/venues/1/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getVenueEvents")).andExpect(jsonPath("$.length()", equalTo(1)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues/1/events")));
		
		verify(venueService).existsById(1);
		verify(venueService).findByIdV(1);
		verify(eventService).findAllByOrderByDateAndalphabet();
	}
	
	@Test
	public void getVenueEventsWithEvents() throws Exception {
		Venue v = new Venue();
		Event e = new Event();
		v.setId(1);
		e.setVenue(v);
		when(venueService.existsById(1)).thenReturn(true);
		when(venueService.findByIdV(1)).thenReturn(v);
		when(eventService.findAllByOrderByDateAndalphabet()).thenReturn(Collections.<Event>singletonList(e));

		mvc.perform(get("/api/venues/1/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getVenueEvents")).andExpect(jsonPath("$.length()", equalTo(2)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/venues/1/events")))
				.andExpect(jsonPath("$._embedded.events.length()", equalTo(1)));
		
		verify(venueService).existsById(1);
		verify(venueService).findByIdV(1);
		verify(eventService).findAllByOrderByDateAndalphabet();
	}
	
	@Test
	public void getVenueEventsNotFound() throws Exception {
		when(venueService.existsById(99)).thenReturn(false);
		
		mvc.perform(get("/api/venues/99/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("venue 99"))).andExpect(jsonPath("$.id", equalTo(99)))
				.andExpect(handler().methodName("getVenueEvents"));
	}
	
	@Test
	public void getNewVenue() throws Exception {
		mvc.perform(get("/api/venues/add_venue").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotAcceptable())
				.andExpect(handler().methodName("newVenue"));
	}
	
	@Test
	public void postVenue() throws Exception {
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());

		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Venue Test\", \"address\": \"1 Some Street\", \"postcode\": \"X11 1XX\", \"capacity\": \"100\" }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andExpect(content().string(""))
				.andExpect(header().string("Location", containsString("/api/venues/")))
				.andExpect(handler().methodName("createVenue"));

		verify(venueService).save(arg.capture());
		assertThat("Venue Test", equalTo(arg.getValue().getName()));
		assertThat("1 Some Street", equalTo(arg.getValue().getAddress()));
		assertThat("X11 1XX", equalTo(arg.getValue().getPostcode()));
		assertThat(100, equalTo(arg.getValue().getCapacity()));
	}

	@Test
	public void postVenueNoAuth() throws Exception {
		mvc.perform(post("/api/venues").contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Venue Test\", \"address\": \"1 Some Street\", \"postcode\": \"X11 1XX\", \"capacity\": \"100\" }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void postVenueBadAuth() throws Exception {
		mvc.perform(post("/api/venues").with(anonymous()).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Venue Test\", \"address\": \"1 Some Street\", \"postcode\": \"X11 1XX\", \"capacity\": \"100\" }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void postVenueBadRole() throws Exception {
		mvc.perform(post("/api/venues").with(user("Rob").roles(BAD_ROLE)).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Venue Test\", \"address\": \"1 Some Street\", \"postcode\": \"X11 1XX\", \"capacity\": \"100\" }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void postBadVenueNoName() throws Exception {
		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"address\": \"1 Some Street\", \"postcode\": \"X11 1XX\", \"capacity\": \"100\" }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("createVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void postBadVenueNoAddr() throws Exception {
		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Venue Test\", \"postcode\": \"X11 1XX\", \"capacity\": \"100\" }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("createVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void postBadVenueNoPCD() throws Exception {
		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Venue Test\", \"address\": \"1 Some Street\", \"capacity\": \"100\" }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("createVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void postBadVenueNoCapacity() throws Exception {
		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Venue Test\", \"address\": \"1 Some Street\", \"postcode\": \"X11 1XX\" }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("createVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void postBadVenueLongName() throws Exception {
		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Venue Test that is over 255 characters (256 characters) abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijk\", "
						+ "\"address\": \"1 Some Street\", \"postcode\": \"X11 1XX\", \"capacity\": \"100\" }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("createVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void postBadVenueLongAddr() throws Exception {
		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Venue Test\", \"address\": \"Address of the venue which is over 299 characters (300 characters) "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopq\","
						+ " \"postcode\": \"X11 1XX\", \"capacity\": \"100\" }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("createVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void postBadVenueNegativeCapacity() throws Exception {
		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Venue Test\", \"address\": \"1 Some Street\", \"postcode\": \"X11 1XX\", \"capacity\": \"-1\" }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("createVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}
	

	@Test
	public void postEmptyVenue() throws Exception {
		mvc.perform(post("/api/venues").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON).content("{ }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("createVenue"));

		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void deleteVenue() throws Exception {
	    when(venueService.existsById(1)).thenReturn(true);
	    when(venueService.existsByIdAndEventsIsEmpty(1)).thenReturn(true);
	    
	    mvc.perform(delete("/api/venues/1").with(user("Adm").roles(Security.ADMIN_ROLE)).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent())
	            .andExpect(content().string("")).andExpect(handler().methodName("deleteVenue"));
	    
	    verify(venueService).deleteById(1);
	}
	
	@Test
	public void deleteVenueConflict() throws Exception {
	    when(venueService.existsById(1)).thenReturn(true);
	    when(venueService.existsByIdAndEventsIsEmpty(1)).thenReturn(false);
	    
	    mvc.perform(delete("/api/venues/1").with(user("Adm").roles(Security.ADMIN_ROLE)).accept(MediaType.APPLICATION_JSON)).andExpect(status().isConflict())
	    		.andExpect(jsonPath("$.error", containsString("venue 1"))).andExpect(jsonPath("$.id", equalTo(1)))
	            .andExpect(handler().methodName("deleteVenue"));
	    
	    verify(venueService, never()).deleteById(1);
	}
	
	@Test
	public void deleteVenueNotFound() throws Exception {
	    when(venueService.existsById(1)).thenReturn(false);
	    
	    mvc.perform(delete("/api/venues/1").with(user("Adm").roles(Security.ADMIN_ROLE)).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
	    		.andExpect(jsonPath("$.error", containsString("venue 1"))).andExpect(jsonPath("$.id", equalTo(1)))
	            .andExpect(handler().methodName("deleteVenue"));
	    
	    verify(venueService, never()).deleteById(1);
	}
}
