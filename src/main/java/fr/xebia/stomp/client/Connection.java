package fr.xebia.stomp.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.xebia.stomp.client.FrameBuilder.ConnectBuilder;
import fr.xebia.stomp.client.FrameBuilder.SendBuilder;

public class Connection implements Closeable {
	private final Socket socket;
	private final FrameInputStream stompInputStream;

	private final static Logger LOGGER = LoggerFactory.getLogger(Connection.class);

	private ExecutorService executorService = Executors.newFixedThreadPool(1);

	protected Connection(Socket socket, Object... socketParams) {
		this.socket = socket;
		fillSocketParameters(socket, socketParams);
		stompInputStream = new FrameInputStream(socket);
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
			case RECEIVE_BUFFER_SIZE:
				try {
					socket.setReceiveBufferSize((Integer) socketParams[i + 1]);
					LOGGER.info("Set receive buffer size to {}", socket.getReceiveBufferSize());
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

	public Frame receive() {
		return stompInputStream.read();
	}

	/**
	 * Waiting for at most the given time a new message. Two cases should be observed if a timeout is reached.
	 * 
	 * Firstly the server has never sent the waited frame. So there is nothing to do except to attempt to wait again the new message.
	 * 
	 * Secondly the frame reception has started but not finished before the timeout. If the socket timeout has not been reached, this connection
	 * continues to waiting for the remained part of the message. So several problems, hard to predict, can occur if client calls again the receive
	 * method.
	 * 
	 * @param timeout
	 *            the maximum time to wait
	 * @param unit
	 *            the time unit of the timeout argument
	 * @return the new received frame in this connection
	 * @throws TimeoutException
	 *             if the timed is wait out
	 */
	public Frame receive(long timeout, TimeUnit unit) throws TimeoutException {
		Future<Frame> future = executorService.submit(new Callable<Frame>() {
			@Override
			public Frame call() {
				return stompInputStream.read();
			}
		});
		try {
			return future.get(timeout, unit);
		} catch (InterruptedException e) {
			throw new StompException(e);
		} catch (ExecutionException e) {
			throw new StompException(e);
		}
	}

	public Future<Frame> receiveAsync() {
		return executorService.submit(new Callable<Frame>() {
			@Override
			public Frame call() {
				return stompInputStream.read();
			}
		});

	}

	@Override
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
		TIMEOUT, RECEIVE_BUFFER_SIZE
	}

}
