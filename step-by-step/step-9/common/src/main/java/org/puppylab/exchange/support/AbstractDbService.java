package org.puppylab.exchange.support;

import org.springframework.beans.factory.annotation.Autowired;

import org.puppylab.exchange.db.DbTemplate;

/**
 * Service with db support.
 */
public abstract class AbstractDbService extends LoggerSupport {

    @Autowired
    protected DbTemplate db;
}
