package uk.ac.man.cs.eventlite.entities;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Point;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.ac.man.cs.eventlite.controllers.EventsController;

import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
@Table(name = "venues")
public class Venue {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@NotNull(message = "The name cannot be empty.")
	@Size(max = 255, message = "The name must have less than 256 characters.")
	private String name;
	
	@NotNull(message = "The addrees cannot be empty.")
	@Size(max = 299, message = "The name must have less than 300 characters.")
	private String address;
	
	@NotNull(message = "The postcode cannot be empty.")
	private String postcode;
	
	@Min(value = 1, message = "The capacity must be positive and not 0")
	private int capacity;
	
	@OneToMany(mappedBy = "venue")
	private Set<Event> events;
	
	private double longitude;
	
	private double latitude;

	public Venue() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}
	
	public String getAddress() {
		return address;
	}
	
	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}
	
	public String getPostcode() {
		return postcode;
	}
	
	public Set<Event> getEvents() {
	    return events;
	}
	
	public void setLatitude(double latitude)
	{
		this.latitude = latitude;
	}
	
	public void setLongitude(double longitude)
	{
		this.longitude = longitude;
	}
	
	public double getLatitude()
	{
		return this.latitude;
	}
	
	public double getLongitude()
	{
		return this.longitude;
	}
}
