package com.jummania;

import com.jummania.reader.Reader;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

import static com.jummania.FastCache2.UNSAFE;

public final class Deserializer2 {

    public <T> T deserialize(Class<T> clazz, Reader reader) {
        try {
            return (T) deserialize0(clazz, reader);
        } catch (Throwable e) {
            return null;
        }
    }

    private Object deserialize0(Type type, Reader reader) throws Throwable {
        if (type instanceof Class<?> clazz) {

            if (clazz == int.class || clazz == Integer.class) return reader.readInt();
            if (clazz == long.class || clazz == Long.class) return reader.readLong();
            if (clazz == short.class || clazz == Short.class) return reader.readShort();
            if (clazz == byte.class || clazz == Byte.class) return reader.readByte();
            if (clazz == char.class || clazz == Character.class) return reader.readChar();
            if (clazz == boolean.class || clazz == Boolean.class) return reader.readBoolean();
            if (clazz == float.class || clazz == Float.class) return reader.readFloat();
            if (clazz == double.class || clazz == Double.class) return reader.readDouble();
            if (clazz == String.class) return reader.readString();

            if (clazz.isArray()) {
                int length = reader.readInt();
                Class<?> componentType = clazz.getComponentType();
                Object array = Array.newInstance(componentType, length);

                for (int i = 0; i < length; i++) {
                    Array.set(array, i, deserialize0(componentType, reader));
                }
                return array;
            }

            if (Collection.class.isAssignableFrom(clazz)) {
                throw new IllegalStateException("Collection generic type required");
            }

            Object object = UNSAFE.allocateInstance(clazz);
            FastCache2.FieldCacheMap cache = FastCache2.get(clazz);

            Type[] types = cache.types();
            byte[] kinds = cache.kinds();
            int fieldCount = types.length;

            for (int i = 0; i < fieldCount; i++) {
                Object value;
                byte kind = kinds[i];

                switch (kind) {
                    case FastCache2.INT -> value = reader.readInt();
                    case FastCache2.LONG -> value = reader.readLong();
                    case FastCache2.SHORT -> value = reader.readShort();
                    case FastCache2.BYTE -> value = reader.readByte();
                    case FastCache2.CHAR -> value = reader.readChar();
                    case FastCache2.BOOLEAN -> value = reader.readBoolean();
                    case FastCache2.FLOAT -> value = reader.readFloat();
                    case FastCache2.DOUBLE -> value = reader.readDouble();
                    case FastCache2.STRING -> value = reader.readString();
                    default -> value = deserialize0(types[i], reader);
                }

                FastCache2.setValue(i, object, cache, value);
            }

            return object;
        }

        if (type instanceof ParameterizedType p) {
            Type rawType = p.getRawType();

            if (rawType instanceof Class<?> rawClass && Collection.class.isAssignableFrom(rawClass)) {
                int size = reader.readInt();
                ArrayList<Object> list = new ArrayList<>(size);
                Type itemType = p.getActualTypeArguments()[0];

                for (int i = 0; i < size; i++) {
                    list.add(deserialize0(itemType, reader));
                }

                return list;
            }
        }

        throw new IllegalStateException("Unsupported type: " + type);
    }
}