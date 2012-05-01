package fr.xebia.stomp.client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.transport.stomp.Stomp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrameBuilder {

	private final static Logger LOGGER = LoggerFactory.getLogger(FrameBuilder.class);

	private Command command;
	private Map<String, String> header = new HashMap<String, String>();
	private String message;

	/**
	 * If connection is not null, the method {@link Connection#send(Frame)} will be called at {@link FrameBuilder#end()}.
	 */
	private Connection connection;

	protected FrameBuilder() {
		// Nothing
	}

	protected FrameBuilder(Connection connection) {
		this.connection = connection;
	}

	public Frame end() {
		Frame frame = new Frame(command, header, message);
		if (connection != null) {
			connection.send(frame);
		}
		return frame;
	}

	public Frame end(Connection connection) {
		Frame frame = new Frame(command, header, message);
		connection.send(frame);
		return frame;
	}

	public FrameBuilder command(Command command) {
		this.command = command;
		return this;
	}

	public FrameBuilder header(String key, String value) {
		header.put(key, value);
		return this;
	}

	public FrameBuilder message(String message) {
		this.message = message;
		return this;
	}

	public FrameBuilder receipt() {
		command = Command.RECEIPT;
		return this;
	}

	public FrameBuilder messsage() {
		command = Command.MESSAGE;
		return this;
	}

	// Connect
	public static class ConnectBuilder {
		private final FrameBuilder frameBuilder;

		protected ConnectBuilder(FrameBuilder messageBuilder) {
			this.frameBuilder = messageBuilder;
		}

		public ConnectBuilder login(String login) {
			frameBuilder.header.put("login", login);
			return this;
		}

		public ConnectBuilder passcode(String passcode) {
			frameBuilder.header.put("passcode", passcode);
			return this;
		}

		public Connection to(String host, int port, Object... socketParams) {
			Connection localConnection;
			try {
				localConnection = new Connection(new Socket(host, port), socketParams);
			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			this.frameBuilder.end(localConnection);
			Frame serverResponse = localConnection.receive();
			if (serverResponse == null) {
				throw new IllegalStateException("received null response after a connection");
			} else if (Command.ERROR == serverResponse.command) {
				throw new IllegalStateException("After a CONNECTION request, receive a ERROR response " + serverResponse);
			} else if (Command.CONNECTED != serverResponse.command) {
				throw new IllegalStateException("After a CONNECTION request, receive a response which is not CONNECTED nor ERROR. Received frame is " + serverResponse);
			}
			LOGGER.debug("Connected to {}:{}, the CONNECTED frame from server is {}", new Object[] { host, port, serverResponse });
			return localConnection;
		}
	}

	// Disconnect
	public static class DisconnectBuilder {
		private final FrameBuilder frameBuilder;

		protected DisconnectBuilder(FrameBuilder messageBuilder) {
			this.frameBuilder = messageBuilder;
		}

		public Frame on(String receipt) {
			frameBuilder.header.put("receipt", receipt);
			return frameBuilder.end();
		}
	}

	// Send
	public static SendBuilder send() {
		return new SendBuilder(new FrameBuilder().command(Command.SEND));
	}

	public static SendBuilder send(Connection connection) {
		return new SendBuilder(new FrameBuilder(connection).command(Command.SEND));
	}

	public static class SendBuilder {
		private final FrameBuilder frameBuilder;

		public SendBuilder(FrameBuilder frameBuilder) {
			this.frameBuilder = frameBuilder;
		}

		public Frame to(String queue) {
			frameBuilder.header.put("destination", queue);
			return frameBuilder.end();
		}

		public SendBuilder withReceipt(String idReceipt) {
			frameBuilder.header.put("receipt", idReceipt);
			return this;
		}

		public SendBuilder expiresOn(long time) {
			frameBuilder.header.put("expires", String.valueOf(time));
			return this;
		}

		public SendBuilder header(String property, String value) {
			frameBuilder.header.put(property, value);
			return this;
		}

		public SendBuilder message(String message) {
			frameBuilder.message = message;
			return this;
		}
	}

	/**
	 * Build a subscribe frame.
	 * 
	 * @return
	 * @see Stomp 1.1 specification <a href="http://stomp.github.com/stomp-specification-1.1.html#SUBSCRIBE">subscription</a>
	 */
	public static SubscribeBuilder subscribe() {
		return new SubscribeBuilder(new FrameBuilder().command(Command.SUBSCRIBE));
	}

	public static SubscribeBuilder subscribe(Connection connection) {
		return new SubscribeBuilder(new FrameBuilder(connection).command(Command.SUBSCRIBE));
	}

	public static UnsubscribeBuilder unsubscribe() {
		return new UnsubscribeBuilder(new FrameBuilder().command(Command.UNSUBSCRIBE));
	}

	public static UnsubscribeBuilder unsubscribe(Connection connection) {
		return new UnsubscribeBuilder(new FrameBuilder(connection).command(Command.UNSUBSCRIBE));
	}

	public static class SubscribeBuilder {
		private final FrameBuilder frameBuilder;

		/**
		 * Use {@link FrameBuilder#subscribe()}.
		 * 
		 * @param frameBuilder
		 */
		protected SubscribeBuilder(FrameBuilder frameBuilder) {
			this.frameBuilder = frameBuilder;
		}

		/**
		 * Corresponding to the frame header <code>id</code>.
		 * 
		 * 
		 * @param clientId
		 * @return
		 * @see Stomp 1.1 specification <a href="http://stomp.github.com/stomp-specification-1.1.html#SUBSCRIBE_id_Header">client id</a>
		 */
		public SubscribeBuilder forClient(String clientId) {
			this.frameBuilder.header("id", clientId);
			return this;
		}

		/**
		 * Corresponding to the frame header <code>ack:auto</code>.
		 * 
		 * @return
		 * @see Stomp 1.1 specification <a href="http://stomp.github.com/stomp-specification-1.1.html#SUBSCRIBE_ack_Header">ack</a>
		 */
		public SubscribeBuilder autoAcknowledge() {
			this.frameBuilder.header("ack", "auto");
			return this;
		}

		/**
		 * Corresponding to the frame header <code>ack:client</code>.
		 * 
		 * @return
		 * @see Stomp 1.1 specification <a href="http://stomp.github.com/stomp-specification-1.1.html#SUBSCRIBE_ack_Header">ack</a>
		 */
		public SubscribeBuilder clientAcknowledge() {
			this.frameBuilder.header("ack", "client");
			return this;
		}

		/**
		 * Corresponding to the frame header <code>ack:client-individual</code>.
		 * 
		 * @return
		 * @see Stomp 1.1 specification <a href="http://stomp.github.com/stomp-specification-1.1.html#SUBSCRIBE_ack_Header">ack</a>
		 */
		public SubscribeBuilder clientIndividualAcknowledge() {
			this.frameBuilder.header("ack", "client-individual");
			return this;
		}

		/**
		 * Corresponding to the frame header <code>destination</code>.
		 * 
		 * @param destination
		 * @return the built frame
		 */
		public Frame to(String destination) {
			return frameBuilder.header("destination", destination).end();
		}
	}

	public static class UnsubscribeBuilder {
		private FrameBuilder frameBuilder;

		public UnsubscribeBuilder(FrameBuilder frameBuilder) {
			this.frameBuilder = frameBuilder;
		}

		public Frame to(String clientId) {
			return frameBuilder.header("id", clientId).end();
		}

	}

	// Ack
	public static class AckBuilder {
		private final FrameBuilder frameBuilder;

		protected AckBuilder(FrameBuilder frameBuilder) {
			this.frameBuilder = frameBuilder;
		}

		public AckBuilder messageId(String messageId) {
			frameBuilder.header("message-id", messageId);
			return this;
		}

		public AckBuilder transaction(String transaction) {
			frameBuilder.header("transaction", transaction);
			return this;
		}

		public Frame to(String subscription) {
			return frameBuilder.header("subscription", subscription).end();
		}
	}

	public static AckBuilder ack(Connection connection) {
		return new AckBuilder(new FrameBuilder(connection).command(Command.ACK));
	}

	public static AckBuilder ack() {
		return new AckBuilder(new FrameBuilder().command(Command.ACK));
	}

	// Nack
	public static class NackBuilder {
		private final FrameBuilder frameBuilder;

		protected NackBuilder(FrameBuilder frameBuilder) {
			this.frameBuilder = frameBuilder;
		}

		public NackBuilder messageId(String messageId) {
			frameBuilder.header("message-id", messageId);
			return this;
		}

		public NackBuilder transaction(String transaction) {
			frameBuilder.header("transaction", transaction);
			return this;
		}

		public Frame to(String subscription) {
			return frameBuilder.header("subscription", subscription).end();
		}
	}

	public static NackBuilder nack(Connection connection) {
		return new NackBuilder(new FrameBuilder(connection).command(Command.NACK));
	}

	public static NackBuilder nack() {
		return new NackBuilder(new FrameBuilder().command(Command.NACK));
	}

	// Begin
	public static class BeginBuilder {
		private final FrameBuilder frameBuilder;

		protected BeginBuilder(FrameBuilder frameBuilder) {
			this.frameBuilder = frameBuilder;
		}

		public Frame to(String transaction) {
			return frameBuilder.header("transaction", transaction).end();
		}
	}

	public static BeginBuilder begin(Connection connection) {
		return new BeginBuilder(new FrameBuilder(connection).command(Command.BEGIN));
	}

	public static BeginBuilder begin() {
		return new BeginBuilder(new FrameBuilder().command(Command.BEGIN));
	}

	// Commit
	public static class CommitBuilder {
		private final FrameBuilder frameBuilder;

		protected CommitBuilder(FrameBuilder frameBuilder) {
			this.frameBuilder = frameBuilder;
		}

		public Frame to(String transaction) {
			return frameBuilder.header("transaction", transaction).end();
		}
	}

	public static CommitBuilder commit(Connection connection) {
		return new CommitBuilder(new FrameBuilder(connection).command(Command.COMMIT));
	}

	public static CommitBuilder commit() {
		return new CommitBuilder(new FrameBuilder().command(Command.COMMIT));
	}

	// Abort
	public static class AbortBuilder {
		private final FrameBuilder frameBuilder;

		protected AbortBuilder(FrameBuilder frameBuilder) {
			this.frameBuilder = frameBuilder;
		}

		public Frame to(String transaction) {
			return frameBuilder.header("transaction", transaction).end();
		}
	}

	public static AbortBuilder abort(Connection connection) {
		return new AbortBuilder(new FrameBuilder(connection).command(Command.COMMIT));
	}

	public static AbortBuilder abort() {
		return new AbortBuilder(new FrameBuilder().command(Command.COMMIT));
	}
}
