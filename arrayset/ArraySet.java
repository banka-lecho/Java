package info.kgeorgiy.ja.Shpileva.arrayset;

import java.util.*;

import static java.lang.Math.abs;

public class ArraySet<T> extends AbstractSet<T> implements SortedSet<T> {
    private final List<T> elements;
    private final Comparator<? super T> comparator;

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(Collection<T> elements) {
        this(elements, null);
    }

    public ArraySet(Collection<T> elements, Comparator<? super T> comparator) {
        TreeSet<T> sortedElements = new TreeSet<>(comparator);
        sortedElements.addAll(elements);
        this.elements = List.copyOf(sortedElements);
        this.comparator = comparator;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        return 0 <= Collections.binarySearch(elements, (T) o, comparator);
    }

    @Override
    public Iterator<T> iterator() {
        return elements.iterator();
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    private ArraySet(Comparator<? super T> comparator, List<T> elements) {
        this.comparator = comparator;
        this.elements = elements;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        int compare = compareElements(fromElement, toElement);
        if (compare > 0) {
            throw new IllegalArgumentException("First argument must be less than or equal to the second");
        } else if (compare == 0) {
            return new ArraySet<>(Collections.emptyList(), comparator);
        }

        int positionFrom = findPosition(fromElement);
        int positionTo = findPosition(toElement);

        return new ArraySet<>(comparator, elements.subList(positionFrom, positionTo));
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        int elementPosition = findPosition(toElement);
        return new ArraySet<>(comparator, elements.subList(0, elementPosition));
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        int elementPosition = findPosition(fromElement);
        return new ArraySet<>(comparator, elements.subList(elementPosition, size()));
    }

    @Override
    public T first() {
        checkNotEmpty();
        return elements.get(0);
    }

    @Override
    public T last() {
        checkNotEmpty();
        return elements.get(size() - 1);
    }

    private void checkNotEmpty() {
        if (size() == 0) {
            throw new NoSuchElementException();
        }
    }

    // :NOTE: SuppressWarnings
    @SuppressWarnings("unchecked")
    private int compareElements(T first, T second) {
        if (comparator == null) {
            return ((Comparable<? super T>) first).compareTo(second);
        } else {
            return comparator.compare(first, second);
        }
    }

    private int findPosition(T element) {
        int pos = Collections.binarySearch(elements, element, comparator);
        if (pos < 0) {
            return abs(pos + 1);
        }
        return pos;
    }
}
