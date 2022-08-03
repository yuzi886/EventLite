package uk.ac.man.cs.eventlite.controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.man.cs.eventlite.assemblers.EventModelAssembler;
import uk.ac.man.cs.eventlite.assemblers.VenueModelAssembler;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@RestController
@RequestMapping(value = "/api", produces = { MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE })
public class HomepageControllerApi {

	@Autowired
	private EventService eventService;

	@Autowired
	private EventModelAssembler eventAssembler;
	
	@Autowired
	private VenueService venueService;

	@Autowired
	private VenueModelAssembler venueAssembler;

//	@GetMapping("/upcoming_events")
//	public CollectionModel<EntityModel<Event>> getUpcomingEvents() {
//		return eventAssembler.toCollectionModel(eventService.findTop3ByOrderByDateAscTimeAsc(LocalDate.now(), LocalTime.now()))
//				.add(linkTo(methodOn(HomepageControllerApi.class).getUpcomingEvents()).withSelfRel());
//	}
	
	@GetMapping("/popular_venues")
	public CollectionModel<EntityModel<Venue>> getPopularVenues() {
		return venueAssembler.toCollectionModel(venueService.findAllByOrderByNumOfEvents())
				.add(linkTo(methodOn(HomepageControllerApi.class).getPopularVenues()).withSelfRel());
	}
}
