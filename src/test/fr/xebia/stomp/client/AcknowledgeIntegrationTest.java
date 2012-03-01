package fr.xebia.stomp.client;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AcknowledgeIntegrationTest {

	@Test
	public void should_auto_acknowledge_message() {
		receiverConnection.subscribe().autoAcknowledge().forClient("receiver").to("/queue/test");
		senderConnection.send().message("test").to("/queue/test");
		Frame receivedFrame = receiverConnection.receive();
		System.out.println(receivedFrame);

		// FIXME How to test the acknowledged message?
	}

	@Test
	public void should_client_acknowledge_message() {
		// Init
		receiverConnection.subscribe().clientAcknowledge().forClient("receiver").to("/queue/test");
		senderConnection.send().message("test").to("/queue/test");
		Frame receivedFrame = receiverConnection.receive();
		System.out.println(receivedFrame);
		assertTrue(receivedFrame.is(Command.MESSAGE));
		String messageId = receivedFrame.message().messageId();
		String subscription = receivedFrame.message().subscription();
		assertEquals("receiver", subscription);

		// Test
		receiverConnection.closeQuietly();
		receiverConnectionBis.subscribe().clientAcknowledge().forClient("receiverBis").to("/queue/test");
		Frame receivedUnacknowledgedFrame = receiverConnectionBis.receive();
		System.out.println(receivedUnacknowledgedFrame);
		receiverConnectionBis.ack().message(messageId).to(subscription);

		// Assert
		// FIXME Wait with a Timeout
	}

	@Test
	public void should_client_individual_acknowledge_message() {
		// Init
		receiverConnection.subscribe().clientIndividualAcknowledge().forClient("receiver").to("/queue/test");
		senderConnection.send().message("test").to("/queue/test");
		Frame receivedFrame = receiverConnection.receive();
		System.out.println(receivedFrame);
		assertTrue(receivedFrame.is(Command.MESSAGE));
		String messageId = receivedFrame.message().messageId();
		String subscription = receivedFrame.message().subscription();
		assertEquals("receiver", subscription);

		// Test
		receiverConnection.closeQuietly();
		receiverConnectionBis.subscribe().clientAcknowledge().forClient("receiverBis").to("/queue/test");
		Frame receivedUnacknowledgedFrame = receiverConnectionBis.receive();
		System.out.println(receivedUnacknowledgedFrame);
		receiverConnectionBis.ack().message(messageId).to(subscription);

		// Assert
		// FIXME Wait with a Timeout
	}

	private Connection senderConnection;
	private Connection receiverConnection;
	private Connection receiverConnectionBis;

	@Before
	public void setUp() {
		senderConnection = Connection.login("admin").passcode("password").to("localhost", 61613);
		receiverConnection = Connection.login("admin").passcode("password").to("localhost", 61613, Connection.SocketParam.TIMEOUT, 500);
		receiverConnectionBis = Connection.login("admin").passcode("password").to("localhost", 61613, Connection.SocketParam.TIMEOUT, 500);
	}

	@After
	public void tearDown() {
		receiverConnection.unsubscribe("receiver");
		receiverConnection.unsubscribe("receiverBis");
		senderConnection.closeQuietly();
		receiverConnection.closeQuietly();
		receiverConnectionBis.closeQuietly();
	}
}
