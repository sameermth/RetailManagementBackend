package com.retailmanagement.modules.erp.common.util;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ErpJsonPayloads {
    private ErpJsonPayloads() {}

    public static String of(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Key values must be even");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return toJson(map);
    }

    public static String toJson(Map<String, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        Iterator<? extends Map.Entry<String, ?>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ?> entry = iterator.next();
            sb.append("\"").append(escape(entry.getKey())).append("\":");
            sb.append(renderValue(entry.getValue()));
            if (iterator.hasNext()) {
                sb.append(',');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static String renderValue(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    public static String escape(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
