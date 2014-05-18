package ru.jango.j0widget;

import java.util.Comparator;

import ru.jango.j0util.PathUtil;

/**
 * Special comparator for sorting strings. In {@link #cmp(Object, Object)} would be called
 * {@link Object#toString()} on both objects and then they would be compared.
 *
 * @see java.util.Collections#sort(java.util.List, java.util.Comparator)
 */
public class StringComparator<T> implements Comparator<T> {

    @Override
    public int compare(T obj1, T obj2) {
        return cmp(obj1, obj2);
    }

    public static int cmp(Object obj1, Object obj2) {
        if (obj1 == null && obj2 != null) return -1;
        if (obj1 != null && obj2 == null) return 1;
        if (obj1 == null && obj2 == null) return 0;

        final String s1 = PathUtil.getFilenameWithoutExt(obj1.toString());
        final String s2 = PathUtil.getFilenameWithoutExt(obj2.toString());

        if (s1.length() < s2.length()) return -1;
        else if (s1.length() > s2.length()) return 1;
        return s1.compareTo(s2);
    }
}
