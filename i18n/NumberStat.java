package info.kgeorgiy.ja.Shpileva.i18n;

import java.util.*;

public class NumberStat<T extends Comparable<? super T>> {
    private Locale locale;
    private Map<T, Integer> cnt;
    private Set<T> unique;
    private T min;
    private T max;

    NumberStat(Locale locale) {
        this.cnt = new HashMap<>();
        this.unique = new HashSet<>();
        min = null;
        max = null;
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
    }

    protected void setMinMax(T element) {
        if (element.compareTo(min) < 0) {
            min = element;
        }

        if (max.compareTo(element) < 0) {
            max = element;
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

    public Set<T> getUnique() {
        return unique;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCnt(), getUnique(), getMin(), getMax(), locale);
    }

}