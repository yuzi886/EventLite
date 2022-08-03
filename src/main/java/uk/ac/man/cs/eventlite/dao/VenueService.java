package uk.ac.man.cs.eventlite.dao;

import java.util.Optional;

import uk.ac.man.cs.eventlite.entities.Event;
import uk.ac.man.cs.eventlite.entities.Venue;

public interface VenueService {

	public long count();

	public Iterable<Venue> findAll();
	public Venue save(Venue venue);
	public Iterable<Venue> findAllByOrderByalphabet();
	public Optional<Venue> findById(long id);
	public Venue findByIdV(long id);
	public Iterable<Venue> findAllByOrderByNumOfEvents();
	public Iterable<Venue> getByKeyword(String keyword);
	public void deleteById(long id);
	public boolean existsById(long id);
	public boolean existsByIdAndEventsIsEmpty(long id);
}
