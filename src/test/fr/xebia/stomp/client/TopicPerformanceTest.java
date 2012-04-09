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

public class TopicPerformanceTest {
	private static final int NB_OF_MESSAGES = 100;
	private List<Connection> connections = new ArrayList<Connection>();
	private static final String CONTENT = "testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest";

	private Connection connectionSender;
	private Connection connectionReceiver;

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
}
