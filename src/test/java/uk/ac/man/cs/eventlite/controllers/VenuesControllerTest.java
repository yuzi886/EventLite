package uk.ac.man.cs.eventlite.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
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

@ExtendWith(SpringExtension.class)
@WebMvcTest(VenuesController.class)
@Import(Security.class)
public class VenuesControllerTest {
	
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
	public void getIndexWhenNoVenues() throws Exception {
		when(venueService.findAll()).thenReturn(Collections.<Venue>emptyList());

		mvc.perform(get("/venues").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("venues/index")).andExpect(handler().methodName("getAllVenue"));

		verify(venueService).findAllByOrderByalphabet();
		verifyNoInteractions(venue);
	}

	@Test
	public void getIndexWithVenues() throws Exception {
		Venue v = new Venue();
		when(venue.getName()).thenReturn("Kilburn Building");
		when(venueService.findAll()).thenReturn(Collections.<Venue>singletonList(venue));
		mvc.perform(get("/venues").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("venues/index")).andExpect(handler().methodName("getAllVenue"));

		verify(venueService).findAllByOrderByalphabet();
	}
	
	@Test
	public void getIndexWithKeywordWhenNoVenues() throws Exception {
		String keyword = "Venue";

		when(venueService.getByKeyword(keyword)).thenReturn(Collections.<Venue>emptyList());

		mvc.perform(get("/venues?keyword=Venue").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("venues/index")).andExpect(handler().methodName("getAllVenue"));

		verify(venueService).getByKeyword(keyword);
		verifyNoInteractions(venue);
	}

	@Test
	public void getIndexWithKeywordWithVenues() throws Exception {
		String keyword = "Venue";

		when(venueService.getByKeyword(keyword)).thenReturn(Collections.<Venue>singletonList(venue));
		
		mvc.perform(get("/venues?keyword=Venue").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("venues/index")).andExpect(handler().methodName("getAllVenue"));

		verify(venueService).getByKeyword(keyword);
	}
	
	@Test
	public void getVenue() throws Exception {
		when(venueService.findById(1)).thenReturn(Optional.of(venue));
		
		mvc.perform(get("/venues/1").accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
				.andExpect(view().name("venues/show")).andExpect(handler().methodName("getVenue"));
		
		verify(venueService).findById(1);
	}

	@Test
	public void getVenueNotFound() throws Exception {
		mvc.perform(get("/venues/99").accept(MediaType.TEXT_HTML)).andExpect(status().isNotFound())
				.andExpect(view().name("venues/not_found")).andExpect(handler().methodName("getVenue"));
	}
	
	@Test
	public void getNewVenue() throws Exception {
		mvc.perform(get("/venues/add_venue").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk()).andExpect(view().name("venues/add_venue"))
				.andExpect(handler().methodName("newVenue"));
	}
	
	@Test
	public void getNewVenueNoAuth() throws Exception {
		mvc.perform(get("/venues/add_venue").accept(MediaType.TEXT_HTML)).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));
	}
	
	@Test
	public void getUpdateVenue() throws Exception {
		when(venueService.findById(1)).thenReturn(Optional.of(venue));
		
		mvc.perform(get("/venues/updateVenues/1").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk()).andExpect(view().name("venues/updateVenues"))
				.andExpect(handler().methodName("updateLockId"));
	}
	
	@Test
	public void getUpdateVenueNoAuth() throws Exception {
		mvc.perform(get("/venues/updateVenues/1").accept(MediaType.TEXT_HTML)).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));
	}
	
	@Test
	public void getUpdateVenueNotFound() throws Exception {
		mvc.perform(get("/venues/updateVenues/1").with(user("Rob").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML))
				.andExpect(status().isNotFound()).andExpect(view().name("venues/not_found"))
				.andExpect(handler().methodName("updateLockId"));
	}
	
	@Test
	public void postNewVenue() throws Exception {		
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());

		mvc.perform(post("/venues/newVenue").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test").param("address", "1 Some Street").param("postcode", "X11 1XX").param("capacity", "100")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound()).andExpect(content().string(""))
				.andExpect(view().name("redirect:/venues")).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeExists("ok_message"));

		verify(venueService).save(arg.capture());
	}
	
	@Test
	public void postNewVenueNoAuth() throws Exception {
		mvc.perform(post("/venues/newVenue").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test").param("address", "1 Some Street").param("postcode", "X11 1XX").param("capacity", "100")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void postNewVenueBadRole() throws Exception {
		mvc.perform(post("/venues/newVenue").with(user("Rob").roles(BAD_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test").param("address", "1 Some Street").param("postcode", "X11 1XX").param("capacity", "100")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isForbidden());

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void postNewVenueNoCsrf() throws Exception {
		mvc.perform(post("/venues/newVenue").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test").param("address", "1 Some Street").param("postcode", "X11 1XX").param("capacity", "100")
				.accept(MediaType.TEXT_HTML)).andExpect(status().isForbidden());

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void postNewBadVenueNoName() throws Exception {
		mvc.perform(post("/venues/newVenue").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("address", "1 Some Street").param("postcode", "X11 1XX").param("capacity", "100")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/add_venue"))
				.andExpect(model().attributeHasFieldErrors("venue", "name"))
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeCount(0));

		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void postNewBadVenueNoAddr() throws Exception {
		mvc.perform(post("/venues/newVenue").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test").param("postcode", "X11 1XX").param("capacity", "100")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/add_venue"))
				.andExpect(model().attributeHasFieldErrors("venue", "address"))
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeCount(0));

		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void postNewBadVenueNoPCD() throws Exception {
		mvc.perform(post("/venues/newVenue").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test").param("address", "1 Some Street").param("capacity", "100")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/add_venue"))
				.andExpect(model().attributeHasFieldErrors("venue", "postcode"))
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeCount(0));

		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void postNewBadVenueNoCapacity() throws Exception {
		mvc.perform(post("/venues/newVenue").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test").param("address", "1 Some Street").param("postcode", "X11 1XX")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/add_venue"))
				.andExpect(model().attributeHasFieldErrors("venue", "capacity"))
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeCount(0));

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void postNewBadVenueLongName() throws Exception {
		mvc.perform(post("/venues/newVenue").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test that is over 255 characters (256 characters) abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijk")
				.param("address", "1 Some Street").param("postcode", "X11 1XX").param("capacity", "100")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/add_venue"))
				.andExpect(model().attributeHasFieldErrors("venue", "name"))
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeCount(0));

		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void postNewBadVenueLongAddr() throws Exception {
		mvc.perform(post("/venues/newVenue").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test").param("postcode", "X11 1XX").param("capacity", "100")
				.param("address", "Address of the venue which is over 299 characters (300 characters) "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopq")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/add_venue"))
				.andExpect(model().attributeHasFieldErrors("venue", "address"))
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeCount(0));

		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void postNewBadVenueNegativeCapacity() throws Exception {
		mvc.perform(post("/venues/newVenue").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test").param("address", "1 Some Street").param("postcode", "X11 1XX").param("capacity", "-1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/add_venue"))
				.andExpect(model().attributeHasFieldErrors("venue", "capacity"))
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeCount(0));

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void postNewEmptyVenue() throws Exception {
		mvc.perform(post("/venues/newVenue").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk()).andExpect(view().name("venues/add_venue"))
				.andExpect(model().attributeHasFieldErrors("venue", "name")).andExpect(model().attributeHasFieldErrors("venue", "address"))
				.andExpect(model().attributeHasFieldErrors("venue", "postcode")).andExpect(model().attributeHasFieldErrors("venue", "capacity"))
				.andExpect(handler().methodName("createVenue")).andExpect(flash().attributeCount(0));

		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void postUpdateVenue() throws Exception {		
		ArgumentCaptor<Venue> arg = ArgumentCaptor.forClass(Venue.class);
		when(venueService.save(any(Venue.class))).then(returnsFirstArg());

		mvc.perform(post("/venues/updateVenues/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test").param("address", "1 Some Street").param("postcode", "X11 1XX").param("capacity", "100")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound()).andExpect(content().string(""))
				.andExpect(view().name("redirect:/venues")).andExpect(model().hasNoErrors())
				.andExpect(handler().methodName("updateVenues")).andExpect(flash().attributeExists("ok_message"));

		verify(venueService).save(arg.capture());
	}
	
	@Test
	public void postUpdateVenueNoAuth() throws Exception {
		mvc.perform(post("/venues/updateVenues/1").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test").param("address", "1 Some Street").param("postcode", "X11 1XX").param("capacity", "100")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
				.andExpect(header().string("Location", endsWith("/sign-in")));

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void postUpdateVenueBadRole() throws Exception {
		mvc.perform(post("/venues/updateVenues/1").with(user("Rob").roles(BAD_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test").param("address", "1 Some Street").param("postcode", "X11 1XX").param("capacity", "100")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isForbidden());

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void postUpdateVenueNoCsrf() throws Exception {
		mvc.perform(post("/venues/updateVenues/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test").param("address", "1 Some Street").param("postcode", "X11 1XX").param("capacity", "100")
				.accept(MediaType.TEXT_HTML)).andExpect(status().isForbidden());

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void postUpdateBadVenueNoName() throws Exception {
		mvc.perform(post("/venues/updateVenues/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("address", "1 Some Street").param("postcode", "X11 1XX").param("capacity", "100")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/updateVenues"))
				.andExpect(model().attributeHasFieldErrors("venue", "name"))
				.andExpect(handler().methodName("updateVenues")).andExpect(flash().attributeCount(0));

		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void postUpdateBadVenueNoAddr() throws Exception {
		mvc.perform(post("/venues/updateVenues/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test").param("postcode", "X11 1XX").param("capacity", "100")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/updateVenues"))
				.andExpect(model().attributeHasFieldErrors("venue", "address"))
				.andExpect(handler().methodName("updateVenues")).andExpect(flash().attributeCount(0));

		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void postUpdateBadVenueNoPCD() throws Exception {
		mvc.perform(post("/venues/updateVenues/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test").param("address", "1 Some Street").param("capacity", "100")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/updateVenues"))
				.andExpect(model().attributeHasFieldErrors("venue", "postcode"))
				.andExpect(handler().methodName("updateVenues")).andExpect(flash().attributeCount(0));

		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void postUpdateBadVenueNoCapacity() throws Exception {
		mvc.perform(post("/venues/updateVenues/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test").param("address", "1 Some Street").param("postcode", "X11 1XX")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/updateVenues"))
				.andExpect(model().attributeHasFieldErrors("venue", "capacity"))
				.andExpect(handler().methodName("updateVenues")).andExpect(flash().attributeCount(0));

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void postUpdateBadVenueLongName() throws Exception {
		mvc.perform(post("/venues/updateVenues/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test that is over 255 characters (256 characters) abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijk")
				.param("address", "1 Some Street").param("postcode", "X11 1XX").param("capacity", "100")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/updateVenues"))
				.andExpect(model().attributeHasFieldErrors("venue", "name"))
				.andExpect(handler().methodName("updateVenues")).andExpect(flash().attributeCount(0));

		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void postUpdateBadVenueLongAddr() throws Exception {
		mvc.perform(post("/venues/updateVenues/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test").param("postcode", "X11 1XX").param("capacity", "100")
				.param("address", "Address of the venue which is over 299 characters (300 characters) "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz "
						+ "abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz abcdefghijklmnopq")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/updateVenues"))
				.andExpect(model().attributeHasFieldErrors("venue", "address"))
				.andExpect(handler().methodName("updateVenues")).andExpect(flash().attributeCount(0));

		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void postUpdateBadVenueNegativeCapacity() throws Exception {
		mvc.perform(post("/venues/updateVenues/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("name", "Venue Test").param("address", "1 Some Street").param("postcode", "X11 1XX").param("capacity", "-1")
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk())
				.andExpect(view().name("venues/updateVenues"))
				.andExpect(model().attributeHasFieldErrors("venue", "capacity"))
				.andExpect(handler().methodName("updateVenues")).andExpect(flash().attributeCount(0));

		verify(venueService, never()).save(any(Venue.class));
	}

	@Test
	public void postUpdateEmptyVenue() throws Exception {
		mvc.perform(post("/venues/updateVenues/1").with(user("Rob").roles(Security.ADMIN_ROLE)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isOk()).andExpect(view().name("venues/updateVenues"))
				.andExpect(model().attributeHasFieldErrors("venue", "name")).andExpect(model().attributeHasFieldErrors("venue", "address"))
				.andExpect(model().attributeHasFieldErrors("venue", "postcode")).andExpect(model().attributeHasFieldErrors("venue", "capacity"))
				.andExpect(handler().methodName("updateVenues")).andExpect(flash().attributeCount(0));

		verify(venueService, never()).save(any(Venue.class));
	}
	
	@Test
	public void deleteVenue() throws Exception {
	    when(venueService.existsById(1)).thenReturn(true);
	    when(venueService.existsByIdAndEventsIsEmpty(1)).thenReturn(true);
	    
	    mvc.perform(delete("/venues/1").with(user("Adm").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
	            .andExpect(view().name("redirect:/venues")).andExpect(handler().methodName("deleteVenue")).andExpect(flash().attributeExists("ok_message"));
	    
	    verify(venueService).deleteById(1);
	}
	
	@Test
	public void deleteVenueConflict() throws Exception {
	    when(venueService.existsById(1)).thenReturn(true);
	    when(venueService.existsByIdAndEventsIsEmpty(1)).thenReturn(false);
	    
	    mvc.perform(delete("/venues/1").with(user("Adm").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isFound())
	            .andExpect(view().name("redirect:/venues/1")).andExpect(handler().methodName("deleteVenue")).andExpect(flash().attributeExists("error_message"));
	    
	    verify(venueService, never()).deleteById(1);
	}
	
	@Test
	public void deleteVenueNotFound() throws Exception {
	    when(venueService.existsById(1)).thenReturn(false);
	    
	    mvc.perform(delete("/venues/1").with(user("Adm").roles(Security.ADMIN_ROLE)).accept(MediaType.TEXT_HTML).with(csrf())).andExpect(status().isNotFound())
	            .andExpect(view().name("venues/not_found")).andExpect(handler().methodName("deleteVenue"));
	    
	    verify(venueService, never()).deleteById(1);
	}
}
