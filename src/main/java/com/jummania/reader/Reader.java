package com.jummania.reader;

import java.io.IOException;

public interface Reader {

    byte readByte() throws IOException;

    boolean readBoolean() throws IOException;

    short readShort() throws IOException;

    char readChar() throws IOException;

    int readInt() throws IOException;

    long readLong() throws IOException;

    float readFloat() throws IOException;

    double readDouble() throws IOException;

    byte[] readBytes(int length) throws IOException;

    String readString(int length) throws IOException;

    default byte[] readBytes() throws IOException {
        return readBytes(readInt());
    }

    default String readString() throws IOException {
        return readString(readInt());
    }
}
