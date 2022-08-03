package uk.ac.man.cs.eventlite.dao;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import uk.ac.man.cs.eventlite.entities.Event;

public interface EventRepository extends CrudRepository<Event, Long> {

	public Iterable<Event> findAllByOrderByDateAscTimeAsc();
	@Query(value = "select * from events e where e.name like %:keyword% or lower(e.name) like %:keyword% or upper(e.name) like %:keyword%", nativeQuery = true)
	Iterable<Event> findByKeyword(@Param("keyword") String keyword);
	@Query("SELECT e FROM Event e ORDER BY e.date ASC, e.name ASC")
	Iterable<Event> findAllByOrderByDateAndalphabet();
	@Query("SELECT e FROM Event e ORDER BY e.date DESC, e.name ASC")
	Iterable<Event> findAllByOrderByDDateAndalphabet();
	@Query(value = "SELECT * FROM events e WHERE e.date > :date OR (e.date = :date AND e.time > :time) ORDER BY e.date ASC, e.time ASC LIMIT 0, 3", nativeQuery = true)
	public Iterable<Event> findTop3ByOrderByDateAscTimeAsc(@Param("date") LocalDate date, @Param("time") LocalTime time);
	public Iterable<Event> findUpcomingByOrderByDateAscTimeAsc();
	public Iterable<Event> findPrecomingByOrderByDateAscTimeAsc();
}
