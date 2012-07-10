package fr.xebia.stomp.client;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.SocketTimeoutException;

import static org.junit.Assert.assertEquals;

public class TopicIntegrationTest {


    @Test
    public void send_1_frame_to_1_topic_with_2_receivers() throws InterruptedException {
        // Init
        receiverConnection1.subscribe().forClient("receiver1").persistent().to("/topic/test");
        receiverConnection2.subscribe().forClient("receiver2").persistent().to("/topic/test");
        Frame sentFrame = senderConnection.send().message("test send").to("/topic/test");
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
	public void send_1_frame_to_1_topic_with_1_receiver() throws InterruptedException {
		// Init
		receiverConnection1.subscribe().forClient("receiver1").persistent().to("/topic/atopic");
        Thread.sleep(1000);
		receiverConnection1.subscribe().forClient("0").to("/dsub/receiver-topic");
        Frame sentFrame = senderConnection.send().message("test send").to("/dsub/receiver-topic");
        System.out.println("Send " + sentFrame);

		// Test
		Frame receivedFrame1 = receiverConnection1.receive();
		System.out.println("Received " + receivedFrame1);

		// Assert
		Assert.assertEquals(sentFrame.message, receivedFrame1.message);
	}

	private Connection senderConnection;
	private Connection receiverConnection1;
	private Connection receiverConnection2;

	@Before
	public void setUp() {
		senderConnection = Connection.login("admin").passcode("password").to("localhost", 61613);
		receiverConnection1 = Connection.login("admin").passcode("password").to("localhost", 61613, Connection.SocketParam.TIMEOUT, 1500);
		receiverConnection2 = Connection.login("admin").passcode("password").to("localhost", 61613, Connection.SocketParam.TIMEOUT, 1500);
	}

	@After
	public void tearDown() {
        receiverConnection1.subscribe().forClient("receiver1").to("/topic/test");
        receiverConnection2.subscribe().forClient("receiver2").to("/topic/test");
        FrameBuilder.send(senderConnection).message("poison pill").to("/topic/test");
        // empty all queues
        try {
            String currentMessage = "";
            while (!"poison pill".equals(currentMessage)) {
                Frame receive = receiverConnection1.receive();
                currentMessage = receive.message;
                System.out.println(currentMessage);
            }
        } catch (Exception e) {
        }
        try {
            String currentMessage = "";
            while (!"poison pill".equals(currentMessage)) {
                Frame receive = receiverConnection2.receive();
                currentMessage = receive.message;
                System.out.println(currentMessage);
            }
        } catch (Exception e) {
        }

        try {
            senderConnection.closeQuietly();
            receiverConnection1.closeQuietly();
            receiverConnection2.closeQuietly();
        } catch (Exception e) {
        }
    }
}
