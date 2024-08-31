package com.itranswarp.exchange.db;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Represent a bean public field with JPA annotation.
 */
class AccessibleProperty {

    // Field:
    private final Field field;

    // java type:
    final Class<?> propertyType;

    final Function<Object, Object> javaToSqlMapper;

    final Function<Object, Object> sqlToJavaMapper;

    // java bean property name:
    final String propertyName;

    // table column definition:
    final String columnDefinition;

    public Object get(Object bean) throws ReflectiveOperationException {
        Object obj = this.field.get(bean);
        if (this.javaToSqlMapper != null) {
            obj = this.javaToSqlMapper.apply(obj);
        }
        return obj;
    }

    public void set(Object bean, Object value) throws ReflectiveOperationException {
        if (this.sqlToJavaMapper != null) {
            value = this.sqlToJavaMapper.apply(value);
        }
        this.field.set(bean, value);
    }

    boolean isId() {
        return this.field.getAnnotation(Id.class) != null;
    }

    // is id && is id marked as @GeneratedValue(strategy=GenerationType.IDENTITY)
    boolean isIdentityId() {
        if (!isId()) {
            return false;
        }
        GeneratedValue gv = this.field.getAnnotation(GeneratedValue.class);
        if (gv == null) {
            return false;
        }
        GenerationType gt = gv.strategy();
        return gt == GenerationType.IDENTITY;
    }

    boolean isInsertable() {
        if (isIdentityId()) {
            return false;
        }
        Column col = this.field.getAnnotation(Column.class);
        return col == null || col.insertable();
    }

    boolean isUpdatable() {
        if (isId()) {
            return false;
        }
        Column col = this.field.getAnnotation(Column.class);
        return col == null || col.updatable();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AccessibleProperty(Field f) {
        this.field = f;
        this.propertyType = f.getType();
        this.propertyName = f.getName();
        this.columnDefinition = getColumnDefinition(this.propertyType);
        boolean isEnum = f.getType().isEnum();
        this.javaToSqlMapper = isEnum ? (obj) -> ((Enum<?>) obj).name() : null;
        this.sqlToJavaMapper = isEnum ? (obj) -> Enum.valueOf((Class<? extends Enum>) this.propertyType, (String) obj)
                : null;
    }

    private String getColumnDefinition(Class<?> type) {
        Column col = this.field.getAnnotation(Column.class);
        if (col == null) {
            throw new IllegalArgumentException("@Column not found on: " + this.field.toString());
        }
        if (!col.name().isEmpty()) {
            throw new IllegalArgumentException(
                    "@Column(name=\"" + col.name() + "\") is not supported: " + this.field.toString());
        }
        String colDef = null;
        if (col == null || col.columnDefinition().isEmpty()) {
            if (type.isEnum()) {
                colDef = "VARCHAR(32)";
            } else {
                colDef = getDefaultColumnType(type, col);
            }
        } else {
            colDef = col.columnDefinition().toUpperCase();
        }
        boolean nullable = col == null ? true : col.nullable();
        colDef = colDef + " " + (nullable ? "NULL" : "NOT NULL");

        if (isIdentityId()) {
            colDef = colDef + " AUTO_INCREMENT";
        }

        if (!isId() && col != null && col.unique()) {
            colDef = colDef + " UNIQUE";
        }

        return colDef;
    }

    private static String getDefaultColumnType(Class<?> type, Column col) {
        String ddl = DEFAULT_COLUMN_TYPES.get(type);
        if (ddl.equals("VARCHAR($1)")) {
            ddl = ddl.replace("$1", String.valueOf(col == null ? 255 : col.length()));
        }
        if (ddl.equals("DECIMAL($1,$2)")) {
            int preci = col == null ? 0 : col.precision();
            int scale = col == null ? 0 : col.scale();
            if (preci == 0) {
                preci = 10; // default DECIMAL precision of MySQL
            }
            ddl = ddl.replace("$1", String.valueOf(preci)).replace("$2", String.valueOf(scale));
        }
        return ddl;
    }

    static final Map<Class<?>, String> DEFAULT_COLUMN_TYPES = new HashMap<>();

    static {
        DEFAULT_COLUMN_TYPES.put(String.class, "VARCHAR($1)");

        DEFAULT_COLUMN_TYPES.put(boolean.class, "BIT");
        DEFAULT_COLUMN_TYPES.put(Boolean.class, "BIT");

        DEFAULT_COLUMN_TYPES.put(byte.class, "TINYINT");
        DEFAULT_COLUMN_TYPES.put(Byte.class, "TINYINT");
        DEFAULT_COLUMN_TYPES.put(short.class, "SMALLINT");
        DEFAULT_COLUMN_TYPES.put(Short.class, "SMALLINT");
        DEFAULT_COLUMN_TYPES.put(int.class, "INTEGER");
        DEFAULT_COLUMN_TYPES.put(Integer.class, "INTEGER");
        DEFAULT_COLUMN_TYPES.put(long.class, "BIGINT");
        DEFAULT_COLUMN_TYPES.put(Long.class, "BIGINT");
        DEFAULT_COLUMN_TYPES.put(float.class, "REAL");
        DEFAULT_COLUMN_TYPES.put(Float.class, "REAL");
        DEFAULT_COLUMN_TYPES.put(double.class, "DOUBLE");
        DEFAULT_COLUMN_TYPES.put(Double.class, "DOUBLE");

        DEFAULT_COLUMN_TYPES.put(BigDecimal.class, "DECIMAL($1,$2)");
    }

    @Override
    public String toString() {
        return "AccessibleProperty [propertyName=" + propertyName + ", propertyType=" + propertyType
                + ", columnDefinition=" + columnDefinition + "]";
    }
}
