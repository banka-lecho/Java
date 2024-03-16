package info.kgeorgiy.ja.Shpileva.i18n;

import java.util.Locale;

public class MoneyStat extends NumberStat<Double> {
    private double sum;
    private double count;
    private double averageNumber;
    MoneyStat(Locale locale) {
        super(locale);
        sum = 0;
    }
    @Override
    void update(Double element) {
        super.update(element);
        sum += element;
        count++;
        averageNumber = sum / count;
    }
    public double getAvg() {
        return averageNumber;
    }
}
