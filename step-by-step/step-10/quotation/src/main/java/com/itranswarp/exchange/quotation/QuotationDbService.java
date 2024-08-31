package com.itranswarp.exchange.quotation;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itranswarp.exchange.model.quotation.DayBarEntity;
import com.itranswarp.exchange.model.quotation.HourBarEntity;
import com.itranswarp.exchange.model.quotation.MinBarEntity;
import com.itranswarp.exchange.model.quotation.SecBarEntity;
import com.itranswarp.exchange.model.quotation.TickEntity;
import com.itranswarp.exchange.support.AbstractDbService;

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
