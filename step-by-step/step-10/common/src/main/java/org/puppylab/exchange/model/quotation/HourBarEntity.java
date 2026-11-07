package org.puppylab.exchange.model.quotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.puppylab.exchange.model.support.AbstractBarEntity;

/**
 * Store bars of hour.
 */
@Entity
@Table(name = "hour_bars")
public class HourBarEntity extends AbstractBarEntity {

}
