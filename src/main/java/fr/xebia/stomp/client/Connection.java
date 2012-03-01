package fr.xebia.stomp.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.xebia.stomp.client.FrameBuilder.ConnectBuilder;
import fr.xebia.stomp.client.FrameBuilder.SendBuilder;

public class Connection implements Closeable {
	private final Socket socket;
	private final StompInputStream stompInputStream;

	private final static Logger LOGGER = LoggerFactory.getLogger(Connection.class);

	protected Connection(Socket socket, Object... socketParams) {
		this.socket = socket;
		fillSocketParameters(socket, socketParams);
		stompInputStream = new StompInputStream(socket);
	}

	private void fillSocketParameters(Socket socket, Object... socketParams) {
		if ((socketParams == null) || (socketParams.length == 0)) {
			return;
		}
		if ((socketParams.length % 2) != 0) {
			throw new IllegalStateException(
					"Missing a parameter or value. The parameters array for socket should contain an array [Connection.SocketParameter,Object,Connection.SocketParameter,Object...].");
		}
		for (int i = 0; i < socketParams.length; i = i + 2) {
			Object paramKey = socketParams[i];
			if (!(paramKey instanceof SocketParam)) {
				throw new IllegalArgumentException(
						"The parameter key should be a value of enum Connection.SocketParam. The parameters array for socket should contain an array [Connection.SocketParameter,Object,Connection.SocketParameter,Object...].");
			}
			switch ((SocketParam) paramKey) {
			case TIMEOUT:
				try {
					socket.setSoTimeout((Integer) socketParams[i + 1]);
					LOGGER.info("Set timeout of {}ms to the socket", socket.getSoTimeout());
				} catch (SocketException e) {
					throw new StompException(e);
				} catch (ClassCastException e) {
					throw new StompException("Wrong socket value parameter class", e);
				}
				break;
			default:
				break;
			}
		}
	}

	public static ConnectBuilder login(String login) {
		return new ConnectBuilder(new FrameBuilder().command(Command.CONNECT)).login(login);
	}

	public static Connection to(String host, int port, Object... socketParams) {
		return new ConnectBuilder(new FrameBuilder().command(Command.CONNECT)).to(host, port, socketParams);
	}

	/**
	 * Send a frame with a command {@link Command#SEND}.
	 * 
	 * @param frame
	 * @throws StompException
	 *             if the command is not {@link Command#SEND}
	 * @throws NullPointerException
	 *             if the frame is <code>null</code>
	 */
	public void send(Frame frame) {
		if (frame == null) {
			throw new NullPointerException("Can't send a frame null");
		}
		try {
			socket.getOutputStream().write(frame.getBytes());
			socket.getOutputStream().flush();
		} catch (IOException e) {
			LOGGER.error("Can't send the frame " + frame, e);
		}
	}

	/**
	 * Get a builder for sending a message and will send message when {@link FrameBuilder.SendBuilder#message(String)} is called.
	 * 
	 * @return {@link FrameBuilder.SendBuilder}
	 */
	public SendBuilder send() {
		return FrameBuilder.send(this);
	}

	public FrameBuilder.SubscribeBuilder subscribe() {
		return new FrameBuilder.SubscribeBuilder(new FrameBuilder(this).command(Command.SUBSCRIBE));
	}

	public void unsubscribe(String clientId) {
		try {
			Frame frame = FrameBuilder.unsubscribe().forClient(clientId);
			socket.getOutputStream().write(frame.getBytes());
			socket.getOutputStream().flush();
		} catch (IOException e) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.error("Problem during unsubscription", e);
			} else {
				LOGGER.warn("Problem during unsubscription", e.getMessage());
			}
		}
	}

	public Future<Frame> receiveAsync() {
		return stompInputStream.frameAsync();
	}

	public Frame receive() {
		return stompInputStream.frame();
	}

	public void close() throws IOException {
		socket.close();
	}

	public void closeQuietly() {
		try {
			close();
		} catch (IOException e) {
			LOGGER.error("Can't close the connection", e);
		}
	}

	public FrameBuilder.AckBuilder ack() {
		return FrameBuilder.ack(this);
	}

	public enum SocketParam {
		TIMEOUT
	}

}
