package fr.xebia.stomp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;

public class FrameInputStream implements Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger(FrameInputStream.class);
	private BufferedInputStream bufferedInputStream;
	private boolean askingClose = false;

	public FrameInputStream(Socket socket) {
		try {
			this.bufferedInputStream = new BufferedInputStream(socket.getInputStream());
		} catch (IOException e) {
			throw new StompException(e);
		}
	}

	public FrameInputStream(InputStream inputStream) {
		this.bufferedInputStream = new BufferedInputStream(inputStream);
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
				currentByte = (byte) bufferedInputStream.read();
			} while (((Frame.ENDLINE_BYTE == currentByte) || (Frame.NULL_BYTE == currentByte)) && !askingClose);
			stringBuilder = new StringBuilder();
			LOGGER.trace("Start read " + new String(new byte[] { currentByte }));

			do {
				stringBuilder.append((char) currentByte);
				currentByte = (byte) bufferedInputStream.read();
			} while ((Frame.ENDLINE_BYTE != currentByte) && !askingClose);
			command = Command.valueOf(stringBuilder.toString());
			LOGGER.trace("Command " + command);

			// Read header
			header = new HashMap<String, String>();
			while (!askingClose) {
				currentByte = (byte) bufferedInputStream.read();
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
						currentByte = (byte) bufferedInputStream.read();
					} while ((Frame.ENDLINE_BYTE != currentByte) && !askingClose);
					header.put(key, stringBuilder.toString());
				} else {
					break;
				}
			}
			LOGGER.trace("header " + header);

			// Read message
			if (header.containsKey("content-length")) {
				stringBuilder = new StringBuilder();
				int length = Integer.valueOf(header.get("content-length"));
				byte[] buffer = new byte[length];
				int read = 0;
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				while ((read != -1) && (byteArrayOutputStream.size() < length)) {
					read = bufferedInputStream.read(buffer, 0, length - byteArrayOutputStream.size());
					byteArrayOutputStream.write(buffer, 0, read);
				}
				stringBuilder.append(byteArrayOutputStream.toString());
			} else {
				stringBuilder = new StringBuilder();
				currentByte = (byte) bufferedInputStream.read();
				while ((Frame.NULL_BYTE != currentByte) && !askingClose) {
					stringBuilder.append((char) currentByte);
					currentByte = (byte) bufferedInputStream.read();
				}
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
		if (bufferedInputStream != null) {
			bufferedInputStream.close();
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
