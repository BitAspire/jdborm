package com.bitaspire.jdborm.mapper;

import com.bitaspire.jdborm.exception.JdbOrmException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps JDBC {@link ResultSet} rows to Java POJOs using reflection.
 * <p>
 * For each column, the mapper first attempts to find a setter method
 * (e.g. {@code setName} for column {@code name}), then falls back to
 * direct field access. Snake_case column names are converted to
 * camelCase/PascalCase Java names.
 * </p>
 */
public class ResultMapper {

    /**
     * Maps all rows in the ResultSet to a list of objects of the given type.
     *
     * @param rs   the ResultSet to iterate (the mapper does NOT close it)
     * @param type the target class
     * @param <T>  the result type
     * @return list of mapped objects (never null)
     * @throws JdbOrmException if mapping fails
     */
    public <T> List<T> mapAll(ResultSet rs, Class<T> type) {
        try {
            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRow(rs, type));
            }
            return results;
        } catch (SQLException e) {
            throw new JdbOrmException("Failed to map ResultSet to " + type.getName(), e);
        }
    }

    /**
     * Maps the current row of the ResultSet to a single object.
     *
     * @param rs   the ResultSet positioned at the current row
     * @param type the target class
     * @param <T>  the result type
     * @return the mapped object
     * @throws JdbOrmException if instantiation or mapping fails
     */
    public <T> T mapRow(ResultSet rs, Class<T> type) {
        try {
            T instance = type.getDeclaredConstructor().newInstance();
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                String columnName = meta.getColumnLabel(i);
                Object value = rs.getObject(i);
                if (value != null) {
                    setValue(instance, columnName, value);
                }
            }
            return instance;
        } catch (SQLException e) {
            throw new JdbOrmException("Failed to map row to " + type.getName(), e);
        } catch (ReflectiveOperationException e) {
            throw new JdbOrmException("Cannot instantiate " + type.getName(), e);
        }
    }

    private <T> void setValue(T instance, String columnName, Object value) {
        Class<?> type = instance.getClass();
        String setterName = "set" + toPascalCase(columnName);

        try {
            Method setter = findSetter(type, setterName, value);
            if (setter != null) {
                setter.invoke(instance, convertValue(value, setter.getParameterTypes()[0]));
                return;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Field field = findField(type, columnName);
            if (field != null) {
                field.setAccessible(true);
                field.set(instance, convertValue(value, field.getType()));
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private Method findSetter(Class<?> type, String setterName, Object value) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                if (paramType.isAssignableFrom(value.getClass())) {
                    return method;
                }
            }
        }
        for (Method method : type.getMethods()) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                return method;
            }
        }
        return null;
    }

    private Field findField(Class<?> type, String name) {
        String fieldName = toCamelCase(name);
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return value;

        if (targetType == Integer.class || targetType == int.class) {
            return ((Number) value).intValue();
        }
        if (targetType == Long.class || targetType == long.class) {
            return ((Number) value).longValue();
        }
        if (targetType == Double.class || targetType == double.class) {
            return ((Number) value).doubleValue();
        }
        if (targetType == Float.class || targetType == float.class) {
            return ((Number) value).floatValue();
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return value instanceof Number ? ((Number) value).intValue() != 0 : value;
        }
        if (targetType == String.class) {
            return value.toString();
        }

        return value;
    }

    private String toPascalCase(String name) {
        if (name == null || name.isEmpty()) return name;
        StringBuilder sb = new StringBuilder(name.length());
        boolean nextUpper = true;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    private String toCamelCase(String name) {
        if (name == null || name.isEmpty()) return name;
        StringBuilder sb = new StringBuilder(name.length());
        boolean nextUpper = false;
        for (char c : name.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}
