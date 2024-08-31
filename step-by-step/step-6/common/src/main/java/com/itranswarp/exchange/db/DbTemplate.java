package com.itranswarp.exchange.db;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

/**
 * A simple ORM wrapper for JdbcTemplate.
 */
@Component
public class DbTemplate {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    final JdbcTemplate jdbcTemplate;

    // class -> Mapper:
    private Map<Class<?>, Mapper<?>> classMapping;

    public DbTemplate(@Autowired JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        String pkg = getClass().getPackageName();
        int pos = pkg.lastIndexOf(".");
        String basePackage = pkg.substring(0, pos) + ".model";

        List<Class<?>> classes = scanEntities(basePackage);
        Map<Class<?>, Mapper<?>> classMapping = new HashMap<>();
        try {
            for (Class<?> clazz : classes) {
                logger.info("Found class: " + clazz.getName());
                Mapper<?> mapper = new Mapper<>(clazz);
                classMapping.put(clazz, mapper);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.classMapping = classMapping;
    }

    public JdbcTemplate getJdbcTemplate() {
        return this.jdbcTemplate;
    }

    public String getTable(Class<?> clazz) {
        Mapper<?> mapper = classMapping.get(clazz);
        if (mapper == null) {
            throw new RuntimeException("Entity not registered: " + clazz.getSimpleName());
        }
        return mapper.tableName;
    }

    /**
     * Get a model instance by class type and id. EntityNotFoundException is thrown if not found.
     * 
     * @param <T>   Generic type.
     * @param clazz Entity class.
     * @param id    Id value.
     * @return Entity bean found by id.
     */
    public <T> T get(Class<T> clazz, Object id) {
        T t = fetch(clazz, id);
        if (t == null) {
            throw new EntityNotFoundException(clazz.getSimpleName());
        }
        return t;
    }

    /**
     * Get a model instance by class type and id. Return null if not found.
     * 
     * @param <T>   Generic type.
     * @param clazz Entity class.
     * @param id    Id value.
     * @return Entity bean found by id.
     */
    public <T> T fetch(Class<T> clazz, Object id) {
        Mapper<T> mapper = getMapper(clazz);
        if (logger.isDebugEnabled()) {
            logger.debug("SQL: {}", mapper.selectSQL);
        }
        List<T> list = jdbcTemplate.query(mapper.selectSQL, mapper.resultSetExtractor, id);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    /**
     * Remove bean by id.
     * 
     * @param bean The entity.
     */
    public <T> void delete(T bean) {
        try {
            Mapper<?> mapper = getMapper(bean.getClass());
            delete(bean.getClass(), mapper.getIdValue(bean));
        } catch (ReflectiveOperationException e) {
            throw new PersistenceException(e);
        }
    }

    /**
     * Remove bean by id.
     * 
     * @param bean The entity.
     */
    public <T> void delete(Class<T> clazz, Object id) {
        Mapper<?> mapper = getMapper(clazz);
        if (logger.isDebugEnabled()) {
            logger.debug("SQL: {}", mapper.deleteSQL);
        }
        jdbcTemplate.update(mapper.deleteSQL, id);
    }

    @SuppressWarnings("rawtypes")
    public Select select(String... selectFields) {
        return new Select(new Criteria(this), selectFields);
    }

    public <T> From<T> from(Class<T> entityClass) {
        Mapper<T> mapper = getMapper(entityClass);
        return new From<>(new Criteria<>(this), mapper);
    }

    /**
     * Update entity's updatable properties by id.
     * 
     * @param <T>  Generic type.
     * @param bean Entity object.
     */
    public <T> void update(T bean) {
        try {
            Mapper<?> mapper = getMapper(bean.getClass());
            Object[] args = new Object[mapper.updatableProperties.size() + 1];
            int n = 0;
            for (AccessibleProperty prop : mapper.updatableProperties) {
                args[n] = prop.get(bean);
                n++;
            }
            args[n] = mapper.id.get(bean);
            if (logger.isDebugEnabled()) {
                logger.debug("SQL: {}", mapper.updateSQL);
            }
            jdbcTemplate.update(mapper.updateSQL, args);
        } catch (ReflectiveOperationException e) {
            throw new PersistenceException(e);
        }
    }

    public <T> void insert(List<T> beans) {
        for (T bean : beans) {
            doInsert(bean, false);
        }
    }

    public <T> void insertIgnore(List<T> beans) {
        for (T bean : beans) {
            doInsert(bean, true);
        }
    }

    public <T> void insert(Stream<T> beans) {
        beans.forEach((bean) -> {
            doInsert(bean, false);
        });
    }

    public <T> void insertIgnore(Stream<T> beans) {
        beans.forEach((bean) -> {
            doInsert(bean, true);
        });
    }

    public <T> void insert(T bean) {
        doInsert(bean, false);
    }

    public <T> void insertIgnore(T bean) {
        doInsert(bean, true);
    }

    <T> void doInsert(T bean, boolean isIgnore) {
        try {
            int rows;
            final Mapper<?> mapper = getMapper(bean.getClass());
            Object[] args = new Object[mapper.insertableProperties.size()];
            int n = 0;
            for (AccessibleProperty prop : mapper.insertableProperties) {
                args[n] = prop.get(bean);
                n++;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("SQL: {}", isIgnore ? mapper.insertIgnoreSQL : mapper.insertSQL);
            }
            if (mapper.id.isIdentityId()) {
                // using identityId:
                KeyHolder keyHolder = new GeneratedKeyHolder();
                rows = jdbcTemplate.update(new PreparedStatementCreator() {
                    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                        PreparedStatement ps = connection.prepareStatement(
                                isIgnore ? mapper.insertIgnoreSQL : mapper.insertSQL, Statement.RETURN_GENERATED_KEYS);
                        for (int i = 0; i < args.length; i++) {
                            ps.setObject(i + 1, args[i]);
                        }
                        return ps;
                    }
                }, keyHolder);
                if (rows == 1) {
                    Number key = keyHolder.getKey();
                    if (key instanceof BigInteger) {
                        key = ((BigInteger) key).longValueExact();
                    }
                    mapper.id.set(bean, key);
                }
            } else {
                // id is specified:
                rows = jdbcTemplate.update(isIgnore ? mapper.insertIgnoreSQL : mapper.insertSQL, args);
            }
        } catch (ReflectiveOperationException e) {
            throw new PersistenceException(e);
        }
    }

    // get mapper by class:
    @SuppressWarnings("unchecked")
    <T> Mapper<T> getMapper(Class<T> clazz) {
        Mapper<T> mapper = (Mapper<T>) this.classMapping.get(clazz);
        if (mapper == null) {
            throw new RuntimeException("Target class is not a registered entity: " + clazz.getName());
        }
        return mapper;
    }

    private static List<Class<?>> scanEntities(String basePackage) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        List<Class<?>> classes = new ArrayList<>();
        Set<BeanDefinition> beans = provider.findCandidateComponents(basePackage);
        for (BeanDefinition bean : beans) {
            try {
                classes.add(Class.forName(bean.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return classes;
    }

    public String exportDDL() {
        return String.join("\n\n", this.classMapping.values().stream().map((mapper) -> {
            return mapper.ddl();
        }).sorted().toArray(String[]::new));
    }
}
