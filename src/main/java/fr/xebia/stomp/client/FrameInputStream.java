package fr.xebia.stomp.client;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrameInputStream implements Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger(FrameInputStream.class);
	private DataInputStream dataInputStream;
	private boolean askingClose = false;

	public FrameInputStream(Socket socket) {
		try {
			this.dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		} catch (IOException e) {
			throw new StompException(e);
		}
	}

	/**
	 * Read a new frame.
	 * 
	 * @return the new received frame from the connection
	 */
	public Frame read() {
		StringBuilder stringBuilder = null;
		Command command;
		HashMap<String, String> header;
		try {
			// Read command
			LOGGER.trace("Read frame");
			byte currentByte;
			do {
				// Skip unnecessary bytes
				currentByte = dataInputStream.readByte();
			} while (((Frame.ENDLINE_BYTE == currentByte) || (Frame.NULL_BYTE == currentByte)) && !askingClose);
			stringBuilder = new StringBuilder();
			LOGGER.trace("Start read " + new String(new byte[] { currentByte }));

			do {
				stringBuilder.append((char) currentByte);
				currentByte = dataInputStream.readByte();
			} while ((Frame.ENDLINE_BYTE != currentByte) && !askingClose);
			command = Command.valueOf(stringBuilder.toString());
			LOGGER.trace("Command " + command);

			// Read header
			header = new HashMap<String, String>();
			while (!askingClose) {
				currentByte = dataInputStream.readByte();
				if (Frame.ENDLINE_BYTE != currentByte) {
					String key = "";
					stringBuilder = new StringBuilder();
					do {
						if (Frame.HEADER_SEPARATOR_BYTE == currentByte) {
							key = stringBuilder.toString();
							stringBuilder = new StringBuilder();
						} else {
							stringBuilder.append((char) currentByte);
						}
						currentByte = dataInputStream.readByte();
					} while ((Frame.ENDLINE_BYTE != currentByte) && !askingClose);
					header.put(key, stringBuilder.toString());
				} else {
					break;
				}
			}
			LOGGER.trace("header " + header);

			// Read message
			stringBuilder = new StringBuilder();
			currentByte = dataInputStream.readByte();
			while ((Frame.NULL_BYTE != currentByte) && !askingClose) {
				stringBuilder.append((char) currentByte);
				currentByte = dataInputStream.readByte();
			}
			LOGGER.trace("message " + stringBuilder);
		} catch (IOException e) {
			throw new StompException(e);
		} catch (IllegalArgumentException e) {
			throw new StompException("May be a problem occurs with parsing. Current string builder is '" + stringBuilder + "'", e);
		}

		return new Frame(command, header, stringBuilder.toString());
	}

	@Override
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
			LOGGER.warn("can't close connection", e);
		}
	}

}
