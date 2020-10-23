package com.bakdata.conquery.models.types.specific;

import java.util.Collections;
import java.util.Iterator;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.BooleanStore;
import com.bakdata.conquery.models.events.stores.string.SingletonStringStore;
import com.bakdata.conquery.models.types.CType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.Iterators;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(base = CType.class, id = "STRING_SINGLETON")
public class StringTypeSingleton extends StringType {

	private final String singleValue;
	private final BooleanStore delegate;

	@JsonCreator
	public StringTypeSingleton(String singleValue, BooleanStore delegate) {
		super();
		this.singleValue = singleValue;
		this.delegate = delegate;
	}

	@Override
	public ColumnStore createStore(int size) {
		return new SingletonStringStore(singleValue, BooleanStore.create(size));
	}


	@Override
	public int size() {
		return singleValue == null ?0:1;
	}

	@Override
	public StringTypeSingleton select(int[] starts, int[] length) {
		return new StringTypeSingleton(singleValue,delegate.select(starts, length));
	}

	@Override
	public String getElement(int id) {
		return singleValue;
	}
	
	@Override
	public String createScriptValue(Integer value) {
		return singleValue;
	}

	@Override
	public int getId(String value) {
		if(value != null && value.equals(singleValue)) {
			return 0;
		}
		return -1;
	}
	
	@Override
	public Iterator<String> iterator() {
		if(singleValue == null) {
			return Collections.emptyIterator();
		}
		return Iterators.singletonIterator(singleValue);
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return Byte.SIZE;
	}
	
	@Override
	public Dictionary getUnderlyingDictionary() {
		return null;
	}
	
	@Override
	public void adaptUnderlyingDictionary(Dictionary newDict, VarIntType newNumberType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void set(int event, Integer value){
		getDelegate().set(event, true);
	}

	@Override
	public boolean has(int event){
		return getDelegate().has(event);
	}

	@Override
	public Integer get(int event) {
		return 0;
	}
}
