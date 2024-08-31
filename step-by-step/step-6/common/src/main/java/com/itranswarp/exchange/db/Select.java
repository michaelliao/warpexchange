package com.itranswarp.exchange.db;

import java.util.Arrays;

/**
 * SELECT ... from ...
 * 
 * Default to "*".
 */
@SuppressWarnings("rawtypes")
public final class Select extends CriteriaQuery {

    @SuppressWarnings("unchecked")
    Select(Criteria criteria, String... selectFields) {
        super(criteria);
        if (selectFields.length > 0) {
            this.criteria.select = Arrays.asList(selectFields);
        }
    }

    /**
     * Add from clause.
     * 
     * @param entityClass The entity class.
     * @return The criteria object.
     */
    @SuppressWarnings("unchecked")
    public <T> From<T> from(Class<T> entityClass) {
        return new From<T>(this.criteria, this.criteria.db.getMapper(entityClass));
    }
}
