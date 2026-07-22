package com.jummania;

import com.jummania.reader.Reader;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

import static com.jummania.FastCache.UNSAFE;

@SuppressWarnings("unchecked")
public final class Deserializer {

    public <T> T deserialize(Class<T> clazz, Reader reader) {
        return deserialize0(clazz, reader);
    }

    private <T> T deserialize0(Type type, Reader reader) {

        try {

            // Collection<T>
            if (type instanceof ParameterizedType parameterizedType) {

                Type rawType = parameterizedType.getRawType();

                if (rawType instanceof Class<?> rawClass && Collection.class.isAssignableFrom(rawClass)) {

                    int size = reader.readInt();

                    ArrayList<Object> list = new ArrayList<>(size);

                    Type itemType = parameterizedType.getActualTypeArguments()[0];

                    for (int i = 0; i < size; i++) {
                        list.add(deserialize0(itemType, reader));
                    }

                    return (T) list;
                }

                throw new IllegalStateException("Unsupported parameterized type: " + parameterizedType);
            }

            if (!(type instanceof Class<?> clazz)) {
                throw new IllegalStateException("Unsupported type: " + type);
            }

            // Primitive / Wrapper / String
            Object primitive = readPrimitive(clazz, reader);

            if (primitive != null) {
                return (T) primitive;
            }

            // Array
            if (clazz.isArray()) {

                int length = reader.readInt();

                Class<?> componentType = clazz.getComponentType();

                Object array = Array.newInstance(componentType, length);

                for (int i = 0; i < length; i++) {

                    Array.set(array, i, deserialize0(componentType, reader));
                }

                return (T) array;
            }

            // Object
            Object object = UNSAFE.allocateInstance(clazz);

            FastCache.CachedField[] fields = FastCache.get(clazz);

            for (FastCache.CachedField cached : fields) {

                switch (cached.kind()) {

                    case FastCache.INT -> cached.field().setInt(object, reader.readInt());

                    case FastCache.LONG -> cached.field().setLong(object, reader.readLong());

                    case FastCache.SHORT -> cached.field().setShort(object, reader.readShort());

                    case FastCache.BYTE -> cached.field().setByte(object, reader.readByte());

                    case FastCache.CHAR -> cached.field().setChar(object, reader.readChar());

                    case FastCache.BOOLEAN -> cached.field().setBoolean(object, reader.readBoolean());

                    case FastCache.FLOAT -> cached.field().setFloat(object, reader.readFloat());

                    case FastCache.DOUBLE -> cached.field().setDouble(object, reader.readDouble());

                    case FastCache.STRING -> cached.field().set(object, reader.readString());

                    default -> cached.field().set(object, deserialize0(cached.genericType(), reader));
                }
            }

            return (T) object;

        } catch (Throwable e) {
            return null;
        }
    }

    private Object readPrimitive(Class<?> clazz, Reader reader) throws Exception {

        if (clazz == int.class || clazz == Integer.class) return reader.readInt();

        if (clazz == long.class || clazz == Long.class) return reader.readLong();

        if (clazz == short.class || clazz == Short.class) return reader.readShort();

        if (clazz == byte.class || clazz == Byte.class) return reader.readByte();

        if (clazz == char.class || clazz == Character.class) return reader.readChar();

        if (clazz == boolean.class || clazz == Boolean.class) return reader.readBoolean();

        if (clazz == float.class || clazz == Float.class) return reader.readFloat();

        if (clazz == double.class || clazz == Double.class) return reader.readDouble();

        if (clazz == String.class) return reader.readString();

        return null;
    }
}