package info.kgeorgiy.ja.Shpileva.i18n;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.BreakIterator;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

import static java.util.Arrays.asList;

public class TextStatistics {
    private final Locale locale;
    private final NumberFormat numberFormat;
    private final NumberFormat currencyFormat;
    static String inputFilePath;

    public TextStatistics(Locale locale) {
        this.locale = locale;
        this.numberFormat = NumberFormat.getNumberInstance(locale);
        this.currencyFormat = NumberFormat.getCurrencyInstance(locale);
    }

    public void getStatistics() throws IOException {
        NumberStat<Double> numStats = new NumberStat<>(locale);
        NumberStat<Double> currencyStats = new NumberStat<>(locale);
        StringStat<String> sentenceStats = new StringStat<>(locale);
        StringStat<String> wordStats = new StringStat<>(locale);
        StringStat<Date> dateStats = new DateStat(locale);

        String text = fileToString();
        wordLevelStats(text, dateStats, currencyStats, wordStats, numStats);
        getStatsByLevel(text, sentenceStats, locale);
    }

    private String fileToString() throws IOException {
        try {
            return Files.readString(Paths.get(inputFilePath));
        }catch(IOException e){
            throw new IOException("Invalid file's path");
        }
    }

    private void wordLevelStats(String text, StringStat<Date> dateStats, NumberStat<Double> currencyStats,
                                StringStat<String> wordStats, NumberStat<Double> numStats) {

        BreakIterator iterator = BreakIterator.getWordInstance(locale);
        iterator.setText(text);
        int prevIndex = iterator.first();
        int index = iterator.next();
        boolean isCurrency;
        boolean isNumber;
        boolean isDate;
        while(index != BreakIterator.DONE){
            index = iterator.next();
            String word = text.substring(prevIndex, index).trim();
            isNumber = tryParseNumericToken(word, numStats, numberFormat);
            isCurrency = tryParseNumericToken(word, currencyStats, currencyFormat);

            int[] styles = {DateFormat.DEFAULT, DateFormat.FULL, DateFormat.LONG, DateFormat.MEDIUM, DateFormat.SHORT};
            try {
                for (int dateFormat : styles) {
                    DateFormat df = DateFormat.getDateInstance(dateFormat, locale);
                    Date date = df.parse(word);
                    dateStats.update(date);
                }
                isDate = true;
            } catch (ParseException ignored) {
                isDate = false;
            }

            if (!isDate && !isCurrency && !isNumber) {
                wordStats.update(word);
            }
        }
    }

    private void getStatsByLevel(String text, StringStat<String> stats, Locale locale) {
        BreakIterator iterator = BreakIterator.getSentenceInstance(locale);
        iterator.setText(text);
        int prevIndex;
        int index = iterator.next();

        while(index != BreakIterator.DONE){
            index = iterator.next();
            prevIndex = index;
            index = iterator.next();
            String token = text.substring(prevIndex, index).trim();
            if (!token.isEmpty()) {
                stats.update(token);
            }
        }
    }

    private boolean tryParseNumericToken(String word, NumberStat<Double> stats, NumberFormat format) {
        try {
            Double currency = format.parse(word).doubleValue();
            stats.update(currency);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    public static void main(String[] args) throws IOException {
        if (asList(args).contains(null)) {
            System.err.println("Invalid arguments.");
        }
        Locale textLocale = new Locale(args[0]);
        Locale outputLocale = new Locale(args[1]);
        inputFilePath = args[2];
        String outputFilePath = args[3];

        TextStatistics stat = new TextStatistics(textLocale);
        stat.getStatistics();

        if (stats == null) {
            return;
        }

        // тут должен быть вывод
    }
}