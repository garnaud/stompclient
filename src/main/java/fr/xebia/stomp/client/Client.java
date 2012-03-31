package fr.xebia.stomp.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Client {

	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		// Connection connection = Connection.login("admin").passcode("password").to("localhost", 61613);
		// System.out.println(connection);
		// connection.subscribe().forClient("you").to("/queue/a");
		// Future<Frame> futureReceivedMessage = connection.receiveAsync();
		// connection.send(FrameBuilder.send().message("coucou").to("/queue/a"));
		// try {
		// System.out.println("Message received:" + futureReceivedMessage.get());
		// } catch (ExecutionException e) {
		// e.printStackTrace();
		// }
		// connection.closeQuietly();
		System.out.println(Frame.ENDLINE_BYTE);
		System.out.println(Frame.HEADER_SEPARATOR_BYTE);
		System.out.println(Frame.NULL_BYTE);
	}

	public static void test(String[] args) throws UnknownHostException, IOException, InterruptedException {
		Socket socket = new Socket("0.0.0.0", 61613);
		new Thread(new Listener(socket.getInputStream())).start();

		System.out.println("listen message from server");

		OutputStream outputStream = socket.getOutputStream();
		StringBuilder message = new StringBuilder()//
				.append("CONNECT").append("\n")//
				.append("login:admin").append("\n")//
				.append("passcode:password").append("\n")//
				.append("\n")//
				.append("\000");
		System.out.println("write message:\n//" + message.toString() + "//");
		outputStream.write(message.toString().getBytes("UTF-8"));
		System.out.println("message written, go to flush");
		outputStream.flush();

		message = new StringBuilder()//
				.append("SEND").append('\n')//
				.append("destination:/queue/a").append('\n')//
				.append("receipt:001").append('\n')//
				.append('\n')//
				.append("coucou").append('\n')//
				.append("\000");
		System.out.println("write message:\n//" + message.toString() + "//");
		outputStream.write(message.toString().getBytes("UTF-8"));
		System.out.println("message written, go to flush");
		outputStream.flush();

		Thread.sleep(10000L);
	}

	static class Listener implements Runnable {
		private BufferedReader inputStream;

		public Listener(InputStream inputStream) {
			super();
			try {
				this.inputStream = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			try {
				while (true) {
					System.out.println(inputStream.readLine());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}
