package com.itranswarp.exchange.db;

import java.util.ArrayList;
import java.util.List;

/**
 * select ... from ... ORDER BY ...
 * 
 * @param <T> Generic type.
 */
public final class OrderBy<T> extends CriteriaQuery<T> {

    public OrderBy(Criteria<T> criteria, String orderBy) {
        super(criteria);
        orderBy(orderBy);
    }

    /**
     * Order by field name.
     * 
     * @param orderBy The field name.
     * @return Criteria query object.
     */
    public OrderBy<T> orderBy(String orderBy) {
        if (criteria.orderBy == null) {
            criteria.orderBy = new ArrayList<>();
        }
        criteria.orderBy.add(orderBy);
        return this;
    }

    /**
     * Make a desc order by.
     * 
     * @return Criteria query object.
     */
    public OrderBy<T> desc() {
        int last = this.criteria.orderBy.size() - 1;
        String s = criteria.orderBy.get(last);
        if (!s.toUpperCase().endsWith(" DESC")) {
            s = s + " DESC";
        }
        criteria.orderBy.set(last, s);
        return this;
    }

    /**
     * Add limit clause.
     * 
     * @param maxResults The max results.
     * @return Criteria query object.
     */
    public Limit<T> limit(int maxResults) {
        return limit(0, maxResults);
    }

    /**
     * Add limit clause.
     * 
     * @param offset     Offset.
     * @param maxResults The max results.
     * @return Criteria query object.
     */
    public Limit<T> limit(int offset, int maxResults) {
        return new Limit<>(this.criteria, offset, maxResults);
    }

    /**
     * Get all results as list.
     * 
     * @return list.
     */
    public List<T> list() {
        return criteria.list();
    }

    /**
     * Get first row of the query, or null if no result found.
     * 
     * @return Object T or null.
     */
    public T first() {
        return criteria.first();
    }
}
