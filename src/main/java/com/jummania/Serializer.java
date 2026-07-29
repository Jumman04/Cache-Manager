package com.jummania;

import com.jummania.writer.Writer;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

public final class Serializer {

    public void serialize(
            Object obj,
            Writer writer
    ) {

        if (obj == null) {
            return;
        }

        serialize(
                obj,
                obj.getClass(),
                writer
        );
    }

    void serialize(
            Object obj,
            Type type,
            Writer writer
    ) {

        try {

            if (type instanceof ParameterizedType parameterizedType) {

                Type rawType =
                        parameterizedType.getRawType();

                if (rawType instanceof Class<?> rawClass
                        && Collection.class.isAssignableFrom(rawClass)) {

                    Collection<?> collection =
                            (Collection<?>) obj;

                    writer.writeInt(collection.size());

                    Type itemType =
                            parameterizedType
                                    .getActualTypeArguments()[0];

                    for (Object item : collection) {

                        if (item == null) {
                            throw new IllegalStateException(
                                    "Null collection item not supported"
                            );
                        }

                        serialize(
                                item,
                                itemType,
                                writer
                        );
                    }

                    return;
                }
            }

            if (!(type instanceof Class<?> clazz)) {
                throw new IllegalStateException(
                        "Unsupported type: " + type
                );
            }

            // Primitive / Wrapper / String
            if (writePrimitive(
                    clazz,
                    obj,
                    writer
            )) {
                return;
            }

            // Array
            if (clazz.isArray()) {

                int length =
                        Array.getLength(obj);

                writer.writeInt(length);

                Class<?> componentType =
                        clazz.getComponentType();

                for (int i = 0; i < length; i++) {

                    serialize(
                            Array.get(obj, i),
                            componentType,
                            writer
                    );
                }

                return;
            }

            // Collection without generic info
            if (Collection.class.isAssignableFrom(clazz)) {
                throw new IllegalStateException(
                        "Collection generic type required"
                );
            }

            // Object
            FastCache.CachedField[] fields =
                    FastCache.get(clazz);

            for (FastCache.CachedField cached : fields) {

                Field field =
                        cached.field();

                switch (cached.kind()) {

                    case FastCache.INT ->
                            writer.writeInt(
                                    field.getInt(obj)
                            );

                    case FastCache.LONG ->
                            writer.writeLong(
                                    field.getLong(obj)
                            );

                    case FastCache.SHORT ->
                            writer.writeShort(
                                    field.getShort(obj)
                            );

                    case FastCache.BYTE ->
                            writer.writeByte(
                                    field.getByte(obj)
                            );

                    case FastCache.CHAR ->
                            writer.writeChar(
                                    field.getChar(obj)
                            );

                    case FastCache.BOOLEAN ->
                            writer.writeBoolean(
                                    field.getBoolean(obj)
                            );

                    case FastCache.FLOAT ->
                            writer.writeFloat(
                                    field.getFloat(obj)
                            );

                    case FastCache.DOUBLE ->
                            writer.writeDouble(
                                    field.getDouble(obj)
                            );

                    case FastCache.STRING ->
                            writer.writeString(
                                    (String) field.get(obj)
                            );

                    default -> {

                        Object value =
                                field.get(obj);

                        if (value != null) {

                            serialize(
                                    value,
                                    cached.genericType(),
                                    writer
                            );
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean writePrimitive(
            Class<?> clazz,
            Object obj,
            Writer writer
    ) throws Exception {

        if (clazz == Integer.class) {
            writer.writeInt((Integer) obj);
            return true;
        }

        if (clazz == Long.class) {
            writer.writeLong((Long) obj);
            return true;
        }

        if (clazz == Short.class) {
            writer.writeShort((Short) obj);
            return true;
        }

        if (clazz == Byte.class) {
            writer.writeByte((Byte) obj);
            return true;
        }

        if (clazz == Character.class) {
            writer.writeChar((Character) obj);
            return true;
        }

        if (clazz == Boolean.class) {
            writer.writeBoolean((Boolean) obj);
            return true;
        }

        if (clazz == Float.class) {
            writer.writeFloat((Float) obj);
            return true;
        }

        if (clazz == Double.class) {
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