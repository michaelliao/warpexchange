package org.puppylab.exchange.model.quotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.puppylab.exchange.model.support.AbstractBarEntity;

/**
 * Store bars of second.
 */
@Entity
@Table(name = "sec_bars")
public class SecBarEntity extends AbstractBarEntity {

}
