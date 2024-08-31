package com.itranswarp.exchange.model.quotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import com.itranswarp.exchange.model.support.AbstractBarEntity;

/**
 * Store bars of day.
 */
@Entity
@Table(name = "day_bars")
public class DayBarEntity extends AbstractBarEntity {

}
