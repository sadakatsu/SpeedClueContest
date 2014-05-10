package com.sadakatsu.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * The Permutations class provides lazy iteration through all possible
 * permutations of items in a Collection.
 * 
 * @author Joseph A. Craig
 */
public class Permutations<T>
implements Iterable<List<T>>, Iterator<List<T>> {
	//********************* Protected and Private Fields *********************//
	private final int N;
	private int indices[];
	private final List<T> items;
	
	//*************************** Public Interface ***************************//
	/**
	 * Instantiates a new Permutations instance for iterating over the
	 * permutations of the passed Collection.
	 * @param values
	 * The items whose permutations needs to be found.
	 */
	public Permutations(Collection<T> values) {
		items = new ArrayList<>(values);
		N = items.size();
	}
	
	@Override
	public boolean hasNext() {
		return indices != null;
	}

	@Override
	public List<T> next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		
		List<T> next = new ArrayList<>();
		for (int i = 0; i < N; ++i) {
			next.add(items.get(indices[i]));
		}
		
		// This algorithm is based on the "next permutation" algorithm shown in
		// http://stackoverflow.com/questions/352203/generating-permutations-lazily .
		// It is taken from the C++ STL.
		if (N <= 1) {
			indices = null;
		} else {
			boolean stopped = false;
			int i = N - 1;
			while (!stopped) {
				int ii = i--;
				if (i < 0) {
					indices = null;
					stopped = true;
				} else if (indices[i] < indices[ii]) {
					int j = N;
					while (!(indices[i] < indices[--j])) {}
					swapIndices(i, j);
					reverseIndices(ii);
					stopped = true;
				}
			}
		}
		
		return next;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<List<T>> iterator() {
		indices = new int[N];
		for (int i = 0; i < N; ++i) {
			indices[i] = i;
		}
		return this;
	}
	
	//******************* Protected and Private Interface ********************//
	/**
	 * Reverses the order of all indices starting with the passed index through
	 * the last index.
	 * @param start
	 * The index at which to start the reversal.
	 */
	private void reverseIndices(int start) {
		for (int end = N - 1; start < end; ++start, --end) {
			swapIndices(start, end);
		}
	}
	
	/**
	 * Swaps two indices.
	 * @param i
	 * The position of the first index to swap.
	 * @param j
	 * The position of the second index to swap.
	 */
	private void swapIndices(int i, int j) {
		int swap = indices[i];
		indices[i] = indices[j];
		indices[j] = swap;
	}
	
	//*********************** Public Static Interface ************************//
	/**
	 * Calls the Permutations constructor with the passed arguments and returns
	 * the new instance.
	 * 
	 * This method is provided for style.  While the Permutations class is
	 * written so that an instance can be used for more than one iteration, it
	 * is unlikely that it would be.  This means that one would end up with code
	 * like
	 * 
	 *     Permutations<T> waste = new Permutations<>(items);
	 *     for (Collection<T> item : waste) // ...
	 * 
	 * or
	 * 
	 *     for (Collection<T> item : new Permutations<>(items)) // ...
	 * 
	 * These two are functionally identical to calling this method, but I prefer
	 * how the resulting code looks:
	 * 
	 *    for (Collection<T> item : Permutations.get(items)) // ...
	 * 
	 * @param values
	 * The items from which to draw the permutations.
	 * @return
	 * The Permutations<T> instance that will allow for iteration through the
	 * permutations of "values".
	 */
	public static <T> Permutations<T> get(Collection<T> values) {
		return new Permutations<>(values);
	}
}
