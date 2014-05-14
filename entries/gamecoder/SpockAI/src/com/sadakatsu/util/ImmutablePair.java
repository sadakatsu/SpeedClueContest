package com.sadakatsu.util;

public class ImmutablePair<T, U> {
	private T first;
	private U second;
	
	public ImmutablePair(T first, U second) {
		this.first = first;
		this.second = second;
	}
	
	public T getFirst() {
		return first;
	}
	
	public U getSecond() {
		return second;
	}
}
