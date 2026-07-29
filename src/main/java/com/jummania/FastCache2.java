package com.jummania;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
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
    public static final byte ARRAY = 10;
    public static final byte COLLECTION = 11;
    public static final byte OBJECT = 12;

    private static final ConcurrentHashMap<Class<?>, FieldCacheMap> CACHE = new ConcurrentHashMap<>();
    private static final boolean USE_VAR_HANDLE;
    static Unsafe UNSAFE;

    static {
        boolean supported;
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);

            Class.forName("java.lang.invoke.VarHandle");
            supported = true;
        } catch (Exception e) {
            supported = false;
        }
        USE_VAR_HANDLE = supported;
    }

    private FastCache2() {
    }

    public static FieldCacheMap get(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, FastCache2::build);
    }

    private static FieldCacheMap build(Class<?> clazz) {
        try {
            Field[] declared = clazz.getDeclaredFields();

            VarHandle[] varHandles = USE_VAR_HANDLE ? new VarHandle[declared.length] : null;
            MethodHandle[] getters = USE_VAR_HANDLE ? null : new MethodHandle[declared.length];
            MethodHandle[] setters = USE_VAR_HANDLE ? null : new MethodHandle[declared.length];
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

                if (USE_VAR_HANDLE) {
                    varHandles[count] = privateLookup.unreflectVarHandle(field);
                } else {
                    getters[count] = privateLookup.unreflectGetter(field);
                    setters[count] = privateLookup.unreflectSetter(field);
                }

                count++;
            }

            // যদি স্ট্যাটিক বা ফাইনাল ফিল্ড বাদ পড়ার কারণে সাইজ কমে যায়, তবে ট্রিম করে নেওয়া
            if (count != declared.length) {
                if (USE_VAR_HANDLE) varHandles = Arrays.copyOf(varHandles, count);
                if (getters != null) getters = Arrays.copyOf(getters, count);
                if (setters != null) setters = Arrays.copyOf(setters, count);
                types = Arrays.copyOf(types, count);
                kinds = Arrays.copyOf(kinds, count);
            }

            return new FieldCacheMap(varHandles, getters, setters, types, kinds);

        } catch (Throwable e) {
            throw new RuntimeException("Field cache build failed: " + clazz.getName(), e);
        }
    }

    private static byte resolveKind(Class<?> type) {
        if (type == int.class || type == Integer.class) return INT;
        if (type == long.class || type == Long.class) return LONG;
        if (type == short.class || type == Short.class) return SHORT;
        if (type == byte.class || type == Byte.class) return BYTE;
        if (type == char.class || type == Character.class) return CHAR;
        if (type == boolean.class || type == Boolean.class) return BOOLEAN;
        if (type == float.class || type == Float.class) return FLOAT;
        if (type == double.class || type == Double.class) return DOUBLE;
        if (type == String.class) return STRING;
        if (type.isArray()) return ARRAY;
        if (java.util.Collection.class.isAssignableFrom(type)) return COLLECTION;
        return OBJECT;
    }

    public static Object getValue(int fieldIndex, Object instance, FieldCacheMap cacheMap) throws Throwable {
        if (USE_VAR_HANDLE) {
            return cacheMap.varHandles[fieldIndex].get(instance);
        }
        return cacheMap.getters[fieldIndex].invoke(instance);
    }

    public static void setValue(int fieldIndex, Object instance, FieldCacheMap cacheMap, Object value) throws Throwable {
        if (USE_VAR_HANDLE) {
            cacheMap.varHandles[fieldIndex].set(instance, value);
            return;
        }
        cacheMap.setters[fieldIndex].invoke(instance, value);
    }

    public record FieldCacheMap(
            VarHandle[] varHandles,
            MethodHandle[] getters,
            MethodHandle[] setters,
            Type[] types,
            byte[] kinds
    ) {
        public int size() {
            return kinds.length;
        }
    }
}