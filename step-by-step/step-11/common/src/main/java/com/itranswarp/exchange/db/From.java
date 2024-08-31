package com.itranswarp.exchange.db;

import java.util.List;

/**
 * select ... FROM ...
 *
 * @param <T> Generic type.
 */
public final class From<T> extends CriteriaQuery<T> {

    From(Criteria<T> criteria, Mapper<T> mapper) {
        super(criteria);
        this.criteria.mapper = mapper;
        this.criteria.clazz = mapper.entityClass;
        this.criteria.table = mapper.tableName;
    }

    /**
     * Add where clause.
     * 
     * @param clause Clause like "name = ?".
     * @param args   Arguments to match clause.
     * @return CriteriaQuery object.
     */
    public Where<T> where(String clause, Object... args) {
        return new Where<>(this.criteria, clause, args);
    }

    /**
     * Add order by clause.
     * 
     * @param orderBy Field of order by.
     * @return CriteriaQuery object.
     */
    public OrderBy<T> orderBy(String orderBy) {
        return new OrderBy<>(this.criteria, orderBy);
    }

    /**
     * Add limit clause.
     * 
     * @param maxResults The max results.
     * @return CriteriaQuery object.
     */
    public Limit<T> limit(int maxResults) {
        return limit(0, maxResults);
    }

    /**
     * Add limit clause.
     * 
     * @param offset     The offset.
     * @param maxResults The max results.
     * @return CriteriaQuery object.
     */
    public Limit<T> limit(int offset, int maxResults) {
        return new Limit<>(this.criteria, offset, maxResults);
    }

    /**
     * Get all results as list.
     * 
     * @return List of object T.
     */
    public List<T> list() {
        return this.criteria.list();
    }

    /**
     * Get first row of the query, or null if no result found.
     * 
     * @return Object T or null.
     */
    public T first() {
        return this.criteria.first();
    }

    /**
     * Get unique result of the query. Exception will throw if no result found or more than 1 results found.
     * 
     * @return T modelInstance
     * @throws jakarta.persistence.NoResultException        If result set is empty.
     * @throws jakarta.persistence.NonUniqueResultException If more than 1 results found.
     */
    public T unique() {
        return this.criteria.unique();
    }
}
