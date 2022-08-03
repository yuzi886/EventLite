package uk.ac.man.cs.eventlite.dao;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.springframework.data.domain.Sort;

import uk.ac.man.cs.eventlite.entities.Event;

public interface EventService {

	public long count();
	public Event save(Event event);
	public Iterable<Event> findAllByOrderByDateAndalphabet();
	public Iterable<Event> getByKeyword(String keyword);
	public Iterable<Event> getByKeyword(String keyword, String time);
	public Iterable<Event> findAllByOrderByDateAscTimeAsc();
	public Iterable<Event> findTop3ByOrderByDateAscTimeAsc(LocalDate date, LocalTime time);
	public Iterable<Event> findUpcomingByOrderByDateAscTimeAsc();
	public Iterable<Event> findPrecomingByOrderByDateAscTimeAsc();
	public Iterable<Event> findAllByOrderByDDateAndalphabet();
	public void deleteById(long id);
	public boolean existsById(long id);
	public Optional<Event> findById(long id);
	public Event findSingle(Long id);
	public Event findByIdE(long id);
	public Iterable<Event> getnext3events(long id);
	public Iterable<Event> findAllByVenue(long id);
}
