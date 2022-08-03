package uk.ac.man.cs.eventlite.dao;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Iterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import uk.ac.man.cs.eventlite.EventLite;
import uk.ac.man.cs.eventlite.controllers.EventsController;
import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EventLite.class)
@DirtiesContext
@ActiveProfiles("test")
//@Disabled
public class EventServiceTest extends AbstractTransactionalJUnit4SpringContextTests {

	@Autowired
	private EventService eventService;

	// This class is here as a starter for testing any custom methods within the
	// EventService. Note: It is currently @Disabled!
	
	@Test
	public void testToDateOrder() throws Exception {
		Iterator<Event> event = eventService.findAllByOrderByDateAscTimeAsc().iterator();
		Event e1 = event.next();
		boolean flag = false;
		while(event.hasNext()) {
			flag = false;
			Event e = event.next();
			if((e1.getDate().isBefore(e.getDate())) ||(e1.getDate().isEqual(e.getDate())) ) {
				flag = true;
			}
			assertThat(flag, equalTo(true));
			e1 = e;
		}
		
	}
	
	@Test
	public void testToDateAndAlphabetOrder() throws Exception {
		Iterator<Event> event = eventService.findAllByOrderByDateAndalphabet().iterator();
		String[] list = new String[6];
		int i =0;
		while(event.hasNext()) {
			Event e = event.next();
			list[i] = e.getName();
			i++;
		}
		String[] right = {"Event Past", "Event Former", "Event Previous", "Event Alpha", "Event Beta", "Event Apple"};
		assertThat(list, equalTo(right));
		
	}
	
	@Test
	public void testToFutureEvent() throws Exception {
		Iterator<Event> event = eventService.findUpcomingByOrderByDateAscTimeAsc().iterator();
		String[] list = new String[3];
		int i =0;
		while(event.hasNext()) {
			Event e = event.next();
			list[i] = e.getName();
			i++;
		}
		String[] right = {"Event Alpha", "Event Beta", "Event Apple"};
		assertThat(list, equalTo(right));
		
	}
	
	@Test
	public void testToPreEvent() throws Exception {
		Iterator<Event> event = eventService.findPrecomingByOrderByDateAscTimeAsc().iterator();
		String[] list = new String[3];
		int i =0;
		while(event.hasNext()) {
			Event e = event.next();
			list[i] = e.getName();
			i++;
		}
		String[] right = { "Event Former", "Event Previous","Event Past"};
		assertThat(list, equalTo(right));
		
	}
}
