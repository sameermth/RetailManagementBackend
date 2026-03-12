package com.retailmanagement.common.utils;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemplateParser {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");

    public String parse(String template, Object data) {
        if (!StringUtils.hasText(template)) {
            return template;
        }

        if (data instanceof Map) {
            return parseWithMap(template, (Map<String, Object>) data);
        } else {
            return parseWithObject(template, data);
        }
    }

    private String parseWithMap(String template, Map<String, Object> data) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1).trim();
            Object value = data.get(key);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String parseWithObject(String template, Object data) {
        // Use reflection or JSON to access object properties
        // For simplicity, convert to Map using Jackson
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> map = mapper.convertValue(data, Map.class);
            return parseWithMap(template, map);
        } catch (Exception e) {
            return template;
        }
    }

    public String parseWithCustomFormatter(String template, Map<String, Object> data,
                                           Map<String, Formatter> formatters) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            String[] parts = expression.split("\\|");
            String key = parts[0].trim();

            Object value = data.get(key);

            // Apply formatters
            for (int i = 1; i < parts.length; i++) {
                String formatterName = parts[i].trim();
                Formatter formatter = formatters.get(formatterName);
                if (formatter != null) {
                    value = formatter.format(value);
                }
            }

            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    public interface Formatter {
        Object format(Object value);
    }

    // Built-in formatters
    public static class DateFormatter implements Formatter {
        private final String pattern;

        public DateFormatter(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public Object format(Object value) {
            if (value instanceof java.time.LocalDate) {
                return java.time.format.DateTimeFormatter.ofPattern(pattern).format((java.time.LocalDate) value);
            }
            if (value instanceof java.time.LocalDateTime) {
                return java.time.format.DateTimeFormatter.ofPattern(pattern).format((java.time.LocalDateTime) value);
            }
            return value;
        }
    }

    public static class CurrencyFormatter implements Formatter {
        @Override
        public Object format(Object value) {
            if (value instanceof Number) {
                return String.format("₹%,.2f", ((Number) value).doubleValue());
            }
            return value;
        }
    }

    public static class NumberFormatter implements Formatter {
        private final String pattern;

        public NumberFormatter(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public Object format(Object value) {
            if (value instanceof Number) {
                return String.format(pattern, value);
            }
            return value;
        }
    }

    public static class UpperCaseFormatter implements Formatter {
        @Override
        public Object format(Object value) {
            return value != null ? value.toString().toUpperCase() : null;
        }
    }

    public static class LowerCaseFormatter implements Formatter {
        @Override
        public Object format(Object value) {
            return value != null ? value.toString().toLowerCase() : null;
        }
    }

    public static class CapitalizeFormatter implements Formatter {
        @Override
        public Object format(Object value) {
            if (value == null) return null;
            String str = value.toString();
            if (str.isEmpty()) return str;
            return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
        }
    }
}