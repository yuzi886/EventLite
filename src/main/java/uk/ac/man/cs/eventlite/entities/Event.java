package uk.ac.man.cs.eventlite.entities;

import java.time.LocalDate;
import java.time.LocalTime;

import javax.persistence.*;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Future;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "events")
public class Event {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@NotNull(message = "The date cannot be empty.")
	@Future(message = "The date has to be in the future.")
	private LocalDate date;

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime time;
	
	@NotNull(message = "The name cannot be empty.")
	@Size(max = 255, message = "The name must have less than 256 characters.")
	private String name;
	
	@ManyToOne
	//@JoinColumn(name = "venue_id")
	@NotNull(message = "The venue cannot be empty.")
	private Venue venue;
	
	@Size(max = 499, message = "The description must have less than 500 characters.")
	private String description;

	public Event() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public LocalTime getTime() {
		return time;
	}

	public void setTime(LocalTime time) {
		this.time = time;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Venue getVenue() {
		return venue;
	}

	public void setVenue(Venue venue) {
		this.venue = venue;
	}
	
	public void setDescription(String d) {
		this.description = d;
	}

	public String getDescription() {
		return description;
	}
	
}