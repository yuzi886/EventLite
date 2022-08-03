package uk.ac.man.cs.eventlite.config.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import uk.ac.man.cs.eventlite.dao.EventService;
import uk.ac.man.cs.eventlite.dao.VenueService;
import uk.ac.man.cs.eventlite.entities.Venue;
import uk.ac.man.cs.eventlite.entities.Event;

import java.time.LocalDate;
import java.time.LocalTime;

@Configuration
@Profile("default")
public class InitialDataLoader {

	private final static Logger log = LoggerFactory.getLogger(InitialDataLoader.class);

	@Autowired
	private EventService eventService;

	@Autowired
	private VenueService venueService;

	@Bean
	CommandLineRunner initDatabase() {
		return args -> {
			
			Venue v = new Venue();
			Venue v1 = new Venue();
			Venue v2 = new Venue();
			
			if (venueService.count() > 0) {
				log.info("Database already populated with venues. Skipping venue initialization.");
			} else {
				// Build and save initial venues here
				v.setId(1);
				v.setName("Venue A");
				v.setAddress("23 Manchester Road");
				v.setPostcode("E14 3BD");
				v.setCapacity(50);
				venueService.save(v);
				
				v1.setId(2);
				v1.setName("Venue B");
				v1.setAddress("Highland Road");
				v1.setPostcode("S43 2EZ");
				v1.setCapacity(1000);
				venueService.save(v1);
				
				v2.setId(3);
				v2.setName("Venue C");
				v2.setAddress("19 Acacia Avenue");
				v2.setPostcode("WA15 8QY");
				v2.setCapacity(10);
				venueService.save(v2);
			}

			if (eventService.count() > 0) {
				log.info("Database already populated with events. Skipping event initialization.");
			} else {
				// Build and save initial events here.
				Event e = new Event();
				e.setId(1);
				e.setName("Event Alpha");
				e.setDate(LocalDate.parse("2022-07-11"));
				e.setTime(LocalTime.parse("12:30"));
				e.setDescription("Event Alpha is the first of its kind…");
				e.setVenue(v1);
				eventService.save(e);
				
				Event e1 = new Event();
				e1.setId(2);
				e1.setName("Event Beta");
				e1.setDate(LocalDate.parse("2022-07-11"));
				e1.setTime(LocalTime.parse("10:00"));
				e1.setVenue(v);
				eventService.save(e1);
				
				Event e2 = new Event();
				e2.setId(3);
				e2.setName("Event Apple");
				e2.setDate(LocalDate.parse("2022-07-12"));
				e2.setDescription("Event Apple will be host to some of the world’s best iOS developers…");
				e2.setVenue(v);
				eventService.save(e2);
				
				Event e3 = new Event();
				e3.setId(4);
				e3.setName("Event Former");
				e3.setDate(LocalDate.parse("2022-01-11"));
				e3.setTime(LocalTime.parse("11:00"));
				e3.setVenue(v1);
				eventService.save(e3);
				
				Event e4 = new Event();
				e4.setId(5);
				e4.setName("Event Previous");
				e4.setDate(LocalDate.parse("2022-01-11"));
				e4.setTime(LocalTime.parse("18:30"));
				e4.setVenue(v);
				eventService.save(e4);
				
				Event e5 = new Event();
				e5.setId(6);
				e5.setName("Event Past");
				e5.setDate(LocalDate.parse("2022-01-10"));
				e5.setTime(LocalTime.parse("17:00"));
				e5.setVenue(v);
				eventService.save(e5);
			}
		};
	}
}