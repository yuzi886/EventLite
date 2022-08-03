package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import uk.ac.man.cs.eventlite.assemblers.EventModelAssembler;
import uk.ac.man.cs.eventlite.assemblers.VenueModelAssembler;
import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@ExtendWith(SpringExtension.class)
@WebMvcTest(EventsControllerApi.class)
@Import({ Security.class, EventModelAssembler.class, VenueModelAssembler.class })
public class EventsControllerApiTest {
	
	private final static String BAD_ROLE = "USER";

	@Autowired
	private MockMvc mvc;

	@MockBean
	private EventService eventService;
	
	@MockBean
	private VenueService venueService;

	@Test
	public void getIndexWhenNoEvents() throws Exception {
		when(eventService.findAllByOrderByDateAndalphabet()).thenReturn(Collections.<Event>emptyList());

		mvc.perform(get("/api/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getAllEvents")).andExpect(jsonPath("$.length()", equalTo(1)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/events")));

		verify(eventService).findAllByOrderByDateAndalphabet();
	}

	@Test
	public void getIndexWithEvents() throws Exception {
		Event e = new Event();
		when(eventService.findAllByOrderByDateAndalphabet()).thenReturn(Collections.<Event>singletonList(e));

		mvc.perform(get("/api/events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getAllEvents")).andExpect(jsonPath("$.length()", equalTo(2)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/events")))
				.andExpect(jsonPath("$._embedded.events.length()", equalTo(1)));

		verify(eventService).findAllByOrderByDateAndalphabet();
	}
	
	@Test
	public void getEvent() throws Exception {
		Event e = new Event();
		e.setId(1);
		when(eventService.existsById(1)).thenReturn(true);
		when(eventService.findByIdE(1)).thenReturn(e);
		
		mvc.perform(get("/api/events/1").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(handler().methodName("getEvent"))
				.andExpect(jsonPath("$.id", equalTo(1))).andExpect(jsonPath("$._links.self.href", endsWith("/api/events/1")))
				.andExpect(jsonPath("$._links.event.href", endsWith("/api/events/1")))
				.andExpect(jsonPath("$._links.venue.href", endsWith("/api/events/1/venue")));
	}

	@Test
	public void getEventNotFound() throws Exception {
		mvc.perform(get("/api/events/99").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("event 99"))).andExpect(jsonPath("$.id", equalTo(99)))
				.andExpect(handler().methodName("getEvent"));
	}
	
	@Test
	public void getEventVenue() throws Exception {
		Venue v = new Venue();
		Event e = new Event();
		e.setId(1);
		e.setVenue(v);
		when(eventService.existsById(1)).thenReturn(true);
		when(eventService.findByIdE(1)).thenReturn(e);

		mvc.perform(get("/api/events/1/venue").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getEventVenue")).andExpect(jsonPath("$.length()", equalTo(2)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/events/1/venue")))
				.andExpect(jsonPath("$._embedded.venues.length()", equalTo(1)));

		verify(eventService).findByIdE(1);
	}
	
	@Test
	public void getEventVenueNotFound() throws Exception {
		when(eventService.existsById(99)).thenReturn(false);
		
		mvc.perform(get("/api/events/99/venue").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("event 99"))).andExpect(jsonPath("$.id", equalTo(99)))
				.andExpect(handler().methodName("getEventVenue"));
	}
	
	@Test
	public void getNewEvent() throws Exception {
		mvc.perform(get("/api/events/add_event").accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotAcceptable())
				.andExpect(handler().methodName("newEvent"));
	}
	
	@Test
	public void postEvent() throws Exception {
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		when(eventService.save(any(Event.class))).then(returnsFirstArg());

		mvc.perform(post("/api/events").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Event Test\", \"date\": \"2023-05-13\", \"venue\": { \"id\": \"1\" } }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andExpect(content().string(""))
				.andExpect(header().string("Location", containsString("/api/events/")))
				.andExpect(handler().methodName("createEvent"));

		verify(eventService).save(arg.capture());
		assertThat("Event Test", equalTo(arg.getValue().getName()));
		assertThat(LocalDate.parse("2023-05-13"), equalTo(arg.getValue().getDate()));
		assertThat(1L, equalTo(arg.getValue().getVenue().getId()));
	}
	
	@Test
	public void postEventWithOptional() throws Exception {
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		when(eventService.save(any(Event.class))).then(returnsFirstArg());

		mvc.perform(post("/api/events").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Event Test\", \"date\": \"2023-05-13\", \"time\": \"18:00\","
						+ " \"venue\": { \"id\": \"1\" }, \"description\": \"Description of the event\" }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andExpect(content().string(""))
				.andExpect(header().string("Location", containsString("/api/events/")))
				.andExpect(handler().methodName("createEvent"));

		verify(eventService).save(arg.capture());
		assertThat("Event Test", equalTo(arg.getValue().getName()));
		assertThat(LocalDate.parse("2023-05-13"), equalTo(arg.getValue().getDate()));
		assertThat(LocalTime.parse("18:00"), equalTo(arg.getValue().getTime()));
		assertThat(1L, equalTo(arg.getValue().getVenue().getId()));
		assertThat("Description of the event", equalTo(arg.getValue().getDescription()));
	}

	@Test
	public void postEventNoAuth() throws Exception {
		mvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Event Test\", \"date\": \"2023-05-13\", \"venue\": { \"id\": \"1\" } }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postEventBadAuth() throws Exception {
		mvc.perform(post("/api/events").with(anonymous()).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Event Test\", \"date\": \"2023-05-13\", \"venue\": { \"id\": \"1\" } }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postEventBadRole() throws Exception {
		mvc.perform(post("/api/events").with(user("Rob").roles(BAD_ROLE)).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Event Test\", \"date\": \"2023-05-13\", \"venue\": { \"id\": \"1\" } }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postBadEventNoName() throws Exception {
		mvc.perform(post("/api/events").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"date\": \"2023-05-13\", \"venue\": { \"id\": \"1\" } }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("createEvent"));

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void postBadEventNoDate() throws Exception {
		mvc.perform(post("/api/events").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Event Test\", \"venue\": { \"id\": \"1\" } }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("createEvent"));

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void postBadEventNoVenue() throws Exception {
		mvc.perform(post("/api/events").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Event Test\", \"date\": \"2023-05-13\" }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("createEvent"));

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postBadEventLongName() throws Exception {
		mvc.perform(post("/api/events").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Event Test that is over 255 characters (256 characters) abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijk\", "
						+ "\"date\": \"2023-05-13\", \"venue\": { \"id\": \"1\" } }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("createEvent"));

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void postBadEventPreviousDate() throws Exception {
		String date = LocalDate.now().toString();
		
		mvc.perform(post("/api/events").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Event Test\", \"date\": \"" + date + "\", \"venue\": { \"id\": \"1\" } }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("createEvent"));

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void postBadEventLongDesc() throws Exception {
		mvc.perform(post("/api/events").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Event Test\", \"date\": \"2023-05-13\", \"venue\": { \"id\": \"1\" },"
						+ " \"description\": \"Description of the event which is over 499 characters (500 characters) "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwx\" }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("createEvent"));

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postEmptyEvent() throws Exception {
		mvc.perform(post("/api/events").with(user("Rob").roles(Security.ADMIN_ROLE))
				.contentType(MediaType.APPLICATION_JSON).content("{ \"name\": \"\" }")
				.accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnprocessableEntity())
				.andExpect(content().string("")).andExpect(handler().methodName("createEvent"));

		verify(eventService, never()).save(any(Event.class));
	}

	
	@Test
	public void deleteEvent() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(delete("/api/events/1").with(user("Adm").roles(Security.ADMIN_ROLE)).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNoContent())
				.andExpect(content().string("")).andExpect(handler().methodName("deleteEvent"));
		
		verify(eventService).deleteById(1);
	}
	
	@Test
	public void deleteEventNotFound() throws Exception {
		when(eventService.existsById(1)).thenReturn(false);
		
		mvc.perform(delete("/api/events/1").with(user("Adm").roles(Security.ADMIN_ROLE)).accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error", containsString("event 1"))).andExpect(jsonPath("$.id", equalTo(1)))
				.andExpect(handler().methodName("deleteEvent"));
		
		verify(eventService, never()).deleteById(1);
	}
}
