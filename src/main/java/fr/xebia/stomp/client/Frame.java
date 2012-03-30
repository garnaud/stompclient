package fr.xebia.stomp.client;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

public class Frame {

	// Frame components
	public final Command command;
	// FIXME use immutable hashmap here
	public final Map<String, String> header;
	public final String message;

	// Constants
	private final static Charset UTF_8 = Charset.forName("UTF-8");
	private final static String ENDLINE = "\n";
	private final static String NULL = "\00";
	protected final static String HEADER_SEPARATOR = ":";
	protected final static byte HEADER_SEPARATOR_BYTE = ":".getBytes(UTF_8)[0]; // = 58
	public final static byte NULL_BYTE = NULL.getBytes(UTF_8)[0]; // = 0
	public final static byte ENDLINE_BYTE = "\n".getBytes(UTF_8)[0]; // = 10

	public Frame(Command command, Map<String, String> header, String message) {
		super();
		if (command == null) {
			throw new IllegalArgumentException("command is mandatory");
		}
		this.command = command;
		this.header = header;
		this.message = message;
	}

	public boolean is(Command command) {
		return this.command == command;
	}

	public MessageDecorator message() {
		MessageDecorator messageDecorator = null;
		if (is(Command.MESSAGE)) {
			messageDecorator = new MessageDecorator(this);
		}
		return messageDecorator;
	}

	public byte[] getBytes() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(command.name()).append(ENDLINE);
		if ((header != null) && (header.size() > 0)) {
			for (Entry<String, String> entry : header.entrySet()) {
				stringBuilder.append(entry.getKey()).append(HEADER_SEPARATOR).append(entry.getValue()).append(ENDLINE);
			}
		}
		if (message == null) {
			stringBuilder//
					.append(ENDLINE)//
					.append(NULL);
		} else {
			stringBuilder.append(ENDLINE)//
					.append(message)//
					.append(NULL);
		}

		return stringBuilder.toString().getBytes(UTF_8);
	}

	@Override
	public String toString() {
		return "Frame [command=" + command + ", header=" + header + ", message=" + message + "]";
	}

	protected class MessageDecorator {
		private final Frame frame;

		public MessageDecorator(Frame frame) {
			this.frame = frame;
		}

		public String messageId() {
			return this.frame.header.get("message-id");
		}

		public String subscription() {
			return this.frame.header.get("subscription");
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((command == null) ? 0 : command.hashCode());
		result = (prime * result) + ((header == null) ? 0 : headerHashCode());
		result = (prime * result) + ((message == null) ? 0 : message.hashCode());
		return result;
	}

	private int headerHashCode() {
		final int prime = 31;
		int result = 1;
		for (Map.Entry<String, String> headerEntry : header.entrySet()) {
			result = (prime * result) + headerEntry.getKey().hashCode() + headerEntry.getValue().hashCode();
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Frame other = (Frame) obj;
		if (command != other.command) {
			return false;
		}
		if (header == null) {
			if (other.header != null) {
				return false;
			}
		} else if (!equalsHeaderOf(other)) {
			return false;
		}
		if (message == null) {
			if (other.message != null) {
				return false;
			}
		} else if (!message.equals(other.message)) {
			return false;
		}
		return true;
	}

	private boolean equalsHeaderOf(Frame other) {
		boolean result = true;
		for (Map.Entry<String, String> headerEntry : header.entrySet()) {
			result &= other.header.containsKey(headerEntry.getKey()) && (headerEntry.getValue().equals(other.header.get(headerEntry.getValue())));
		}
		return result;
	}

}
