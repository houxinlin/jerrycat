package org.example;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassUtils {
    public static Map<String, Object> getAnnotationValues(List<Object> values) {
        if (values == null || values.size() == 0) return Collections.emptyMap();
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < values.size(); i += 2) {
            String name = values.get(0).toString();
            Object value = values.get(i + 1);
            if (value instanceof List) {
                result.put(name, ((List<?>) value).get(0));
            }
        }
        return result;
    }
}
