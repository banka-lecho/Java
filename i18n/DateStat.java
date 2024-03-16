package info.kgeorgiy.ja.Shpileva.i18n;

import java.util.Date;
import java.util.Locale;

public class DateStat extends StringStat<Date> {
    private Date sum;
    private int count;
    private Date averageNumber;

    //TODO:: проблема со средним числом

    DateStat(Locale locale) {
        super(locale);
        sum = new Date();
    }

    @Override
    void update(Date element) {
        super.update(element);
        Date sum = new Date(element.getTime() + sum.getTime());
        count++;
        averageNumber = sum / count;
    }

    public double getAvg() {
        return averageNumber;
    }
}
