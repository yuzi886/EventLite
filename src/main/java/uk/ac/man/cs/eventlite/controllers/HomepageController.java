package uk.ac.man.cs.eventlite.controllers;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;

@Controller
@RequestMapping(value = "/", produces = { MediaType.TEXT_HTML_VALUE })
public class HomepageController {

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;
	
	@GetMapping
	public String getUpcomingEventsAndPopularVenues(Model model) {
		model.addAttribute("events", eventService.findTop3ByOrderByDateAscTimeAsc(LocalDate.now(), LocalTime.now()));
		model.addAttribute("venues", venueService.findAllByOrderByNumOfEvents());
		
		return "homepage/index";
	}
}
