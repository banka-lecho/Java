package info.kgeorgiy.ja.Shpileva.i18n;

import java.text.BreakIterator;
import java.util.*;

public class StringStat<T extends Comparable<? super T>> {
    //finals?
    private int minLength;
    private String withMinLength;
    private int maxLength;
    private String withMaxLength;
    private int sumLength;
    private Locale locale;
    private Map<T, Integer> cnt;
    private Set<T> unique;
    private T min;
    private T max;

    StringStat(Locale locale) {
        this.cnt = new HashMap<>();
        this.unique = new HashSet<>();
        min = null;
        max = null;
        minLength = 0;
        maxLength = 0;
        sumLength = 0;
        this.locale = locale;
    }

    void update(T element) {
        if (element == null) {
            return;
        }
        int count = cnt.getOrDefault(element, 0);
        cnt.put(element, count + 1);

        unique.add(element);
        setMinMax(element);

        BreakIterator it = BreakIterator.getCharacterInstance(locale);
        it.setText(element.toString());
        int index = it.first();
        int length = 0;
        while (index != BreakIterator.DONE) {
            length++;
            index = it.next();
        }
        length--;

        setMinMaxLength(length, element);
        sumLength += length;
    }

    protected void setMinMax(T element){
        if (element.compareTo(min) < 0) {
            min = element;
        }

        if (max.compareTo(element) < 0) {
            max = element;
        }
    }

    protected void setMinMaxLength(int length, T element){
        if (minLength == 0 || length < minLength) {
            minLength = length;
            withMinLength = element.toString();
        }

        if (length > maxLength) {
            maxLength = length;
            withMaxLength = element.toString();
        }
    }

    public T getMin() {
        return min;
    }

    public T getMax() {
        return max;
    }

    public Map<T, Integer> getCnt() {
        return cnt;
    }

    public String getWithMinLength() {
        return withMinLength;
    }

    public String getWithMaxLength() {
        return withMaxLength;
    }
    public Set<T> getUnique() {
        return unique;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StringStat)) {
            return false;
        }
        StringStat<?> stats = (StringStat<?>) o;
        if( getMinLength() == stats.getMinLength() && getMaxLength() == stats.getMaxLength() &&
                Objects.equals(getMin(), stats.getMin()) && Objects.equals(getMax(), stats.getMax()) &&
                locale.equals(stats.locale)){
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCnt(), getUnique(), getMin(), getMax(), getMinLength(), getWithMinLength(), getMaxLength(), getWithMaxLength(), sumLength, locale);
    }

    public Integer getMinLength() {
        return minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public double getAvg() {
        return (double)sumLength / sumLength;
    }
}