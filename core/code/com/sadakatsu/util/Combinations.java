package com.sadakatsu.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * The Combinations class provides a lazy iteration through all possible
 * combinations of unique items in a Collection with a given size.
 * 
 * @author Joseph A. Craig
 */
public class Combinations<T>
implements Iterable<Collection<T>>, Iterator<Collection<T>> {
	//********************* Protected and Private Fields *********************//
	private final int N;
	private final int size;
	private int[] indices;
	private final List<T> items;
	
	//*************************** Public Interface ***************************//
	/**
	 * Instantiates a new Combinations instance for finding all unique
	 * combinations of the passed values that have the passed combination size.
	 * @param values
	 * The items from which to draw the combinations.
	 * @param combinationSize
	 * The number of items that each combination must have. 
	 */
	public Combinations(Collection<T> values, int combinationSize) {
		Set<T> set = new HashSet<>(values);
		items = new ArrayList<>(values.size() == set.size() ? values : set);
		N = items.size();
		size = combinationSize;
	}
	
	@Override
	public boolean hasNext() {
		if (indices != null && indices[0] > N - size) {
			indices = null;
		}
		return indices != null;
	}

	@Override
	public Collection<T> next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		
		List<T> next = new ArrayList<>();
		for (int i = 0; i < size; ++i) {
			next.add(items.get(indices[i]));
		}
		
		int j = 1;
		while (j <= size && (++indices[size - j]) > N - j) {
			++j;
		}
		for (int i = Math.max(1, size - j + 1); i < size; ++i) {
			indices[i] = indices[i - 1] + 1;
		}
		
		return next;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<Collection<T>> iterator() {
		indices = new int[size];
		for (int i = 0; i < size; ++i) {
			indices[i] = i;
		}
		return this;
	}
	
	//*********************** Public Static Interface ************************//
	/**
	 * Calls the Combinations constructor with the passed arguments and returns
	 * the new instance.
	 * 
	 * This method is provided for style.  While the Combinations class is
	 * written so that an instance can be used for more than one iteration, it
	 * is unlikely that it would be.  This means that one would end up with code
	 * like
	 * 
	 *     Combinations<T> waste = new Combinations<>(items, count);
	 *     for (Collection<T> item : waste) // ...
	 * 
	 * or
	 * 
	 *     for (Collection<T> item : new Combinations<>(items, count)) // ...
	 * 
	 * These two are functionally identical to calling this method, but I prefer
	 * how the resulting code looks:
	 * 
	 *    for (Collection<T> item : Combinations.get(items, count)) // ...
	 * 
	 * @param values
	 * The items from which to draw the combinations.
	 * @param combinationSize
	 * The number of items required in each combination.
	 * @return
	 * The Combinations<T> instance that will allow for iteration through the
	 * combinations from "values".
	 */
	public static <T> Combinations<T> get(
		Collection<T> values,
		int combinationSize
	) {
		return new Combinations<>(values, combinationSize);
	}
}
