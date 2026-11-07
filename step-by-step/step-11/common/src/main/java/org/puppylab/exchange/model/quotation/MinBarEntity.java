package org.puppylab.exchange.model.quotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.puppylab.exchange.model.support.AbstractBarEntity;

/**
 * Store bars of minute.
 */
@Entity
@Table(name = "min_bars")
public class MinBarEntity extends AbstractBarEntity {

}
