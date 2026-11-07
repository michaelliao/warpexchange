package org.puppylab.exchange.quotation;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.puppylab.exchange.model.quotation.DayBarEntity;
import org.puppylab.exchange.model.quotation.HourBarEntity;
import org.puppylab.exchange.model.quotation.MinBarEntity;
import org.puppylab.exchange.model.quotation.SecBarEntity;
import org.puppylab.exchange.model.quotation.TickEntity;
import org.puppylab.exchange.support.AbstractDbService;

@Component
@Transactional
public class QuotationDbService extends AbstractDbService {

    public void saveBars(SecBarEntity sec, MinBarEntity min, HourBarEntity hour, DayBarEntity day) {
        if (sec != null) {
            this.db.insertIgnore(sec);
        }
        if (min != null) {
            this.db.insertIgnore(min);
        }
        if (hour != null) {
            this.db.insertIgnore(hour);
        }
        if (day != null) {
            this.db.insertIgnore(day);
        }
    }

    public void saveTicks(List<TickEntity> ticks) {
        this.db.insertIgnore(ticks);
    }
}
