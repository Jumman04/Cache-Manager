package com.jummania;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public final class FastCache2 {

    public static final byte INT = 1;
    public static final byte LONG = 2;
    public static final byte SHORT = 3;
    public static final byte BYTE = 4;
    public static final byte CHAR = 5;
    public static final byte BOOLEAN = 6;
    public static final byte FLOAT = 7;
    public static final byte DOUBLE = 8;
    public static final byte STRING = 9;
    public static final byte OBJECT = 12; // অন্য সব অবজেক্ট, অ্যারে বা কালেকশন এর জন্য

    private static final ConcurrentHashMap<Class<?>, FieldCacheMap> CACHE = new ConcurrentHashMap<>();

    private FastCache2() {
    }

    public static FieldCacheMap get(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, FastCache2::build);
    }

    private static FieldCacheMap build(Class<?> clazz) {
        try {
            Field[] declared = clazz.getDeclaredFields();
            MethodHandle[] getters = new MethodHandle[declared.length];
            MethodHandle[] setters = new MethodHandle[declared.length];
            Type[] types = new Type[declared.length];
            byte[] kinds = new byte[declared.length];

            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(clazz, lookup);

            int count = 0;
            for (Field field : declared) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || field.isSynthetic()) {
                    continue;
                }

                types[count] = field.getGenericType();
                Class<?> rawType = field.getType();
                kinds[count] = resolveKind(rawType);

                MethodHandle rawGetter = privateLookup.unreflectGetter(field);
                MethodHandle rawSetter = privateLookup.unreflectSetter(field);

                // টাইপ অনুযায়ী পারফেক্ট সিগনেচার ফিক্স করা (বক্সিং ছাড়া)
// টাইপ অনুযায়ী পারফেক্ট সিগনেচার ফিক্স করা
                if (rawType.isPrimitive()) {
                    // প্রিমিটিভের জন্য (যেমন: int, long)
                    getters[count] = rawGetter.asType(MethodType.methodType(rawType, Object.class));
                    setters[count] = rawSetter.asType(MethodType.methodType(void.class, Object.class, rawType));
                } else if (rawType == String.class) {
                    // স্ট্রিং এর জন্য সুনির্দিষ্ট টাইপ (String.class)
                    getters[count] = rawGetter.asType(MethodType.methodType(String.class, Object.class));
                    setters[count] = rawSetter.asType(MethodType.methodType(void.class, Object.class, String.class));
                } else {
                    // অন্য সব কাস্টম অবজেক্টের জন্য জেনারেলাইজড Object.class
                    getters[count] = rawGetter.asType(MethodType.methodType(Object.class, Object.class));
                    setters[count] = rawSetter.asType(MethodType.methodType(void.class, Object.class, Object.class));
                }

                count++;
            }

            if (count != declared.length) {
                getters = Arrays.copyOf(getters, count);
                setters = Arrays.copyOf(setters, count);
                types = Arrays.copyOf(types, count);
                kinds = Arrays.copyOf(kinds, count);
            }

            return new FieldCacheMap(getters, setters, types, kinds);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Field cache build failed: " + clazz.getName(), e);
        }
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
        return OBJECT;
    }

    public record FieldCacheMap(MethodHandle[] getters, MethodHandle[] setters, Type[] types, byte[] kinds) {
    }
}