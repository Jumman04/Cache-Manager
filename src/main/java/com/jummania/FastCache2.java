package com.jummania;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

public final class FastCache2 {

    static final byte INT = 1;
    static final byte LONG = 2;
    static final byte SHORT = 3;
    static final byte BYTE = 4;
    static final byte CHAR = 5;
    static final byte BOOLEAN = 6;
    static final byte FLOAT = 7;
    static final byte DOUBLE = 8;

    static final byte STRING = 9;

    static final byte ARRAY = 10;
    static final byte COLLECTION = 11;
    static final byte OBJECT = 12;

    private static final ConcurrentHashMap<Class<?>, CachedField[]> CACHE = new ConcurrentHashMap<>();

    static final Unsafe UNSAFE;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private FastCache2() {
    }

    public static CachedField[] get(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, FastCache2::build);
    }

    private static CachedField[] build(Class<?> clazz) {

        Field[] declared = clazz.getDeclaredFields();

        int validCount = 0;

        for (Field field : declared) {

            int modifiers = field.getModifiers();

            if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                continue;
            }

            validCount++;
        }

        CachedField[] fields = new CachedField[validCount];

        int index = 0;

        for (Field field : declared) {

            int modifiers = field.getModifiers();

            if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                continue;
            }

            field.setAccessible(true);

            Class<?> rawType = field.getType();

            byte kind = resolveKind(rawType);

            fields[index++] = new CachedField(field, field.getGenericType(), kind);
        }

        return fields;
    }

    private static byte resolveKind(Class<?> type) {

        if (type == int.class) return INT;
        if (type == long.class) return LONG;
        if (type == short.class) return SHORT;
        if (type == byte.class) return BYTE;
        if (type == char.class) return CHAR;
        if (type == boolean.class) return BOOLEAN;
        if (type == float.class) return FLOAT;
        if (type == double.class) return DOUBLE;

        if (type == String.class) return STRING;

        if (type.isArray()) return ARRAY;

        if (java.util.Collection.class.isAssignableFrom(type)) {
            return COLLECTION;
        }

        return OBJECT;
    }

    public record CachedField(Field field, Type genericType, byte kind) {
    }
}