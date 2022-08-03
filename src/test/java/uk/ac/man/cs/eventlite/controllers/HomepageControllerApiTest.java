package uk.ac.man.cs.eventlite.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

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
@WebMvcTest(HomepageControllerApi.class)
@Import({ Security.class, EventModelAssembler.class, VenueModelAssembler.class })
class HomepageControllerApiTest {

	@Autowired
	private MockMvc mvc;

	@MockBean
	private EventService eventService;
	
	@MockBean
	private VenueService venueService;
	
//	@Test
//	public void getIndexWhenNoEvents() throws Exception {
//		when(eventService.findTop3ByOrderByDateAscTimeAsc(LocalDate.now(), LocalTime.now())).thenReturn(Collections.<Event>emptyList());
//
//		mvc.perform(get("/api/upcoming_events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
//				.andExpect(handler().methodName("getUpcomingEvents")).andExpect(jsonPath("$.length()", equalTo(1)))
//				.andExpect(jsonPath("$._links.self.href", endsWith("/api/upcoming_events")));
//	}
//
//	@Test
//	public void getIndexWithEvents() throws Exception {
//		Event e = new Event();
//		e.setDate(LocalDate.now().plusYears(1));
//		when(eventService.findTop3ByOrderByDateAscTimeAsc(LocalDate.now(), LocalTime.now())).thenReturn(Collections.<Event>singletonList(e));
//		
//		mvc.perform(get("/api/upcoming_events").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
//				.andExpect(handler().methodName("getUpcomingEvents")).andExpect(jsonPath("$.length()", equalTo(2)))
//				.andExpect(jsonPath("$._links.self.href", endsWith("/api/upcoming_events")))
//				.andExpect(jsonPath("$._embedded.events.length()", equalTo(1)));
//	}
	
	@Test
	public void getIndexWhenNoVenues() throws Exception {
		when(venueService.findAllByOrderByNumOfEvents()).thenReturn(Collections.<Venue>emptyList());

		mvc.perform(get("/api/popular_venues").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getPopularVenues")).andExpect(jsonPath("$.length()", equalTo(1)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/popular_venues")));

		verify(venueService).findAllByOrderByNumOfEvents();
	}
	
	@Test
	public void getIndexWithVenues() throws Exception {
		Venue v = new Venue();
		when(venueService.findAllByOrderByNumOfEvents()).thenReturn(Collections.<Venue>singletonList(v));
		
		mvc.perform(get("/api/popular_venues").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(handler().methodName("getPopularVenues")).andExpect(jsonPath("$.length()", equalTo(2)))
				.andExpect(jsonPath("$._links.self.href", endsWith("/api/popular_venues")))
				.andExpect(jsonPath("$._embedded.venues.length()", equalTo(1)));

		verify(venueService).findAllByOrderByNumOfEvents();
	}
}
