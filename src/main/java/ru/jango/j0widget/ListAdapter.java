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
