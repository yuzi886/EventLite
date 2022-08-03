package uk.ac.man.cs.eventlite.controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Arrays;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import uk.ac.man.cs.eventlite.assemblers.EventModelAssembler;
import uk.ac.man.cs.eventlite.assemblers.VenueModelAssembler;
import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;
import uk.ac.man.cs.eventlite.exceptions.VenueNotFoundException;
import uk.ac.man.cs.eventlite.exceptions.ConflictException;

@RestController
@RequestMapping(value = "/api/venues", produces = { MediaType.APPLICATION_JSON_VALUE, MediaTypes.HAL_JSON_VALUE })
public class VenuesControllerApi {
	
	private static final String NOT_FOUND_MSG = "{ \"error\": \"%s\", \"id\": %d }";
	
	private static final String CONFLICT_MSG = "{ \"error\": \"%s\", \"id\": %d }";
	
	@Autowired
	private VenueService venueService;
	
	@Autowired
	private VenueModelAssembler venueAssembler;
	
	@Autowired 
	private EventModelAssembler EventAssembler;
	
	@Autowired
	private EventService eventService;
	
	@ExceptionHandler(VenueNotFoundException.class)
	public ResponseEntity<?> VenueNotFoundHandler(VenueNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(String.format(NOT_FOUND_MSG, ex.getMessage(), ex.getId()));
	}
	
	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<?> ConflictHandler(ConflictException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(String.format(CONFLICT_MSG, ex.getMessage(), ex.getId()));
	}
	
	@GetMapping
	public CollectionModel<EntityModel<Venue>> getAllVenue() {
		return venueAssembler.toCollectionModel(venueService.findAllByOrderByalphabet())
				.add(linkTo(methodOn(VenuesControllerApi.class).getAllVenue()).withSelfRel())
				.add(linkTo(HomepageControllerApi.class).slash("/profile/venues").withRel("profile")); 
	}
	
	@GetMapping("/{id}")
	public EntityModel<Venue> getVenue(@PathVariable("id") long id) {
		if (!venueService.existsById(id))
		{
			throw new VenueNotFoundException(id);
		}else{
			Venue venue = venueService.findByIdV(id);
			EntityModel<Venue> model = venueAssembler.toModel(venue);
			model.add(linkTo(methodOn(VenuesControllerApi.class).getVenue(id)).withRel("venue"));
			model.add(linkTo(methodOn(VenuesControllerApi.class).getVenueEvents(id)).withRel("events"));
			model.add(linkTo(methodOn(VenuesControllerApi.class).getnext3events(id)).withRel("next3events"));
			
			return model;
		}
	}
	
	@GetMapping("/{id}/events")
	public CollectionModel<EntityModel<Event>> getVenueEvents(@PathVariable("id") long id) 
	{
		if (!venueService.existsById(id))
		{
			throw new VenueNotFoundException(id);
		}else{
			
			Venue venue = venueService.findByIdV(id);
			Iterable<Event> allEvents = eventService.findAllByOrderByDateAndalphabet();
			
			int n = 0;
			for (Event event: allEvents) 
		    {
		        if(event.getVenue().equals(venue))
		        {
		        	n+=1;
		        }
		    }
	
			Event[] venueEvents = new Event[n];
			
			int i = 0;
			
		    for (Event event: allEvents) 
		    {
		        if(event.getVenue().equals(venue))
		        {
		        	venueEvents[i] = event;
		        	i += 1;
		        }
		    }
		    
		    Iterable<Event> iterableEvents = Arrays.asList(venueEvents);

			return EventAssembler.toCollectionModel(iterableEvents)
					.add(linkTo(methodOn(VenuesControllerApi.class).getVenueEvents(id)).withSelfRel());
		}
	}
	
	@GetMapping("/{id}/next3events")
	public CollectionModel<EntityModel<Event>> getnext3events(@PathVariable("id") long id) {
		if (!venueService.existsById(id)) {
			throw new VenueNotFoundException(id);
		}

		return EventAssembler.toCollectionModel(eventService.getnext3events(id))
				.add(linkTo(methodOn(VenuesControllerApi.class).getnext3events(id)).withSelfRel());
	}
	
	@GetMapping("/add_venue")
	public ResponseEntity<?> newVenue() {
		return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
	}
	
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> createVenue(@RequestBody @Valid Venue venue, BindingResult result) {

		if (result.hasErrors()) {
			return ResponseEntity.unprocessableEntity().build();
		}

		Venue newVenue = venueService.save(venue);
		EntityModel<Venue> entity = venueAssembler.toModel(newVenue);

		return ResponseEntity.created(entity.getRequiredLink(IanaLinkRelations.SELF).toUri()).build();
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteVenue(@PathVariable("id") long id) {
	    if (!venueService.existsById(id)) {
	    	throw new VenueNotFoundException(id);
	    }
	    
	    if (!venueService.existsByIdAndEventsIsEmpty(id)) {
	    	throw new ConflictException(id);
	    }
	    
	    venueService.deleteById(id);
	    
	    return ResponseEntity.noContent().build();
	}
}
