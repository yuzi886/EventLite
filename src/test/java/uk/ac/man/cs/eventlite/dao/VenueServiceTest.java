package uk.ac.man.cs.eventlite.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Iterator;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Iterator;

import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import uk.ac.man.cs.eventlite.EventLite;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventLite.class)
@DirtiesContext
@ActiveProfiles("test")
//@Disabled
public class VenueServiceTest extends AbstractTransactionalJUnit4SpringContextTests {

	@Autowired
	private VenueService venueService;

	// This class is here as a starter for testing any custom methods within the
	// VenueService. Note: It is currently @Disabled!
	
	@Test
	public void testVenueSearch() throws Exception {
		Iterator<Venue> venue = venueService.getByKeyword("venue").iterator();
		String[] v_list = new String[3];
		int i = 0;
		while(venue.hasNext()) {
			Venue v  = venue.next();
			v_list[i] = v.getName();
			i++;
		}
		String[] r = {"Venue A", "Venue B", "Venue C"};
		assertThat(v_list, equalTo(r));
		
	}
	
	@Test
	public void testVenueSearchA() throws Exception{
		Iterator<Venue> venue = venueService.getByKeyword("a").iterator();
		String[] v_list = new String[1];
		int i = 0;
		while(venue.hasNext()) {
			Venue v  = venue.next();
			v_list[i] = v.getName();
			i++;
		}
		String[] r = {"Venue A"};
		assertThat(v_list, equalTo(r));
	}
	
	@Test
	public void testToLocation() throws Exception {
		Iterator<Venue> venue = venueService.findAllByOrderByalphabet().iterator();
		Venue v = venue.next();
		assertNotEquals(v.getLatitude(),(0.0));
		assertNotEquals(v.getLongitude(),(0.0));
	}

}
