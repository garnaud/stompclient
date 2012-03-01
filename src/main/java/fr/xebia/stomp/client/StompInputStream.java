package fr.xebia.stomp.client;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class StompInputStream implements Closeable {
	private DataInputStream dataInputStream;
	private boolean askingClose = false;

	public StompInputStream(Socket socket) {
		try {
			this.dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		} catch (IOException e) {
			throw new StompException(e);
		}
	}

	public Command command() throws IOException {
		System.out.println("Command");
		byte currentByte = dataInputStream.readByte();
		StringBuilder stringBuilder = new StringBuilder();
		do {
			stringBuilder.append((char) currentByte);
			currentByte = dataInputStream.readByte();
		} while ((Frame.ENDLINE_BYTE != currentByte) && !askingClose);
		return Command.valueOf(stringBuilder.toString());
	}

	public Frame frame() {
		StringBuilder stringBuilder = null;
		Command command;
		HashMap<String, String> header;
		try {
			// Read command
			System.out.println("Read frame");
			byte currentByte;
			do {
				// Skip unnecessary bytes
				currentByte = dataInputStream.readByte();
			} while (((Frame.ENDLINE_BYTE == currentByte) || (Frame.NULL_BYTE == currentByte)) && !askingClose);
			stringBuilder = new StringBuilder();
			System.out.println("Start read " + new String(new byte[] { currentByte }));

			do {
				stringBuilder.append((char) currentByte);
				currentByte = dataInputStream.readByte();
			} while ((Frame.ENDLINE_BYTE != currentByte) && !askingClose);
			command = Command.valueOf(stringBuilder.toString());
			System.out.println("Command " + command);

			// Read header
			header = new HashMap<String, String>();
			while (!askingClose) {
				currentByte = dataInputStream.readByte();
				if (Frame.ENDLINE_BYTE != currentByte) {
					stringBuilder = new StringBuilder();
					do {
						stringBuilder.append((char) currentByte);
						currentByte = dataInputStream.readByte();
					} while ((Frame.ENDLINE_BYTE != currentByte) && !askingClose);
					String[] headerKeyValue = stringBuilder.toString().split(Frame.HEADER_SEPARATOR);
					header.put(headerKeyValue[0], headerKeyValue[1]);
				} else {
					break;
				}
			}
			System.out.println("header " + header);

			// Read message
			stringBuilder = new StringBuilder();
			currentByte = dataInputStream.readByte();
			while ((Frame.NULL_BYTE != currentByte) && !askingClose) {
				stringBuilder.append((char) currentByte);
				currentByte = dataInputStream.readByte();
			}
			System.out.println("message " + stringBuilder);
		} catch (IOException e) {
			throw new StompException(e);
		} catch (IllegalArgumentException e) {
			throw new StompException("May be a problem occurs with parsing. Current string builder is '" + stringBuilder + "'", e);
		}

		return new Frame(command, header, stringBuilder.toString());
	}

	public Future<Command> commandAsync() {
		return Executors.newSingleThreadExecutor().submit(new Callable<Command>() {
			public Command call() throws Exception {
				return command();
			}
		});
	}

	public Future<Frame> frameAsync() {
		return Executors.newSingleThreadExecutor().submit(new Callable<Frame>() {
			public Frame call() throws Exception {
				return frame();
			}
		});
	}

	public void skip() throws IOException {
		System.out.println("skip");
		byte currentByte = dataInputStream.readByte();
		do {
			currentByte = dataInputStream.readByte();
			System.out.println(new String(new byte[] { currentByte }));
		} while (Frame.NULL_BYTE != currentByte);
	}

	public void close() throws IOException {
		askingClose = true;
		if (dataInputStream != null) {
			dataInputStream.close();
		}
	}

	public void closeQuietly() {
		try {
			close();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

}
