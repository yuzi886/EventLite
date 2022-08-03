package uk.ac.man.cs.eventlite.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import uk.ac.man.cs.eventlite.config.Security;
import uk.ac.man.cs.eventlite.config.data.TestDataLoader;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@ExtendWith(SpringExtension.class)
@WebMvcTest(EventsController.class)
@Import(Security.class)
public class EventsControllerTest {
	
	private final static String BAD_ROLE = "USER";

	@Autowired
	private MockMvc mvc;

	@Mock
	private Event event;

	@Mock
	private Venue venue;

	@MockBean
	private EventService eventService;

	@MockBean
	private VenueService venueService;

	@Test
	public void getIndexWhenNoEvents() throws Exception {
		when(eventService.findAllByOrderByDateAndalphabet()).thenReturn(Collections.<Event>emptyList());
		when(venueService.findAll()).thenReturn(Collections.<Venue>emptyList());

		mvc.perform(get("/events").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("events/index")).andExpect(handler().methodName("getAllEvents"));
		
		verify(eventService).findUpcomingByOrderByDateAscTimeAsc();
		verify(eventService).findPrecomingByOrderByDateAscTimeAsc();
		verifyNoInteractions(event);
		verifyNoInteractions(venue);
	}

	@Test
	public void getIndexWithEvents() throws Exception {
		Venue v = new Venue();
		when(venue.getName()).thenReturn("Kilburn Building");
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));

		when(event.getVenue()).thenReturn(v);
		when(eventService.findAllByOrderByDateAndalphabet()).thenReturn(Collections.<Event>singletonList(event));

		mvc.perform(get("/events").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("events/index")).andExpect(handler().methodName("getAllEvents"));

		verify(eventService).findUpcomingByOrderByDateAscTimeAsc();
		verify(eventService).findPrecomingByOrderByDateAscTimeAsc();
	}
	
	@Test
	public void getIndexWithKeywordWhenNoEvents() throws Exception {
		String keyword = "Event";
		
		when(eventService.getByKeyword(keyword,"u")).thenReturn(Collections.<Event>emptyList());
		when(eventService.getByKeyword(keyword,"p")).thenReturn(Collections.<Event>emptyList());

		mvc.perform(get("/events?keyword=Event").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("events/index")).andExpect(handler().methodName("getAllEvents"));
		
		verify(eventService).getByKeyword(keyword,"u");
		verify(eventService).getByKeyword(keyword,"p");
		verifyNoInteractions(event);
	}

	@Test
	public void getIndexWithKeywordWithEvents() throws Exception {
		String keyword = "Event";

		when(event.getVenue()).thenReturn(venue);
		when(eventService.getByKeyword(keyword,"u")).thenReturn(Collections.<Event>singletonList(event));
		when(eventService.getByKeyword(keyword,"p")).thenReturn(Collections.<Event>singletonList(event));

		mvc.perform(get("/events?keyword=Event").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("events/index")).andExpect(handler().methodName("getAllEvents"));

		verify(eventService).getByKeyword(keyword,"u");
		verify(eventService).getByKeyword(keyword,"p");
	}
	
	@Test
	public void getEvent() throws Exception {
		when(event.getVenue()).thenReturn(venue);
		when(eventService.findById(1)).thenReturn(Optional.of(event));
		
		mvc.perform(get("/events/1").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("events/show")).andExpect(handler().methodName("getEvent"));
		
		verify(eventService).findById(1);
	}

	@Test
	public void getEventNotFound() throws Exception {
		mvc.perform(get("/events/99").accept(MediaType.TEXT_HTML)).andExpect(status().isNotFound())
				.andExpect(view().name("events/not_found")).andExpect(handler().methodName("getEvent"));
	}
	
	@Test
	public void getNewEvent() throws Exception {
		mvc.perform(get("/events/add_event").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk()).andExpect(view().name("events/add_event"))
				.andExpect(handler().methodName("newEvent"));
	}
	
	@Test
	public void getNewEventNoAuth() throws Exception {
		mvc.perform(get("/events/add_event").accept(MediaType.TEXT_HTML)).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));
	}
	
	@Test
	public void getUpdateEvent() throws Exception {
		when(event.getVenue()).thenReturn(venue);
		when(eventService.findById(1)).thenReturn(Optional.of(event));

		mvc.perform(get("/events/updateEvents/1").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk()).andExpect(view().name("events/updateEvents"))
				.andExpect(handler().methodName("updateLockId"));
	}
	
	@Test
	public void getUpdateEventNoAuth() throws Exception {
		mvc.perform(get("/events/updateEvents/1").accept(MediaType.TEXT_HTML)).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));
	}
	
	@Test
	public void getUpdateEventNotFound() throws Exception {
		mvc.perform(get("/events/updateEvents/99").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML))
				.andExpect(status().isNotFound()).andExpect(view().name("events/not_found")).andExpect(handler().methodName("updateLockId"));
	}
	
	@Test
	public void postNewEvent() throws Exception {
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		when(eventService.save(any(Event.class))).then(returnsFirstArg());

		mvc.perform(post("/events/newEvent").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test").param("date", LocalDate.now().plusYears(1).toString()).param("venue.id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound()).andExpect(content().string(""))
				.andExpect(view().name("redirect:/events")).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("createEvent")).andExpect(flash().attributeExists("ok_message"));

		verify(eventService).save(arg.capture());
	}
	
	@Test
	public void postNewEventWithOptional() throws Exception {
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		when(eventService.save(any(Event.class))).then(returnsFirstArg());

		mvc.perform(post("/events/newEvent").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test").param("date", LocalDate.now().plusYears(1).toString()).param("time", "18:00")
				.param("venue.id", "1").param("description", "Description of the event")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound()).andExpect(content().string(""))
				.andExpect(view().name("redirect:/events")).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("createEvent")).andExpect(flash().attributeExists("ok_message"));

		verify(eventService).save(arg.capture());
	}
	
	@Test
	public void postNewEventNoAuth() throws Exception {
		mvc.perform(post("/events/newEvent").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test").param("date", LocalDate.now().plusYears(1).toString()).param("venue.id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postNewEventBadRole() throws Exception {
		mvc.perform(post("/events/newEvent").with(user("Rob").roles(BAD_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test").param("date", LocalDate.now().plusYears(1).toString()).param("venue.id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isForbidden());

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postNewEventNoCsrf() throws Exception {
		mvc.perform(post("/events/newEvent").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test").param("date", LocalDate.now().plusYears(1).toString()).param("venue.id", "1")
				.accept(MediaType.TEXT_HTML)).andExpect(status().isForbidden());

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postNewBadEventNoName() throws Exception {
		mvc.perform(post("/events/newEvent").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("date", LocalDate.now().plusYears(1).toString()).param("venue.id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/add_event"))
				.andExpect(model().attributeHasFieldErrors("event", "name"))
				.andExpect(handler().methodName("createEvent")).andExpect(flash().attributeCount(0));

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void postNewBadEventNoDate() throws Exception {
		mvc.perform(post("/events/newEvent").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test").param("venue.id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/add_event"))
				.andExpect(model().attributeHasFieldErrors("event", "date"))
				.andExpect(handler().methodName("createEvent")).andExpect(flash().attributeCount(0));

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void postNewBadEventNoVenue() throws Exception {
		mvc.perform(post("/events/newEvent").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test").param("date", LocalDate.now().plusYears(1).toString())
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/add_event"))
				.andExpect(model().attributeHasFieldErrors("event", "venue"))
				.andExpect(handler().methodName("createEvent")).andExpect(flash().attributeCount(0));

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postNewBadEventLongName() throws Exception {
		mvc.perform(post("/events/newEvent").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test that is over 255 characters (256 characters) abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijk")
				.param("date", LocalDate.now().plusYears(1).toString()).param("venue.id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/add_event"))
				.andExpect(model().attributeHasFieldErrors("event", "name"))
				.andExpect(handler().methodName("createEvent")).andExpect(flash().attributeCount(0));

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void postNewBadEventPreviousDate() throws Exception {
		mvc.perform(post("/events/newEvent").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test").param("date", LocalDate.now().toString()).param("venue.id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/add_event"))
				.andExpect(model().attributeHasFieldErrors("event", "date"))
				.andExpect(handler().methodName("createEvent")).andExpect(flash().attributeCount(0));

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void postNewBadEventLongDesc() throws Exception {
		mvc.perform(post("/events/newEvent").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test").param("date", LocalDate.now().plusYears(1).toString()).param("venue.id", "1")
				.param("description", "Description of the event which is over 499 characters (500 characters) "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwx")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/add_event"))
				.andExpect(model().attributeHasFieldErrors("event", "description"))
				.andExpect(handler().methodName("createEvent")).andExpect(flash().attributeCount(0));

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postNewEmptyEvent() throws Exception {
		mvc.perform(post("/events/newEvent").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/add_event"))
				.andExpect(model().attributeHasFieldErrors("event", "name"))
				.andExpect(model().attributeHasFieldErrors("event", "date"))
				.andExpect(model().attributeHasFieldErrors("event", "venue"))
				.andExpect(handler().methodName("createEvent")).andExpect(flash().attributeCount(0));

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void postUpdateEvent() throws Exception {
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		when(eventService.save(any(Event.class))).then(returnsFirstArg());

		mvc.perform(post("/events/updateEvents/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test").param("date", LocalDate.now().plusYears(1).toString()).param("venue.id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound()).andExpect(content().string(""))
				.andExpect(view().name("redirect:/events")).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("updateEvents")).andExpect(flash().attributeExists("ok_message"));

		verify(eventService).save(arg.capture());
	}
	
	@Test
	public void postUpdateEventWithOptional() throws Exception {
		ArgumentCaptor<Event> arg = ArgumentCaptor.forClass(Event.class);
		when(eventService.save(any(Event.class))).then(returnsFirstArg());

		mvc.perform(post("/events/updateEvents/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test").param("date", LocalDate.now().plusYears(1).toString()).param("time", "18:00")
				.param("venue.id", "1").param("description", "Description of the event")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound()).andExpect(content().string(""))
				.andExpect(view().name("redirect:/events")).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("updateEvents")).andExpect(flash().attributeExists("ok_message"));

		verify(eventService).save(arg.capture());
	}
	
	@Test
	public void postUpdateEventNoAuth() throws Exception {
		mvc.perform(post("/events/updateEvents/1").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test").param("date", LocalDate.now().plusYears(1).toString()).param("venue.id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postUpdateEventBadRole() throws Exception {
		mvc.perform(post("/events/updateEvents/1").with(user("Rob").roles(BAD_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test").param("date", LocalDate.now().plusYears(1).toString()).param("venue.id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isForbidden());

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postUpdateEventNoCsrf() throws Exception {
		mvc.perform(post("/events/updateEvents/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test").param("date", LocalDate.now().plusYears(1).toString()).param("venue.id", "1")
				.accept(MediaType.TEXT_HTML)).andExpect(status().isForbidden());

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postUpdateBadEventNoName() throws Exception {
		mvc.perform(post("/events/updateEvents/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("date", LocalDate.now().plusYears(1).toString()).param("venue.id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/updateEvents"))
				.andExpect(model().attributeHasFieldErrors("event", "name"))
				.andExpect(handler().methodName("updateEvents")).andExpect(flash().attributeCount(0));

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void postUpdateBadEventNoDate() throws Exception {
		mvc.perform(post("/events/updateEvents/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test").param("venue.id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/updateEvents"))
				.andExpect(model().attributeHasFieldErrors("event", "date"))
				.andExpect(handler().methodName("updateEvents")).andExpect(flash().attributeCount(0));

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void postUpdateBadEventNoVenue() throws Exception {
		mvc.perform(post("/events/updateEvents/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test").param("date", LocalDate.now().plusYears(1).toString())
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/updateEvents"))
				.andExpect(model().attributeHasFieldErrors("event", "venue"))
				.andExpect(handler().methodName("updateEvents")).andExpect(flash().attributeCount(0));

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postUpdateBadEventLongName() throws Exception {
		mvc.perform(post("/events/updateEvents/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test that is over 255 characters (256 characters) abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijk")
				.param("date", LocalDate.now().plusYears(1).toString()).param("venue.id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/updateEvents"))
				.andExpect(model().attributeHasFieldErrors("event", "name"))
				.andExpect(handler().methodName("updateEvents")).andExpect(flash().attributeCount(0));

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void postUpdateBadEventPreviousDate() throws Exception {
		mvc.perform(post("/events/updateEvents/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test").param("date", LocalDate.now().toString()).param("venue.id", "1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/updateEvents"))
				.andExpect(model().attributeHasFieldErrors("event", "date"))
				.andExpect(handler().methodName("updateEvents")).andExpect(flash().attributeCount(0));

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void postUpdateBadEventLongDesc() throws Exception {
		mvc.perform(post("/events/updateEvents/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Event Test").param("date", LocalDate.now().plusYears(1).toString()).param("venue.id", "1")
				.param("description", "Description of the event which is over 499 characters (500 characters) "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwx")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/updateEvents"))
				.andExpect(model().attributeHasFieldErrors("event", "description"))
				.andExpect(handler().methodName("updateEvents")).andExpect(flash().attributeCount(0));

		verify(eventService, never()).save(any(Event.class));
	}

	@Test
	public void postUpdateEmptyEvent() throws Exception {
		mvc.perform(post("/events/updateEvents/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("events/updateEvents"))
				.andExpect(model().attributeHasFieldErrors("event", "name"))
				.andExpect(model().attributeHasFieldErrors("event", "date"))
				.andExpect(model().attributeHasFieldErrors("event", "venue"))
				.andExpect(handler().methodName("updateEvents")).andExpect(flash().attributeCount(0));

		verify(eventService, never()).save(any(Event.class));
	}
	
	@Test
	public void deleteEvent() throws Exception {
		when(eventService.existsById(1)).thenReturn(true);
		
		mvc.perform(delete("/events/1").with(user("Adm").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(view().name("redirect:/events")).andExpect(handler().methodName("deleteEvent")).andExpect(flash().attributeExists("ok_message"));
		
		verify(eventService).deleteById(1);
	}
	
	@Test
	public void deleteEventNotFound() throws Exception {
		when(eventService.existsById(1)).thenReturn(false);
		
		mvc.perform(delete("/events/1").with(user("Adm").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isNotFound())
				.andExpect(view().name("events/not_found")).andExpect(handler().methodName("deleteEvent"));
		
		verify(eventService, never()).deleteById(1);
	}

	@Test
	public void searchTest() throws Exception 
	{
		Venue v = new Venue();
		v.setId(1);
		v.setName("Kilburn Building");
		v.setCapacity(120);
		Venue v1 = new Venue();
		v1.setId(2);
		v1.setName("Online");
		v1.setCapacity(100000);
		venueService.save(v);
		venueService.save(v1);
	
		Event e1 = new Event();
		e1.setId(1);
		e1.setName("COMP23412 Showcase (group F)");
		e1.setDate(LocalDate.parse("2022-05-17"));
		e1.setTime(LocalTime.parse("16:00"));
		e1.setVenue(v);
		eventService.save(e1);
		
		Event e2 = new Event();
		e2.setId(2);
		e2.setName("COMP23412 Showcase (group G)");
		e2.setDate(LocalDate.parse("2022-05-19"));
		e2.setTime(LocalTime.parse("16:00"));
		e2.setVenue(v);
		eventService.save(e2);
		
		Event e3 = new Event();
		e3.setId(3);
		e3.setName("COMP23412 Showcase (group H)");
		e3.setDate(LocalDate.parse("2022-05-20"));
		e3.setTime(LocalTime.parse("16:00"));
		e3.setVenue(v);
		eventService.save(e3);
		
		assertEquals(eventService.getByKeyword("h"), eventService.getByKeyword("H"));
		
		assertEquals(eventService.getByKeyword("COMP"),eventService.findAllByOrderByDateAndalphabet());
		
	}
}
