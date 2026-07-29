package com.jummania;

import com.jummania.writer.Writer;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

public final class Serializer2 {

    public void serialize(Object obj, Writer writer) throws Throwable {
        if (obj == null) {
            return;
        }
        serialize(obj, obj.getClass(), writer);
    }

    void serialize(Object obj, Type type, Writer writer) throws Throwable {

        if (obj == null) {
            return;
        }

        // Collection<T>
        if (type instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();

            if (rawType instanceof Class<?> rawClass && Collection.class.isAssignableFrom(rawClass)) {
                Collection<?> collection = (Collection<?>) obj;
                writer.writeInt(collection.size());

                Type itemType = parameterizedType.getActualTypeArguments()[0];

                for (Object item : collection) {
                    if (item == null) {
                        throw new IllegalStateException("Null collection item not supported");
                    }
                    serialize(item, itemType, writer);
                }
                return;
            }
        }

        if (!(type instanceof Class<?> clazz)) {
            throw new IllegalStateException("Unsupported type: " + type);
        }

        // Primitive / Wrapper / String
        if (writePrimitive(clazz, obj, writer)) {
            return;
        }

        // Array
        if (clazz.isArray()) {
            int length = Array.getLength(obj);
            writer.writeInt(length);
            Class<?> componentType = clazz.getComponentType();

            for (int i = 0; i < length; i++) {
                serialize(Array.get(obj, i), componentType, writer);
            }
            return;
        }

        // Collection without generic info
        if (Collection.class.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Collection generic type required");
        }

        // Object
        FastCache2.FieldCacheMap cache = FastCache2.get(clazz);
        byte[] kinds = cache.kinds();
        Type[] types = cache.types();
        int size = kinds.length;

        for (int i = 0; i < size; i++) {
            byte kind = cache.kinds()[i];
            switch (kind) {
                // সরাসরি (int) এবং (long) এ কাস্ট করে invokeExact কল করুন, কোনো বক্সিং হবে না!
                case FastCache2.INT -> writer.writeInt((int) cache.getters()[i].invokeExact(obj));
                case FastCache2.LONG -> writer.writeLong((long) cache.getters()[i].invokeExact(obj));
                case FastCache2.SHORT -> writer.writeShort((short) cache.getters()[i].invokeExact(obj));
                case FastCache2.BYTE -> writer.writeByte((byte) cache.getters()[i].invokeExact(obj));
                case FastCache2.CHAR -> writer.writeChar((char) cache.getters()[i].invokeExact(obj));
                case FastCache2.BOOLEAN -> writer.writeBoolean((boolean) cache.getters()[i].invokeExact(obj));
                case FastCache2.FLOAT -> writer.writeFloat((float) cache.getters()[i].invokeExact(obj));
                case FastCache2.DOUBLE -> writer.writeDouble((double) cache.getters()[i].invokeExact(obj));
                case FastCache2.STRING -> writer.writeString((String) cache.getters()[i].invokeExact(obj));
                default -> {
                    Object value = cache.getters()[i].invokeExact(obj);
                    if (value != null) {
                        serialize(value, cache.types()[i], writer);
                    }
                }
            }

        }


    }

    private boolean writePrimitive(Class<?> clazz, Object obj, Writer writer) throws IOException {
        if (clazz == Integer.class || clazz == int.class) {
            writer.writeInt((Integer) obj);
            return true;
        }
        if (clazz == Long.class || clazz == long.class) {
            writer.writeLong((Long) obj);
            return true;
        }
        if (clazz == Short.class || clazz == short.class) {
            writer.writeShort((Short) obj);
            return true;
        }
        if (clazz == Byte.class || clazz == byte.class) {
            writer.writeByte((Byte) obj);
            return true;
        }
        if (clazz == Character.class || clazz == char.class) {
            writer.writeChar((Character) obj);
            return true;
        }
        if (clazz == Boolean.class || clazz == boolean.class) {
            writer.writeBoolean((Boolean) obj);
            return true;
        }
        if (clazz == Float.class || clazz == float.class) {
            writer.writeFloat((Float) obj);
            return true;
        }
        if (clazz == Double.class || clazz == double.class) {
            writer.writeDouble((Double) obj);
            return true;
        }
        if (clazz == String.class) {
            writer.writeString((String) obj);
            return true;
        }
        return false;
    }
}