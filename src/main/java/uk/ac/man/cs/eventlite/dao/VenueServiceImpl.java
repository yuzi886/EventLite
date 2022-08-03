package uk.ac.man.cs.eventlite.dao;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.ac.man.cs.eventlite.controllers.EventsController;
import uk.ac.man.cs.eventlite.entities.Venue;
@Service
public class VenueServiceImpl implements VenueService {
	@Autowired
	private VenueRepository venueRepository;

	@Override
	public long count() {
		return venueRepository.count();
	}

	@Override
	public Iterable<Venue> findAll() {
		return venueRepository.findAll();
	}
	@Override
	public Venue save(Venue venue) {
		final Logger logg = LoggerFactory.getLogger(EventsController.class);
		if(venue.getAddress() != null) {
			final Logger log = LoggerFactory.getLogger(EventsController.class);
			String MAPBOX_ACCESS_TOKEN = "pk.eyJ1Ijoia2VsbHltYW5jaGVzdGVyIiwiYSI6ImNsMTZhbXVobTA1aWQzZHBraGE2ZHl0c2sifQ.nagdXpdlycqcUQHam-7l7w";
			MapboxGeocoding mapboxGeocoding = MapboxGeocoding.builder().accessToken(MAPBOX_ACCESS_TOKEN).query(venue.getPostcode()).build();
			mapboxGeocoding.enqueueCall(new Callback<GeocodingResponse>() {
				@Override
				public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
					List<CarmenFeature> results = response.body().features();
			
					if (results.size() > 0) {
					  Point firstResultPoint = results.get(0).center();
					  venue.setLongitude(firstResultPoint.longitude());
					  venue.setLatitude(firstResultPoint.latitude());
					  log.info( "onResponse: found");
					  log.info("the longitude:"+venue.getLongitude());
					  log.info("the latitude:"+venue.getLatitude());
					} else {
					  // No result for your request were found.
					  log.info( "onResponse: No result found");
					}
				}
			
				@Override
				public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
					throwable.printStackTrace();
				}
			});
		}
		try {
			Thread.sleep(1000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		logg.info( "return");
		return venueRepository.save(venue);

	} 
	
	@Override
	public Iterable<Venue> findAllByOrderByalphabet(){
		return venueRepository.findAllByOrderByalphabet();
	}
	
	@Override
	public Optional<Venue> findById(long id) {
		return venueRepository.findById(id);
	}
	
	@Override
	public Venue findByIdV(long id) {
		return venueRepository.findById(id).get();
	}
	
	@Override
	public Iterable<Venue> findAllByOrderByNumOfEvents() {
		return venueRepository.findAllByOrderByNumOfEvents();
	}
	
	@Override
	public Iterable<Venue> getByKeyword(String keyword) {
		return venueRepository.findByKeyword(keyword);
	}
	
	@Override
	public void deleteById(long id) {
	    venueRepository.deleteById(id);
	}
	
	@Override
	public boolean existsById(long id) { 
		return venueRepository.existsById(id);
	}
	
	@Override
	public boolean existsByIdAndEventsIsEmpty(long id) { 
		return venueRepository.existsByIdAndEventsIsEmpty(id);
	}
}
