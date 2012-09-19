package fr.xebia.stomp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;

public class FrameInputStream implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(FrameInputStream.class);
    private BufferedReader bufferedReader;
    private boolean askingClose = false;

    /**
     * Constructor.
     *
     * @param socket the socket which will be read through its {@code InputStream}
     */
    public FrameInputStream(Socket socket) {
        try {
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF8"));
        } catch (IOException e) {
            throw new StompException(e);
        }
    }

    /**
     * Useful for testint without socket.
     *
     * @param inputStream the input stream which will be read
     */
    protected FrameInputStream(InputStream inputStream) throws UnsupportedEncodingException {
        this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF8"));
    }

    /**
     * Read a new frame.
     *
     * @return the new received frame from the connection
     */
    public Frame read() {
        StringBuilder stringBuilder = new StringBuilder();
        Command command;
        HashMap<String, String> header;
        try {
            // Read command
            LOGGER.trace("Read frame");
            byte currentByte;
            do {
                // Skip unnecessary bytes
                currentByte = (byte) bufferedReader.read();
            } while (!askingClose && ((Frame.ENDLINE_BYTE == currentByte) || (Frame.NULL_BYTE == currentByte)));

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Start read " + new String(new byte[]{currentByte}));
            }

            command = Command.valueOf((char) currentByte + bufferedReader.readLine());
            LOGGER.trace("Command {}", command);

            // Read header
            header = new HashMap<String, String>();
            char currentChar;
            while (!askingClose) {
                currentChar = (char) bufferedReader.read();
                if ('\n' != currentChar && '\r' != currentChar) {
                    do {
                        if (':' == currentChar) {
                            header.put(stringBuilder.toString(), bufferedReader.readLine());
                            stringBuilder=new StringBuilder();
                            break;
                        } else {
                            stringBuilder.append(currentChar);
                            LOGGER.trace("'{}'", currentChar);
                            currentChar = (char) bufferedReader.read();
                        }
                    } while (!askingClose);
                } else {
                    break;
                }
            }
            LOGGER.trace("header '{}'", header.entrySet().iterator().next().getKey());

            // Read message
            if (header.containsKey("content-length")) {
                int length = Integer.valueOf(header.get("content-length"));
                LOGGER.trace("Content length is setted to {}",length);
                stringBuilder = new StringBuilder(length);
                char[] buffer = new char[length];
                int read = 0;
                int totalRead = 0;
                while (read != -1 && totalRead < length) {
                    read = bufferedReader.read(buffer, totalRead, length-totalRead);
                    if(read!=-1){
                        totalRead+=read;
                    }
                }
                stringBuilder.append(buffer);
            } else {
                stringBuilder = new StringBuilder();
                currentChar = (char) bufferedReader.read();
                while (!askingClose && ('\00' != currentChar)) {
                    stringBuilder.append(currentChar);
                    currentChar = (char) bufferedReader.read();
                }
            }
            LOGGER.trace("message {}", stringBuilder);
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
        if (bufferedReader != null) {
            bufferedReader.close();
        }
    }

    public void closeQuietly() {
        try {
            close();
        } catch (IOException e) {
            LOGGER.warn("can't close connection: {}", e.getMessage());
        }
    }

}
