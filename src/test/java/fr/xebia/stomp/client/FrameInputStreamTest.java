package fr.xebia.stomp.client;

import com.google.common.base.Strings;
import org.apache.activemq.util.ByteArrayInputStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FrameInputStreamTest {

    @Test
    public void should_read_connect_frame() throws IOException {
        // Init
        Frame frameIn = new FrameBuilder()//
                .command(Command.CONNECT)//
                .header("host", "test.org")//
                .header("login", "test")//
                .header("passcode", "test")//
                .message("a message")//
                .end();
        FrameInputStream frameInputStream = new FrameInputStream(new BufferedInputStream(new ByteArrayInputStream(frameIn.getBytes())));

        // Test
        long startTime = System.nanoTime();
        Frame frameOut = frameInputStream.read();
        long elapsed = System.nanoTime() - startTime;

        // Assert
        assertNotNull(frameOut);
        assertEquals(frameIn, frameOut);
        System.out.println("elapsed: " + elapsed + "ns");
        Assert.assertEquals("Should change the delta or elapsed time for unit perf test", 200000, elapsed, 30000);
    }

    @Test
    public void should_read_frame_with_some_utf8_characters() throws IOException {
        // Init
        Frame frameIn = new FrameBuilder()//
                .command(Command.MESSAGE)//
                .header("0é0","toot")//
                .header("&é'(§è!çà)-$^`=;,°_*¨M+/.?end•¿#‰Ó¥Ô∏Œª","&é'(§è!çà)-$^`=;,°_*¨M+/.?end•¿#‰Ó¥Ô∏Œª")//
                .message("&é'(§è!çà)-$^`=:;,°_*¨M+/.?end•¿#‰Ó¥Ô∏Œª")//
                .end();
        FrameInputStream frameInputStream = new FrameInputStream(new BufferedInputStream(new ByteArrayInputStream(frameIn.getBytes())));

        // Test
        long startTime = System.nanoTime();
        Frame frameOut = frameInputStream.read();
        long elapsed = System.nanoTime() - startTime;

        // Assert
        assertNotNull(frameOut);
        assertEquals(frameIn, frameOut);
        System.out.println("elapsed: " + elapsed + "ns");
    }

    @Test
    public void should_read_message_with_content_length_equals_100000B() throws IOException {
        // Init
        String message = Strings.repeat("0", 100000);
        Frame frameIn = new FrameBuilder()//
                .command(Command.CONNECT)//
                .header("host", "test.org")//
                .header("login", "test")//
                .header("passcode", "test")//
                .header("content-length", String.valueOf(100000))//
                .message(message)//
                .end();
        FrameInputStream frameInputStream = new FrameInputStream(new BufferedInputStream(new ByteArrayInputStream(frameIn.getBytes())));

        // Test
        long startTime = System.nanoTime();
        Frame frameOut = frameInputStream.read();
        long elapsed = System.nanoTime() - startTime;

        // Assert
        assertNotNull(frameOut);
        assertEquals(frameIn, frameOut);
        System.out.println("elapsed: " + elapsed + "ns");
        Assert.assertEquals("Should change the delta or elapsed time for unit perf test", 1400000, elapsed, 400000);
    }

    @Test
    public void should_read_message_with_content_length_equals_10000B() throws IOException {
        // Init
        String message = Strings.repeat("0", 10000);
        Frame frameIn = new FrameBuilder()//
                .command(Command.CONNECT)//
                .header("host", "test.org")//
                .header("login", "test")//
                .header("passcode", "test")//
                .header("content-length", String.valueOf(10000))//
                .message(message)//
                .end();
        FrameInputStream frameInputStream = new FrameInputStream(new BufferedInputStream(new ByteArrayInputStream(frameIn.getBytes())));

        // Test
        long startTime = System.nanoTime();
        Frame frameOut = frameInputStream.read();
        long elapsed = System.nanoTime() - startTime;

        // Assert
        assertNotNull(frameOut);
        assertEquals(frameIn, frameOut);
        System.out.println("elapsed: " + elapsed + "ns");
        Assert.assertEquals("Should change the delta or elapsed time for unit perf test", 350000, elapsed, 150000);
    }

    @Test
    public void should_read_message_with_content_length_equals_1000B() throws IOException {
        // Init
        String message = Strings.repeat("0", 1000);
        Frame frameIn = new FrameBuilder()//
                .command(Command.CONNECT)//
                .header("host", "test.org")//
                .header("login", "test")//
                .header("passcode", "test")//
                .header("content-length", String.valueOf(1000))//
                .message(message)//
                .end();
        FrameInputStream frameInputStream = new FrameInputStream(new BufferedInputStream(new ByteArrayInputStream(frameIn.getBytes())));

        // Test
        long startTime = System.nanoTime();
        Frame frameOut = frameInputStream.read();
        long elapsed = System.nanoTime() - startTime;

        // Assert
        assertNotNull(frameOut);
        assertEquals(frameIn, frameOut);
        System.out.println("elapsed: " + elapsed + "ns");
        Assert.assertEquals("Should change the delta or elapsed time for unit perf test", 130000, elapsed, 40000);
    }
}
