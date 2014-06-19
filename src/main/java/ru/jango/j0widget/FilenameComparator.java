/*
 * The MIT License Copyright (c) 2014 Krayushkin Konstantin (jangokvk@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ru.jango.j0widget;

import java.util.Comparator;

import ru.jango.j0util.PathUtil;

/**
 * Special comparator for sorting lists of URIs, URLs, File objects etc.. In {@link #cmp(Object, Object)}
 * would be called {@link Object#toString()} on both objects, then filename would be extracted and
 * then they would be compared. Like this:
 * <p>
 * <code>
 * final String s1 = PathUtil.getFilenameWithoutExt(obj1.toString());
 * final String s2 = PathUtil.getFilenameWithoutExt(obj2.toString());
 * </code>
 * </p>
 *
 * @see java.util.Collections#sort(java.util.List, java.util.Comparator)
 */
public class FilenameComparator<T> implements Comparator<T> {

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
