package com.jummania;

public class SerializerBuilder {
    StringBuilder serializerCode = new StringBuilder();
    String targetPackage;

    SerializerBuilder(String targetPackage) {
        this.targetPackage = targetPackage;
    }

    StringBuilder append(String string) {
        return serializerCode.append(string);
    }
}
