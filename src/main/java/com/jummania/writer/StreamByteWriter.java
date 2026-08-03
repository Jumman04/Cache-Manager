package com.jummania.writer;


import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class StreamByteWriter implements Writer, Closeable, Flushable {

    private final DataOutputStream out;

    public StreamByteWriter(DataOutputStream out) {
        this.out = out;
    }

    @Override
    public void writeByte(byte value) throws IOException {
        out.writeByte(value);
    }

    @Override
    public void writeBoolean(boolean value) throws IOException {
        out.writeBoolean(value);
    }

    @Override
    public void writeShort(short value) throws IOException {
        out.writeShort(value);
    }

    @Override
    public void writeChar(char value) throws IOException {
        out.writeChar(value);
    }

    @Override
    public void writeInt(int value) throws IOException {
        out.writeInt(value);
    }

    @Override
    public void writeLong(long value) throws IOException {
        out.writeLong(value);
    }

    @Override
    public void writeFloat(float value) throws IOException {
        out.writeFloat(value);
    }

    @Override
    public void writeDouble(double value) throws IOException {
        out.writeDouble(value);
    }

    @Override
    public void writeBytes(byte[] bytes) throws IOException {
        out.write(bytes.length);
        out.write(bytes);
    }

    @Override
    public void writeString(String value) throws IOException {

        if (value == null) {
            writeInt(0);
            return;
        }

        writeBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }
}