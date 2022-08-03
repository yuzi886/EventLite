package uk.ac.man.cs.eventlite.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

public interface VenueRepository extends CrudRepository<Venue, Long> {
	@Query("SELECT v FROM Venue v ORDER BY v.name ASC")
	Iterable<Venue> findAllByOrderByalphabet();
	
	@Query(value = "SELECT v FROM Venue v LEFT JOIN v.events ve GROUP BY v ORDER BY COUNT(ve.id) DESC", countQuery = "select count(v.id) from Venue v")
	public Iterable<Venue> findAllByOrderByNumOfEvents();
	
	@Query(value = "select * from venues v where v.name like %:keyword% or lower(v.name) like %:keyword% or upper(v.name) like %:keyword% order by v.name asc", nativeQuery = true)
	Iterable<Venue> findByKeyword(@Param("keyword") String keyword);
	
	public boolean existsByIdAndEventsIsEmpty(@Param("id") long id);
}