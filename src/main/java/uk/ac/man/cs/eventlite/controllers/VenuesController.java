package uk.ac.man.cs.eventlite.controllers;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;
import uk.ac.man.cs.eventlite.exceptions.VenueNotFoundException;

@Controller
@RequestMapping(value = "/venues", produces = { MediaType.TEXT_HTML_VALUE })
public class VenuesController {
	
	@Autowired
	private VenueService venueService;
	
	@Autowired
	private EventService eventService;

	@ExceptionHandler(VenueNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String VenueNotFoundHandler(VenueNotFoundException ex, Model model) {
		model.addAttribute("not_found_id", ex.getId());

		return "venues/not_found";
	}
	
	@GetMapping("/{id}")
	public String getVenue(@PathVariable("id") long id, Model model) {
		Venue venue = venueService.findById(id).orElseThrow(() -> new VenueNotFoundException(id));

		model.addAttribute("venue", venue);
		model.addAttribute("events",eventService.findAllByVenue(id));

		return "venues/show";
	}
	
	@GetMapping
	public String getAllVenue(Model model, String keyword) {
		if (keyword != null) {
			model.addAttribute("venues",venueService.getByKeyword(keyword));
		}
		else {
			model.addAttribute("venues", venueService.findAllByOrderByalphabet());
		}

		return "venues/index";
	}
	
	@GetMapping("/add_venue")
	public String newVenue(Model model) {
		
		if (!model.containsAttribute("venue")) {
			model.addAttribute("venue", new Venue());
			//model.addAttribute("venues", venueService.findAll());
		}
		
		//model.addAttribute("venues", venueService.findAll());

		return "venues/add_venue";
	}
	
	@PostMapping(value = "/newVenue",  consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String createVenue(@RequestBody @Valid @ModelAttribute Venue venue, BindingResult errors,
			Model model, RedirectAttributes redirectAttrs) {

		if (errors.hasErrors()) {
			//model.addAttribute("venues", venueService.findAll());
			model.addAttribute("venue", venue);
			return "venues/add_venue";
		}

		venueService.save(venue);
		redirectAttrs.addFlashAttribute("ok_message", "New venue added.");

		return "redirect:/venues";
	}
	
	@DeleteMapping("/{id}")
	public String deleteVenue(@PathVariable("id") long id, RedirectAttributes redirectAttrs) {
		if (!venueService.existsById(id)) {
			throw new VenueNotFoundException(id);
		}
		
		if (!venueService.existsByIdAndEventsIsEmpty(id)) {
			redirectAttrs.addFlashAttribute("error_message", "Venue cannot be deleted, the venue is linked to one or more events.");
			return "redirect:/venues/" + id;
		}
		
		venueService.deleteById(id);
		redirectAttrs.addFlashAttribute("ok_message", "Venue deleted.");
		
		return "redirect:/venues";
	}
	
	
	@GetMapping("/updateVenues/{id}")
	public String updateLockId(Model model, @PathVariable("id") long id) {
		
		Venue venue = venueService.findById(id).orElseThrow(() -> new VenueNotFoundException(id));
		
		if (!model.containsAttribute("venue")) {
			model.addAttribute("venue", venue);
		}

		return "venues/updateVenues";
		
	}

	@PostMapping(value="/updateVenues/{id}", consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String updateVenues(@PathVariable("id") long id, @RequestBody @Valid @ModelAttribute Venue venue, BindingResult result, Model model, RedirectAttributes redirectAttrs) {
		
		if (result.hasErrors()) {
			model.addAttribute("venue", venue);
			return "venues/updateVenues";
		}
		
		venueService.save(venue);
		redirectAttrs.addFlashAttribute("ok_message", "Venue updated.");
		
		return "redirect:/venues";
		
		
	}

}
