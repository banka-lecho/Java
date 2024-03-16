package info.kgeorgiy.ja.Shpileva.i18n.texts;

import info.kgeorgiy.ja.Shpileva.i18n.NumberStat;
import info.kgeorgiy.ja.Shpileva.i18n.TextStatistics;
import org.junit.Test;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;


public class Tests {
    private static final String RESOURCES_PATH = String.join(File.separator,
            "ru", "ifmo", "rain", "mozhevitin", "i18n", "resources");
    private static final String RUSSIAN_FILE_PATH = RESOURCES_PATH + File.separator + "russian.in";
    private static final String US_FILE_PATH = RESOURCES_PATH + File.separator + "us.in";
    private static final String ARAB_FILE_PATH = RESOURCES_PATH + File.separator + "arabic.in";

    @Test
    public void russianCategoriesFullTest() throws ParseException {
        Locale locale = new Locale.Builder().setRegion("RU").setLanguageTag("ru").build();
        TextStatistics st = new TextStatistics(locale);

        TotalStats total = st.getStatistics(RUSSIAN_FILE_PATH);

        NumberStat expectedNumStats = new NumberStat(locale);
        expectedNumStats.setSum(471);
        expectedNumStats.setMax(228d);
        expectedNumStats.setMin(1d);
        expectedNumStats.setMaxLength(3);
        expectedNumStats.setMinLength(1);

        assertEquals(expectedNumStats, total.getNumStats());
        assertEquals(7, TotalStats.totalCount(total.getNumStats()));
        assertEquals(6, total.getNumStats().getUnique().size());

        TextStat<Date> expectedDateStats = new TextStat<>(locale);
        Date date = DateFormat.getDateInstance(DateFormat.SHORT, locale).parse("26.05.2020");

        expectedDateStats.setMax(date);
        expectedDateStats.setMin(date);
        expectedDateStats.setMaxLength(date.toString().length());
        expectedDateStats.setMinLength(date.toString().length());

        assertEquals(expectedDateStats, total.getDateStats());

        TextStat<String> expectedWordStats = new TextStat<>(locale);
        expectedWordStats.setMaxLength("Русские".length());
        expectedWordStats.setMinLength(1);
        expectedWordStats.setMin("Ааа");
        expectedWordStats.setMax("я");

        assertEquals(expectedWordStats, total.getWordStats());

        TextStat<String> expectedSentenceStats = new TextStat<>(locale);
        expectedSentenceStats.setMin("Ааа.");
        expectedSentenceStats.setMax("Яяя.");
        expectedSentenceStats.setMinLength(4);
        expectedSentenceStats.setMaxLength(32);

        assertEquals(expectedSentenceStats, total.getSentenceStats());

        TextStat<String> expectedLineStats = new TextStat<>(locale);
        expectedLineStats.setMinLength(36);
        expectedLineStats.setMaxLength(42);
        expectedLineStats.setMax("а б я. Сегодня 26.05.2020. Ааа. Яяя.");
        expectedLineStats.setMin("Русские вперед. Русские 1 2 3 4 5 228 228.");

        assertEquals(expectedLineStats, total.getLineStats());
    }

    @Test
    public void totalUSTest() {
        Locale locale = Locale.US;

        TotalStats total = new TextStatistics(locale).getStatistics(US_FILE_PATH);
        assertEquals(206, total.getWordsCount());
        assertEquals(19, total.getNumsCount());
        assertEquals(12, total.getLinesCount());
        assertEquals(0, total.getDatesCount());
        assertEquals(0, total.getCurrenciesCount());
    }

    @Test
    public void totalArabicTest() {
        Locale locale = new Locale("ar");

        TotalStats total = new TextStatistics(locale).getStatistics(ARAB_FILE_PATH);
        System.out.println(total);

        assertEquals(22, total.getWordsCount());
        assertEquals(3, total.getLinesCount());
        assertEquals(2, total.getSentencesCount());
        assertEquals(0, total.getDatesCount());
        assertEquals(0, total.getNumsCount());
        assertEquals(0, total.getCurrenciesCount());
    }
}
