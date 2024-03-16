package info.kgeorgiy.ja.Shpileva.i18n;

import java.util.Date;

public class OutputStatistic {
    private DateStat dates;
    private MoneyStat money;
    private StringStat<String> sentences;
    private StringStat<String> words;
    private NumberStat<Double> numbers;

    private OutputStatistic(DateStat dates, MoneyStat money, StringStat<String> sentences, StringStat<String> words,
                           NumberStat<Double> numbers, String fileName){
        this.dates = dates;
        this.money = money;
        this.sentences = sentences;
        this.words = words;
        this.numbers = numbers;
    }


}
