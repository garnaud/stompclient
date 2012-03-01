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
	public final static byte NULL_BYTE = NULL.getBytes(UTF_8)[0];
	public final static byte ENDLINE_BYTE = "\n".getBytes(UTF_8)[0];

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
}
