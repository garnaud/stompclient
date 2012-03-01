package fr.xebia.stomp.client;

import java.net.SocketTimeoutException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SendAndReceiveIntegrationTest {

	@Test
	public void sendAndReceive() {
		// Init
		receiverConnection1.subscribe().forClient("receiver1").to("/queue/test");
		Frame sentFrame = FrameBuilder.send().message("test send").to("/queue/test");
		senderConnection.send(sentFrame);
		System.out.println("Send " + sentFrame);

		// Test
		Frame receivedFrame = receiverConnection1.receive();
		System.out.println("Received " + receivedFrame);

		// Assert
		Assert.assertEquals(sentFrame.message, receivedFrame.message);
	}

	@Test
	public void send2FramesToReceiver() {
		// Init
		receiverConnection1.subscribe().forClient("send2FramesToReceiver").to("/queue/test");
		Frame sentFrame = FrameBuilder.send().message("test send2FramesToReceiver").to("/queue/test");
		senderConnection.send(sentFrame);
		senderConnection.send(sentFrame);
		System.out.println("Send " + sentFrame);

		// Test
		Frame receivedFrame1 = receiverConnection1.receive();
		System.out.println("Received " + receivedFrame1);
		Frame receivedFrame2 = receiverConnection1.receive();
		System.out.println("Received " + receivedFrame2);

		// Assert
		Assert.assertEquals(sentFrame.message, receivedFrame1.message);
		Assert.assertEquals(sentFrame.message, receivedFrame2.message);
	}

	@Test
	public void send1000FramesToReceiver() {
		// Init
		receiverConnection1.subscribe().forClient("send2FramesToReceiver").to("/queue/test");
		Frame sentFrame = FrameBuilder.send().message("test send2FramesToReceiver").to("/queue/test");
		System.out.println("Send " + sentFrame);
		for (int i = 0; i < 1000; i++) {
			senderConnection.send(sentFrame);
		}

		// Test
		for (int i = 0; i < 1000; i++) {
			Frame receive = receiverConnection1.receive();
			Assert.assertEquals(Command.MESSAGE, receive.command);
		}
	}

	@Test
	public void send_1_frame_to_1_topic_with_2_receivers() throws InterruptedException {
		// Init
		Frame sentFrame = FrameBuilder.send().message("test send").to("/topic/test");
		receiverConnection1.subscribe().forClient("receiver1").to("/topic/test");
		receiverConnection2.subscribe().forClient("receiver2").to("/topic/test");
		senderConnection.send(sentFrame);
		System.out.println("Send " + sentFrame);

		// Test
		Frame receivedFrame1 = receiverConnection1.receive();
		Frame receivedFrame2 = receiverConnection2.receive();
		System.out.println("Received " + receivedFrame1);
		System.out.println("Received " + receivedFrame2);

		// Assert
		Assert.assertEquals(sentFrame.message, receivedFrame1.message);
		Assert.assertEquals(sentFrame.message, receivedFrame2.message);
	}

	@Test
	public void should_not_receive_frame_after_unsubscription() throws InterruptedException {
		// Init
		Frame sentFrame = FrameBuilder.send().message("test send before unsubscription").to("/queue/test");
		receiverConnection1.subscribe().forClient("receiver1").to("/queue/test");
		senderConnection.send(sentFrame);
		System.out.println("Send " + sentFrame);

		// Test
		Frame receivedFrame1 = receiverConnection1.receive();
		System.out.println("Received " + receivedFrame1);
		receiverConnection1.unsubscribe("receiver1");
		Thread.sleep(1000);
		sentFrame = FrameBuilder.send().message("test send after unsubscription " + 1).to("/queue/test");
		senderConnection.send(sentFrame);

		// Assert
		try {
			Frame receivedFrame2 = receiverConnection1.receive();
			System.out.println("Received after unsubcription " + receivedFrame2);
			Assert.fail();
		} catch (StompException e) {
			assertEquals(SocketTimeoutException.class, e.getCause().getClass());
		}
	}

	private Connection senderConnection;
	private Connection receiverConnection1;
	private Connection receiverConnection2;

	@Before
	public void setUp() {
		senderConnection = Connection.login("admin").passcode("password").to("localhost", 61613);
		receiverConnection1 = Connection.login("admin").passcode("password").to("localhost", 61613, Connection.SocketParam.TIMEOUT, 500);
		receiverConnection2 = Connection.login("admin").passcode("password").to("localhost", 61613, Connection.SocketParam.TIMEOUT, 500);
	}

	@After
	public void tearDown() {
		receiverConnection1.unsubscribe("receiver1");
		receiverConnection1.unsubscribe("receiver2");
		senderConnection.closeQuietly();
		receiverConnection1.closeQuietly();
		receiverConnection2.closeQuietly();
	}
}
