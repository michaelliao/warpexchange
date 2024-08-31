package com.itranswarp.exchange.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LoggerSupport {

    /**
     * 子类可以直接使用logger
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

}
