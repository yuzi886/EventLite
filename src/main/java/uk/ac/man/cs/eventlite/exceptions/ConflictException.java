package uk.ac.man.cs.eventlite.exceptions;

public class ConflictException extends RuntimeException {

	private static final long serialVersionUID = 5016812401135889608L;

	private long id;

	public ConflictException(long id) {
		super("Could not delete venue " + id + " due to linked events");

		this.id = id;
	}

	public long getId() {
		return id;
	}
}
