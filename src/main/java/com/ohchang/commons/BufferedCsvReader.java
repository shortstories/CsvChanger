package com.ohchang.commons;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author OhChang Kwon(ohchang.kwon@navercorp.com)
 *
 */
public class BufferedCsvReader implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(BufferedCsvReader.class);
  private static final int DEFAULT_BUFFER_SIZE = 4096;
  private static final int MAX_TIMEOUT = 5000;
  private static final String CHARSET_NAME = "UTF-8";

  private InputStream input;

  private byte[] buffer;

  private long offset;
  private int bufferLimit;
  private int bufferPosition;

  public BufferedCsvReader(final Path filePath) throws TimeoutException {
    this(filePath, DEFAULT_BUFFER_SIZE);
  }

  public BufferedCsvReader(final Path filePath, final int bufferSize) throws TimeoutException {
    waitUntilFileReadable(filePath);

    try {
      input = new FileInputStream(filePath.toFile());
    } catch (IOException e) {
      logger.error("Fail to open file", e);
      return;
    }

    buffer = new byte[bufferSize];
    offset = 0;
    bufferLimit = 0;
    bufferPosition = 0;
  }

  private boolean fillBuffer() throws IOException {
    int readCount = input.read(buffer, 0, buffer.length);

    // EOF
    if (readCount <= 0) {
      return false;
    }

    bufferLimit = readCount;
    bufferPosition = 0;

    return true;
  }

  private int findComma(final byte[] buffer, final int position, final int limit) {
    for (int i = position; i < limit; i++) {
      byte ch = buffer[i];

      if (ch == ',') {
        return i;
      }
    }

    return -1;
  }

//  private int handleNewLineCharOsDifferent(final byte[] buffer, final int newLineCharPosition, final int limit) {
//    int index = newLineCharPosition;
//
//    // for windows
//    if (buffer[index] == '\r'
//        && index + 1 < limit
//        && buffer[index + 1] == '\n') {
//      index++;
//    }
//
//    // include new line char
//    index++;
//
//    return index;
//  }

  private void waitUntilFileReadable(final Path filePath) throws TimeoutException {
    long timeout = 1_000L;

    // 윈도우에서는 파일이 복사될 때 그 즉시 읽을 수 없어 IOException 발생.
    // 파일이 읽기 불가 상태이면 정해진 MAX_TIMEOUT 까지
    // 기다려보고, 그래도 불가능하면 TimeoutException throw
    while (Files.isReadable(filePath) == false) {
      if (timeout > MAX_TIMEOUT) {
        throw new TimeoutException("File wait Timeout [Max " + MAX_TIMEOUT + "ms]");
      }

      try {
        Thread.sleep(timeout);
      } catch (InterruptedException e) {
        logger.warn("Unexpected interrupt", e);
      }

      timeout <<= 1;
    }
  }

  private String convertBufferToString(final byte[] buffer, final int newLineCharPosition,
      final int position, final ByteArrayOutputStream byteStream) throws IOException {
    String line;
    int length = newLineCharPosition - position;

    if (byteStream == null) {
      line = new String(buffer, position, length, CHARSET_NAME);
    } else {
      byteStream.write(buffer, position, length);
      line = byteStream.toString(CHARSET_NAME);
      byteStream.close();
    }

    bufferPosition = newLineCharPosition;
    offset += length;

    return line;
  }

  public String readCol() throws IOException {
    if (input == null) {
      throw new IllegalStateException("The file does not open");
    }

    ByteArrayOutputStream byteStream = null;

    for (;;) {
      if (bufferPosition >= bufferLimit) {
        if (fillBuffer() == false) {
          // EOF
          if (byteStream != null && byteStream.size() > 0) {
            String col = byteStream.toString(CHARSET_NAME);
            byteStream.close();

            return col;
          }

          return null;
        }
      }

      int commaCharPosition = findComma(buffer, bufferPosition, bufferLimit);

      if (commaCharPosition >= 0) {
//        commaCharPosition = handleNewLineCharOsDifferent(buffer, commaCharPosition, bufferLimit);
        commaCharPosition++;
        return convertBufferToString(buffer, commaCharPosition, bufferPosition, byteStream);
      }

      if (byteStream == null) {
        byteStream = new ByteArrayOutputStream(buffer.length << 1);
      }
      int length = bufferLimit - bufferPosition;
      byteStream.write(buffer, bufferPosition, length);
      bufferPosition = bufferLimit;
      offset += length;
    }
  }

  @Override
  public void close() throws IOException {
    try {
      input.close();
    } finally {
      input = null;
    }
  }

  public void skip(final long offset) throws IOException {
    this.offset = offset;

    if (offset > 0) {
      input.skip(offset);
    }
  }

  public long getOffset() {
    return offset;
  }
}
