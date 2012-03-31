package fr.xebia.stomp.client;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import com.google.common.collect.Sets;

import fr.xebia.stomp.client.Connection.SocketParam;

public class PerformanceTest {
	private static final int NB_OF_MESSAGES = 100;
	private List<Connection> connections = new ArrayList<Connection>();
	private static final String CONTENT = "testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest";

	private Connection connectionSender;
	private Connection connectionReceiver;

	@Test
	public void sendAndReceiveMessagesInQueue() throws UnknownHostException, IOException {
		System.out.println("Normal\n\n");
		List<Integer> sizes = Arrays.asList(0, 1, 10, 100, 500, 1000, 10000, 100000, 500000, 1000000);
		for (Integer size : sizes) {
			setUp();
			sendAndReceiveMessagesInQueueOfSize(size, null);
			tearDown();
		}
	}

	@Test
	public void sendAndReceiveMessagesInQueueWithContentLength() throws UnknownHostException, IOException {
		System.out.println("With content length\n\n");
		List<Integer> sizes = Arrays.asList(0, 1, 10, 100, 500, 1000, 10000, 100000, 500000, 1000000);
		for (Integer size : sizes) {
			setUp();
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("content-length", String.valueOf(size));
			sendAndReceiveMessagesInQueueOfSize(size, headers);
			tearDown();
		}
	}

	@Test
	public void sendAndReceiveMessagesInQueueWithContentLengthAndPersistence() throws UnknownHostException, IOException {
		System.out.println("With content length and persistence\n\n");
		List<Integer> sizes = Arrays.asList(0, 1, 10, 100, 500, 1000, 10000, 100000, 500000, 1000000);
		for (Integer size : sizes) {
			setUp();
			Map<String, String> headers = new HashMap<String, String>();
			headers.put("content-length", String.valueOf(size));
			headers.put("persistent", "true");
			sendAndReceiveMessagesInQueueOfSize(size, headers);
			tearDown();
		}
	}

	private void sendAndReceiveMessagesInQueueOfSize(int size, Map<String, String> headers) throws UnknownHostException, IOException {
		// Build frame
		long expireTime = System.currentTimeMillis() + 10000; // Expires in 10s
		Frame frame = FrameBuilder.send()//
				.header("persistent", "false")//
				.header("expires", Long.toString(expireTime))//
				.message(of(size))//
				.to("/queue/test");
		if (headers != null) {
			for (Map.Entry<String, String> header : headers.entrySet()) {
				frame.header.put(header.getKey(), header.getValue());
			}
		}
		// Send frames
		send(frame, size);

		// Receive frames
		receive(size);
	}

	private void receive(int size) {
		long start = System.nanoTime();
		for (int i = 0; i < NB_OF_MESSAGES; i++) {
			connectionReceiver.receive();
		}
		long elapsedInMillis = (System.nanoTime() - start) / 1000000;
		System.out.println("Receive " + NB_OF_MESSAGES + " messages of " + size + "bytes in " + elapsedInMillis + "ms - " + ((NB_OF_MESSAGES * 1000) / elapsedInMillis) + "msg/s - "
				+ ((NB_OF_MESSAGES * size * 1000) / elapsedInMillis) + "bps");
	}

	private void send(Frame frame, int size) {
		long start = System.nanoTime();
		for (int i = 0; i < NB_OF_MESSAGES; i++) {
			connectionSender.send(frame);
		}
		long elapsedInMillis = (System.nanoTime() - start) / 1000000;
		System.out.println("Send " + NB_OF_MESSAGES + " messages of " + size + "bytes in " + elapsedInMillis + "ms - " + ((NB_OF_MESSAGES * 1000) / elapsedInMillis) + "msg/s - "
				+ ((NB_OF_MESSAGES * size * 1000) / elapsedInMillis) + "bps");

	}

	@Test
	public void should_receive_10000_messages_in_topic() throws UnknownHostException, IOException, InterruptedException {
		Connection connection1 = Connection.login("admin").passcode("password").to("localhost", 61613, SocketParam.TIMEOUT, 5000);
		final Connection connection2 = Connection.login("admin").passcode("password").to("localhost", 61613, SocketParam.TIMEOUT, 5000);
		connections.add(connection1);
		connections.add(connection2);
		FrameBuilder.subscribe(connection2).forClient("receiver").to("/topic/test");
		Thread.sleep(3000);
		final long start = System.nanoTime();
		new Thread(new Runnable() {

			@Override
			public void run() {
				for (int i = 0; i < NB_OF_MESSAGES; i++) {
					connection2.receive();
				}
				long elapsed = (System.nanoTime() - start) / 1000000;
				System.out.println("should_receive_10000_messages_in_topic=" + elapsed + "ms - " + ((NB_OF_MESSAGES * 1000) / elapsed) + "msg/s");
			}
		}).start();
		for (int i = 0; i < NB_OF_MESSAGES; i++) {
			FrameBuilder.send(connection1).header("persistent", "false").message(CONTENT).to("/topic/test");
		}
		Thread.sleep(5000);
	}

	public void setUp() {
		connectionSender = Connection.login("admin").passcode("password").to("localhost", 61613, SocketParam.TIMEOUT, 10000);
		connectionReceiver = Connection.login("admin").passcode("password").to("localhost", 61613, SocketParam.TIMEOUT, 10000);
		connections.add(connectionSender);
		connections.add(connectionReceiver);
		FrameBuilder.subscribe(connectionReceiver).forClient("receiver").to("/queue/test");
	}

	@After
	public void tearDown() {
		for (Connection connection : connections) {
			connection.unsubscribe("receiver");
			connection.closeQuietly();
		}
	}

	private String of(int size) {
		byte[] messageBytes = new byte[size];
		Arrays.fill(messageBytes, "a".getBytes()[0]);
		return new String(messageBytes);
	}
}
