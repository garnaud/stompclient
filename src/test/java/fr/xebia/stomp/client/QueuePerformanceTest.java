package fr.xebia.stomp.client;

import fr.xebia.stomp.client.Connection.SocketParam;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static junit.framework.Assert.fail;

public class QueuePerformanceTest {
    private static final long MAX_NUMBER_OF_MESSAGES = 10000;
    private static final long MAX_SIZE_OF_QUEUE_IN_BYTES = 200000000;
    private List<Connection> connections = new ArrayList<Connection>();

    private Connection connectionSender;
    private Connection connectionReceiver;

    @Test
    public void sendAndReceiveMessagesInQueue() throws IOException {
        System.out.println("\nsendAndReceiveMessagesInQueue\n");
        List<Integer> sizes = Arrays.asList(0, 1, 10, 100, 500, 1000, 10000, 100000, 500000, 1000000);
        for (Integer size : sizes) {
            System.out.println("Test size " + size);
            setUp();
            sendAndReceiveMessagesInQueueOfSize(size, null);
            tearDown();
        }
    }

    @Test
    public void sendAndReceiveMessagesInQueueWithContentLength() throws IOException {
        System.out.println("\nsendAndReceiveMessagesInQueueWithContentLength\n");
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
    public void sendAndReceiveMessagesInQueueWithContentLengthAndPersistence() throws IOException {
        System.out.println("\nsendAndReceiveMessagesInQueueWithContentLengthAndPersistence\n");
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

    private void sendAndReceiveMessagesInQueueOfSize(int size, Map<String, String> headers) throws IOException {
        // Build frame
        long expireTime = System.currentTimeMillis() + 1200000; // Expires in 2mn
        Frame frame = FrameBuilder.send()//
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
        long nbOfMessages = computeNumberOfMessages(size);
        long start = System.nanoTime();
        for (int i = 0; i < nbOfMessages; i++) {
            connectionReceiver.receive();
        }
        long elapsedInMillis = (System.nanoTime() - start) / 1000000;
        System.out.println("Receive " + nbOfMessages + " messages of " + size + "bytes in " + elapsedInMillis + "ms - " + ((nbOfMessages * 1000) / elapsedInMillis) + "msg/s - "
                + ((nbOfMessages * size * 1000) / elapsedInMillis) + " Bps");
    }

    private void send(Frame frame, int size) {
        long nbOfMessages = computeNumberOfMessages(size);
        long start = System.nanoTime();
        for (int i = 0; i < nbOfMessages; i++) {
            connectionSender.send(frame);
        }
        long elapsedInMillis = (System.nanoTime() - start) / 1000000;
        System.out.println("Send " + nbOfMessages + " messages of " + size + " bytes in " + elapsedInMillis + "ms - " + (elapsedInMillis == 0 ? "infini" : ((nbOfMessages * 1000) / elapsedInMillis))
                + "msg/s - " + (elapsedInMillis == 0 ? "infini" : ((nbOfMessages * size * 1000) / elapsedInMillis) + " Bps"));
    }

    public void setUp() {
        connectionSender = Connection.login("admin").passcode("password").to("localhost", 61613, SocketParam.TIMEOUT, 50000);
        connectionReceiver = Connection.login("admin").passcode("password").to("localhost", 61613, SocketParam.TIMEOUT, 50000);
        connections.add(connectionSender);
        connections.add(connectionReceiver);
        FrameBuilder.subscribe(connectionReceiver).forClient("receiver").to("/queue/test");
    }

    public void tearDown() {
        for (Connection connection : connections) {
            try {
                connection.unsubscribe("receiver");
            } catch (Throwable e) {
                if(!"Socket is closed".equals(e.getMessage())){
                    fail();
                }
            }
            connection.closeQuietly();
        }
    }

    private String of(int size) {
        byte[] messageBytes = new byte[size];
        Arrays.fill(messageBytes, "a".getBytes()[0]);
        return new String(messageBytes);
    }

    // Compute the number of messages depending on the size of each messages.
    private long computeNumberOfMessages(int size) {
        return Math.min(MAX_NUMBER_OF_MESSAGES, (MAX_SIZE_OF_QUEUE_IN_BYTES / Math.max(1, size)));
    }
}
