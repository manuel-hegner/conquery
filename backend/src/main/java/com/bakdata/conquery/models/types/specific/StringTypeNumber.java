package com.bakdata.conquery.models.types.specific;

import java.util.Iterator;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.string.NumberStringStore;
import com.bakdata.conquery.models.types.CType;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@CPSType(base = CType.class, id = "STRING_NUMBER")
public class StringTypeNumber extends StringType {

	//used as a compact intset
	private Range<Integer> range;
	@Nonnull
	protected VarIntType delegate;
	
	@JsonCreator
	public StringTypeNumber(Range<Integer> range, VarIntType numberType) {
		super();
		this.range = range;
		this.delegate = numberType;
	}

	@Override
	public ColumnStore createStore(int size) {
		return NumberStringStore.create(size);
	}

	@Override
	public long estimateMemoryBitWidth() {
		return delegate.estimateMemoryBitWidth();
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[numberType=" + delegate + "]";
	}

	@Override
	public Iterator<String> iterator() {
		return IntStream
			.rangeClosed(
				range.getMin(),
				range.getMax()
			)
			.mapToObj(Integer::toString)
			.iterator();
	}

	@Override
	public Object createPrintValue(Integer value) {
		return value;
	}

	@Override
	public Object createScriptValue(Integer value) {
		return value.toString();
	}

	@Override
	public String getElement(int id) {
		return Integer.toString(id);
	}

	@Override
	public int size() {
		return range.getMax() - range.getMin() + 1;
	}

	@Override
	public int getId(String value) {
		try {
			int result = Integer.parseInt(value);
			if(range.contains(result)) {
				return result;
			}
			return -1;
		}
		catch(NumberFormatException e) {
			return -1;
		}
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
	public StringTypeNumber select(int[] starts, int[] length) {
		return new StringTypeNumber(range, delegate.select(starts, length));
	}

	@Override
	public Integer get(int event) {
		return getDelegate().get(event).intValue();
	}

	@Override
	public void set(int event, Integer value){
		getDelegate().set(event, value.longValue());
	}

	@Override
	public boolean has(int event){
		return getDelegate().has(event);
	}
}
