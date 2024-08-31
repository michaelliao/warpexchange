package com.itranswarp.exchange.db;

import java.util.ArrayList;
import java.util.List;

/**
 * select ... from ... WHERE ...
 * 
 * @param <T> Generic type.
 */
public final class Where<T> extends CriteriaQuery<T> {

    Where(Criteria<T> criteria, String clause, Object... params) {
        super(criteria);
        this.criteria.where = clause;
        this.criteria.whereParams = new ArrayList<>();
        // add:
        for (Object param : params) {
            this.criteria.whereParams.add(param);
        }
    }

    public Limit<T> limit(int maxResults) {
        return limit(0, maxResults);
    }

    public Limit<T> limit(int offset, int maxResults) {
        return new Limit<>(this.criteria, offset, maxResults);
    }

    public OrderBy<T> orderBy(String orderBy) {
        return new OrderBy<>(this.criteria, orderBy);
    }

    /**
     * Get all results as list.
     * 
     * @return list.
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
