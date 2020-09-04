package com.bakdata.conquery.models.events.stores;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;

import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.ColumnStore;

public class DecimalStore extends ColumnStoreAdapter<DecimalStore> {

	private final BigDecimal[] values;

	public DecimalStore(ImportColumn column, BigDecimal[] values) {
		super(column);
		this.values = values;
	}

	@Override
	public boolean has(int event) {
		return values[event] != null;
	}

	@Override
	public DecimalStore merge(List<? extends ColumnStore<?>> stores) {
		if (!stores.stream().allMatch(store -> store.getColumn().equals(getColumn()))) {
			throw new IllegalArgumentException("Not all stores belong to the same Column");
		}

		final int newSize = stores.stream().map(DecimalStore.class::cast).mapToInt(store -> store.values.length).sum();
		final BigDecimal[] mergedValues = new BigDecimal[newSize];

		int start = 0;

		for (ColumnStore<?> store : stores) {
			final DecimalStore doubleStore = (DecimalStore) store;

			System.arraycopy(doubleStore.values, 0, mergedValues, start, doubleStore.values.length);
			start += doubleStore.values.length;
		}

		return new DecimalStore(getColumn(), mergedValues);
	}

	@Override
	public BigDecimal getDecimal(int event) {
		return values[event];
	}

	@Override
	public Object getAsObject(int event) {
		return getDecimal(event);
	}

	@Override
	public void serialize(OutputStream outputStream) {

	}

}
