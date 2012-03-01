package fr.xebia.stomp.client;

public class StompException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public StompException() {
		super();
	}

	public StompException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public StompException(String arg0) {
		super(arg0);
	}

	public StompException(Throwable arg0) {
		super(arg0);
	}

}
