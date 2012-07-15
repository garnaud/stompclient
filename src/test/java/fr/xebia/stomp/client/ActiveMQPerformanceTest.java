package fr.xebia.stomp.client;

import fr.xebia.stomp.client.Connection.SocketParam;
import org.apache.activemq.transport.stomp.StompConnection;
import org.apache.activemq.transport.stomp.StompFrame;
import org.junit.Test;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;

import static junit.framework.Assert.fail;

public class ActiveMQPerformanceTest {
    private static final long MAX_NUMBER_OF_MESSAGES = 10000;
    private static final long MAX_SIZE_OF_QUEUE_IN_BYTES = 200000000;
    private List<StompConnection> connections = new ArrayList<StompConnection>();

    private StompConnection connectionSender;
    private StompConnection connectionReceiver;

    @Test
    public void sendAndReceiveMessagesInQueue() throws Exception {
        System.out.println("\nsendAndReceiveMessagesInQueue\n");
        List<Integer> sizes = Arrays.asList(0, 1, 10, 100, 500, 1000, 10000, 100000, 500000, 1000000);
        for (Integer size : sizes) {
            setUp();
            sendAndReceiveMessagesInQueueOfSize(size, null);
            close();
        }
    }

    @Test
    public void sendAndReceiveMessagesInQueueWithContentLength() throws Exception {
        System.out.println("\nsendAndReceiveMessagesInQueueWithContentLength\n");
        List<Integer> sizes = Arrays.asList(0, 1, 10, 100, 500, 1000, 10000, 100000, 500000, 1000000);
        for (Integer size : sizes) {
            setUp();
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("content-length", String.valueOf(size));
            sendAndReceiveMessagesInQueueOfSize(size, headers);
            close();
        }
    }

    @Test
    public void sendAndReceiveMessagesInQueueWithContentLengthAndPersistence() throws Exception {
        System.out.println("\nsendAndReceiveMessagesInQueueWithContentLengthAndPersistence\n");
        List<Integer> sizes = Arrays.asList(0, 1, 10, 100, 500, 1000, 10000, 100000, 500000, 1000000);
        for (Integer size : sizes) {
            setUp();
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("content-length", String.valueOf(size));
            headers.put("persistent", "true");
            sendAndReceiveMessagesInQueueOfSize(size, headers);
            close();
        }
    }

    private void sendAndReceiveMessagesInQueueOfSize(int size, Map<String, String> headers) throws Exception {
        // Build frame
        long expireTime = System.currentTimeMillis() + 1200000; // Expires in 2mn
        HashMap<String, String> h = new HashMap<String, String>();
        h.put("destination","/queue/test");
        StompFrame frame = new StompFrame("SEND", h, of(size).getBytes("UTF-8"));
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                h.put(header.getKey(), header.getValue());
            }
        }
        // Send frames
        send(frame, size);

        // Receive frames
        receive(size);
    }

    private void receive(int size) throws Exception {
        long nbOfMessages = computeNumberOfMessages(size);
        long start = System.nanoTime();
        for (int i = 0; i < nbOfMessages; i++) {
            connectionReceiver.receiveFrame();
        }
        long elapsedInMillis = (System.nanoTime() - start) / 1000000;
        System.out.println("Receive " + nbOfMessages + " messages of " + size + "bytes in " + elapsedInMillis + "ms - " + ((nbOfMessages * 1000) / elapsedInMillis) + "msg/s - "
                + ((nbOfMessages * size * 1000) / elapsedInMillis) + " Bps");
    }

    private void send(StompFrame frame, int size) throws Exception {
        long nbOfMessages = computeNumberOfMessages(size);
        long start = System.nanoTime();
        for (int i = 0; i < nbOfMessages; i++) {
            connectionSender.sendFrame(frame.format());
        }
        long elapsedInMillis = (System.nanoTime() - start) / 1000000;
        System.out.println("Send " + nbOfMessages + " messages of " + size + " bytes in " + elapsedInMillis + "ms - " + (elapsedInMillis == 0 ? "infini" : ((nbOfMessages * 1000) / elapsedInMillis))
                + "msg/s - " + (elapsedInMillis == 0 ? "infini" : ((nbOfMessages * size * 1000) / elapsedInMillis) + " Bps"));
    }

    public void setUp() throws Exception {
        connectionSender = new StompConnection();
        connectionSender.open("localhost",61612);
        connectionSender.connect("system","manager");
        connectionReceiver = new StompConnection();
        connectionReceiver.open("localhost",61612);
        connectionReceiver.connect("system","manager");


        connections.add(connectionSender);
        connections.add(connectionReceiver);
        connectionReceiver.subscribe("/queue/test");
    }

    public void close() {
        for (StompConnection connection : connections) {
            try {
                connection.unsubscribe("/queue/test");
            } catch (Throwable e) {
                if(!"Socket is closed".equals(e.getMessage())){
                    e.printStackTrace();
                    fail();
                }
            }
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        connections.clear();
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
