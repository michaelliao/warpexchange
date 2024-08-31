package com.itranswarp.exchange.support;

import org.springframework.beans.factory.annotation.Autowired;

import com.itranswarp.exchange.db.DbTemplate;

/**
 * Service with db support.
 */
public abstract class AbstractDbService extends LoggerSupport {

    @Autowired
    protected DbTemplate db;
}
