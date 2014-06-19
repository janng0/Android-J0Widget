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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.widget.BaseAdapter;

/**
 * A simple wrapper for {@link android.widget.BaseAdapter}. Acts like an {@link android.widget.ArrayAdapter},
 * but doesn't need {@link android.content.Context} or any resources for displaying items.
 */
public abstract class ListAdapter<T extends Object> extends BaseAdapter implements Iterable<T> {
	private List<T> items = new LinkedList<T>();

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public T getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public Iterator<T> iterator() {
		return items.iterator();
	}

	public void clear() {
		items.clear();
		notifyDataSetChanged();
	}

	public List<T> getAll() {
		final List<T> itemsCopy = new ArrayList<T>();
		itemsCopy.addAll(items);

		return itemsCopy;
	}

	public void addAll(Collection<T> newItems) {
		items.addAll(newItems);
		notifyDataSetChanged();
	}

	public void add(T item) {
		items.add(item);
		notifyDataSetChanged();
	}

	public void remove(T item) {
		items.remove(item);
		notifyDataSetChanged();
	}

	public void remove(int index) {
		items.remove(index);
		notifyDataSetChanged();
	}

	public void sort(Comparator<T> cmp) {
		Collections.sort(items, cmp);
		notifyDataSetChanged();
	}

}
