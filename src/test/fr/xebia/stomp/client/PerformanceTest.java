package fr.xebia.stomp.client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import fr.xebia.stomp.client.Connection.SocketParam;

public class PerformanceTest {
	private static final int ITERATIONS = 40000;
	private List<Connection> connections = new ArrayList<Connection>();

	@After
	public void tearDown() {
		for (Connection connection : connections) {
			connection.unsubscribe("receiver1");
			connection.unsubscribe("receiver2");
			connection.closeQuietly();
		}
	}

	@Test
	public void should_receive_10000_messages_in_queue() throws UnknownHostException, IOException {
		Connection connection1 = Connection.login("admin").passcode("password").to("localhost", 61613, SocketParam.TIMEOUT, 5000);
		Connection connection2 = Connection.login("admin").passcode("password").to("localhost", 61613, SocketParam.TIMEOUT, 5000);
		connections.add(connection1);
		connections.add(connection2);
		FrameBuilder.subscribe(connection2).forClient("receiver1").to("/queue/test");

		for (int i = 0; i < ITERATIONS; i++) {
			FrameBuilder.send(connection1).header("persistent", "false").message("test").to("/queue/test");
		}
		long start = System.nanoTime();
		for (int i = 0; i < ITERATIONS; i++) {
			Frame frame = connection2.receive();
		}
		long elapsed = (System.nanoTime() - start) / 1000000;

		System.out.println("should_receive_10000_messages_in_queue=" + elapsed + "ms - " + ((ITERATIONS * 1000) / elapsed) + "msg/s");
	}

	@Test
	public void should_receive_10000_messages_in_queue_in_same_connection() throws UnknownHostException, IOException {
		Connection connection = Connection.login("admin").passcode("password").to("localhost", 61613, SocketParam.TIMEOUT, 5000);
		connections.add(connection);
		FrameBuilder.subscribe(connection).forClient("receiver1").to("/queue/test");

		for (int i = 0; i < ITERATIONS; i++) {
			FrameBuilder.send(connection).header("persistent", "false").message("test").to("/queue/test");
		}
		long start = System.nanoTime();
		for (int i = 0; i < ITERATIONS; i++) {
			Frame frame = connection.receive();
		}
		long elapsed = (System.nanoTime() - start) / 1000000;
		System.out.println("should_receive_10000_messages_in_queue_in_same_connection=" + elapsed + "ms - " + ((ITERATIONS * 1000) / elapsed) + "msg/s");
	}

	@Test
	public void should_receive_10000_messages_in_topic() throws UnknownHostException, IOException, InterruptedException {
		Connection connection1 = Connection.login("admin").passcode("password").to("localhost", 61613, SocketParam.TIMEOUT, 5000);
		final Connection connection2 = Connection.login("admin").passcode("password").to("localhost", 61613, SocketParam.TIMEOUT, 5000);
		connections.add(connection1);
		connections.add(connection2);
		FrameBuilder.subscribe(connection2).forClient("receiver2").to("/topic/test");
		Thread.sleep(3000);
		final long start = System.nanoTime();
		new Thread(new Runnable() {

			@Override
			public void run() {
				for (int i = 0; i < ITERATIONS; i++) {
					Frame frame = connection2.receive();
				}
				long elapsed = (System.nanoTime() - start) / 1000000;
				System.out.println("should_receive_10000_messages_in_topic=" + elapsed + "ms - " + ((ITERATIONS * 1000) / elapsed) + "msg/s");
			}
		}).start();
		for (int i = 0; i < ITERATIONS; i++) {
			FrameBuilder.send(connection1).header("persistent", "false").message("test").to("/topic/test");
		}
		Thread.sleep(10000);
	}
}
