package uk.ac.man.cs.eventlite.dao;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import uk.ac.man.cs.eventlite.controllers.EventsController;
import uk.ac.man.cs.eventlite.entities.Event;


@Service
public class EventServiceImpl implements EventService {
	
	@Autowired
	private EventRepository eventRepository;

	@Override
	public long count() {
		return eventRepository.count();
	}

	@Override
	public Iterable<Event> findAllByOrderByDateAscTimeAsc() {
		return eventRepository.findAllByOrderByDateAscTimeAsc();
	}
	
	@Override
	public Event save(Event event) {
		return eventRepository.save(event);
	}
	
	@Override
	public Optional<Event> findById(long id) {
		return eventRepository.findById(id);
	}
	
	@Override
	public Iterable<Event> findAllByOrderByDateAndalphabet(){	
		return eventRepository.findAllByOrderByDateAndalphabet();
	}
	

	@Override
	public Iterable<Event> getByKeyword(String keyword){
		  return eventRepository.findByKeyword(keyword);
	}
	
	@Override
	public Iterable<Event> getByKeyword(String keyword, String time){
		  Iterator<Event> search = eventRepository.findByKeyword(keyword).iterator();
		  LocalTime currentTime = LocalTime.now();
		  LocalDate currentDate = LocalDate.now();
		  ArrayList<Event> result = new ArrayList<Event>();
		  if (time == "u") {
			  while(search.hasNext()) {
					Event en = search.next();
					if(en.getDate().isAfter(currentDate)) {
						result.add(en);
					}else if(en.getDate().isEqual(currentDate)) {
						if(en.getTime().isAfter(currentTime)) {
							result.add(en);
						}
					}
				}
			    return result;
		  }
		  else if (time == "p") {
			  while(search.hasNext()) {
					Event en = search.next();
					if(en.getDate().isBefore(currentDate)) {
						result.add(en);
					}else if(en.getDate().isBefore(currentDate)) {
						if(en.getTime().isAfter(currentTime)) {
							result.add(en);
						}
					}
				}
			  return result;
		  }
		  else
			  return null;
	} 
	
	@Override
	public Iterable<Event> findTop3ByOrderByDateAscTimeAsc(LocalDate date, LocalTime time) {
		return eventRepository.findTop3ByOrderByDateAscTimeAsc(date, time);
	}
	
	@Override
	public void deleteById(long id) {
		eventRepository.deleteById(id);
	}
	
	@Override
	public Event findSingle(Long id) {
		return findById(id).orElse(null);
	}

	@Override
	public boolean existsById(long id) { 
		return eventRepository.existsById(id);
	}
	
	@Override
	public Iterable<Event> findUpcomingByOrderByDateAscTimeAsc(){
		Iterator<Event> e = eventRepository.findAllByOrderByDateAndalphabet().iterator();
		LocalTime currentTime = LocalTime.now();
		LocalDate currentDate = LocalDate.now();
		ArrayList<Event> output = new ArrayList<Event>();
		final Logger log = LoggerFactory.getLogger(EventsController.class);
		while(e.hasNext()) {
			Event en = e.next();
			if(en.getDate().isAfter(currentDate)) {
				output.add(en);
			}else if(en.getDate().isEqual(currentDate)) {
				if(en.getTime().isAfter(currentTime)) {
					output.add(en);
				}
			}
		}
		//Iterator<Event> out = output.iterator();
		return output;
		
	}
	@Override 
	public Iterable<Event> getnext3events(long id) {
		Iterator<Event> e = eventRepository.findAllByOrderByDateAndalphabet().iterator();
		LocalTime currentTime = LocalTime.now();
		LocalDate currentDate = LocalDate.now();
		ArrayList<Event> output = new ArrayList<Event>();
		while(e.hasNext() && output.size() < 3) {
			Event en = e.next();
			if (en.getVenue().getId() == id) {
				if(en.getDate().isAfter(currentDate)) {
					output.add(en);
				}else if(en.getDate().isEqual(currentDate)) {
					if(en.getTime().isAfter(currentTime)) {
						output.add(en);
					}
				}
			}
		}
		//Iterator<Event> out = output.iterator();
		return output;
	}
	
	@Override
	public Iterable<Event> findPrecomingByOrderByDateAscTimeAsc() {
		Iterator<Event> e = eventRepository.findAllByOrderByDDateAndalphabet().iterator();
		LocalTime currentTime = LocalTime.now();
		LocalDate currentDate = LocalDate.now();
		ArrayList<Event> output = new ArrayList<Event>();
		while(e.hasNext()) {
			Event en = e.next();
			if(en.getDate().isBefore(currentDate)) {
				output.add(en);
			}else if(en.getDate().isEqual(currentDate)) {
				if(en.getTime().isBefore(currentTime)) {
					output.add(en);
				}
			}
		}
		//Iterator<Event> out = output.iterator();
		return output;
	}
	
	@Override
	public Iterable<Event> findAllByOrderByDDateAndalphabet(){
		return eventRepository.findAllByOrderByDDateAndalphabet();
	}
	
	@Override 
	public Iterable<Event> findAllByVenue(long id) {
		Iterator<Event> e = eventRepository.findAllByOrderByDateAndalphabet().iterator();
		LocalTime currentTime = LocalTime.now();
		LocalDate currentDate = LocalDate.now();
		ArrayList<Event> output = new ArrayList<Event>();
		while(e.hasNext()) {
			Event en = e.next();
			if (en.getVenue().getId() == id) {
				if(en.getDate().isAfter(currentDate)) {
					output.add(en);
				}else if(en.getDate().isEqual(currentDate)) {
					if(en.getTime().isAfter(currentTime)) {
						output.add(en);
					}
				}
			}
		}
		
		return output;
	}
	
	@Override
	public Event findByIdE(long id) {
		return eventRepository.findById(id).get();
	}
}
