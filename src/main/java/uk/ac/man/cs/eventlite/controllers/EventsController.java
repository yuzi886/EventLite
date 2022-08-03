package uk.ac.man.cs.eventlite.controllers;

import uk.ac.man.cs.eventlite.entities.Event;
import javax.validation.Valid;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponents;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import javax.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import twitter4j.DirectMessage;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.exceptions.EventNotFoundException;
import uk.ac.man.cs.eventlite.entities.Venue;
import org.springframework.web.bind.annotation.PostMapping;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

@Controller
@RequestMapping(value = "/events", produces = { MediaType.TEXT_HTML_VALUE })
public class EventsController {

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;

	@ExceptionHandler(EventNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public String eventNotFoundHandler(EventNotFoundException ex, Model model) {
		model.addAttribute("not_found_id", ex.getId());

		return "events/not_found";
	}

	@GetMapping("/{id}")
	public String getEvent(@PathVariable long id, Model model) {
		Event event = eventService.findById(id).orElseThrow(() -> new EventNotFoundException(id));

		model.addAttribute("event", event);

		return "events/show";
	}
	
	@GetMapping
	public String getAllEvents(Model model, String keyword, RedirectAttributes redirectAttrs) throws TwitterException {
		if (keyword != null) {
			model.addAttribute("events",eventService.getByKeyword(keyword,"u"));
			model.addAttribute("preEvents",eventService.getByKeyword(keyword,"p"));
		}
		else {
			model.addAttribute("events", eventService.findUpcomingByOrderByDateAscTimeAsc());// change
			model.addAttribute("preEvents",eventService.findPrecomingByOrderByDateAscTimeAsc());
		}
		
		try {
			Twitter twitter = getTwitterInstance();
			List<Status> statuses = twitter.getHomeTimeline();
			
			int size = ((statuses.size() < 5) ? statuses.size() : 5);
			model.addAttribute("timeline", statuses.subList(0, size));
		} catch (TwitterException e) {
			redirectAttrs.addFlashAttribute("error_message", "Could not retrieve the Twitter timeline.");
		}

		return "events/index";
	}
	
	
	@GetMapping("/add_event")
	public String newEvent(Model model) {
		
		if (!model.containsAttribute("event")) {
			model.addAttribute("event", new Event());
		}
		
		model.addAttribute("venues", venueService.findAll());

		return "events/add_event";
	}
	
	@PostMapping(value ="/{id}",  consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String postTwitter(@PathVariable("id") long id,String tweet, @RequestBody @Valid @ModelAttribute Event event, BindingResult errors, Model model, RedirectAttributes redirectAttrs) throws TwitterException {
		try{
			if (tweet != null) {
				Twitter twitter = getTwitterInstance();
				StatusUpdate statusUpdate = new StatusUpdate(tweet);
				//Status status = twitter.updateStatus("creating baeldung API");
				Status status = twitter.updateStatus(statusUpdate);
				final Logger log = LoggerFactory.getLogger(EventsController.class);
				log.info("post:"+status.getText());
				if (status.getText() != null) {
					redirectAttrs.addFlashAttribute("ok_message", "Your tweet: '" + tweet + "' was posted.");
				}
			}
		}catch(TwitterException e) {
			redirectAttrs.addFlashAttribute("error_message", "Your tweet was not successfully posted.");
		}
		String address = String.format("redirect:/events/%d", id);
		return address;
		
	}
	
	@PostMapping(value = "/newEvent",  consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String createEvent(@RequestBody @Valid @ModelAttribute Event event, BindingResult errors,
			Model model, RedirectAttributes redirectAttrs) {

		if (errors.hasErrors()) {
			model.addAttribute("venues", venueService.findAll());
			model.addAttribute("event", event);
			return "events/add_event";
		}
		
		//model.addAttribute("venues", venueService.findAll());

		eventService.save(event);
		redirectAttrs.addFlashAttribute("ok_message", "New event added.");

		return "redirect:/events";
	}
	
	@GetMapping("/updateEvents/{id}")
	public String updateLockId(Model model, @PathVariable("id") long id) {
		Event event = eventService.findById(id).orElseThrow(() -> new EventNotFoundException(id));
		
		if (!model.containsAttribute("event")) {
			model.addAttribute("event", event);
		}

		model.addAttribute("venues", venueService.findAll());
		
		return "events/updateEvents";
		
	}

	@PostMapping(value="/updateEvents/{id}", consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String updateEvents(@PathVariable("id") long id, @RequestBody @Valid @ModelAttribute Event event, BindingResult result, Model model, RedirectAttributes redirectAttrs) {
		
		if (result.hasErrors()) {
			model.addAttribute("event", event);
			model.addAttribute("venues", venueService.findAll());
			return "events/updateEvents";
		}

		eventService.save(event);
		redirectAttrs.addFlashAttribute("ok_message", "Event updated.");
		
		return "redirect:/events";
	}
	
	@DeleteMapping("/{id}")
	public String deleteEvent(@PathVariable("id") long id, RedirectAttributes redirectAttrs) {
		if (!eventService.existsById(id)) {
			throw new EventNotFoundException(id);
		}
		eventService.deleteById(id);
		redirectAttrs.addFlashAttribute("ok_message", "Event deleted.");
		
		return "redirect:/events";
	}
	
	public static Twitter getTwitterInstance() {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		  .setOAuthConsumerKey("E67y6EZdoqvuphj0Tp0uJLK0P")
		  .setOAuthConsumerSecret("K7NeAcE6FZU1k2anxnyd0P1IMPSlKGALrFwc2FBNYScVCTYPxm")
		  .setOAuthAccessToken("1524046871328608256-ukQsj29G0PxXhbMb22wAHLLk5bZWCy")
		  .setOAuthAccessTokenSecret("ESrdmPkAQm916Y5UleCVUCxMCqoLXfZ5P8D3Bel5cWiYa");
		TwitterFactory tf = new TwitterFactory(cb.build());
		Twitter twitter = tf.getInstance();
		
		return twitter;
	}
}
