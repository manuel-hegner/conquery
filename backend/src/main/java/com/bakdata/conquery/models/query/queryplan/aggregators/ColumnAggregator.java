package com.bakdata.conquery.models.query.queryplan.aggregators;

import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;

/**
 * An Aggregator aggregating over at least one {@link Column}.
 *
 * For example counting all events where a {@link com.bakdata.conquery.models.datasets.Column} has a value: {@link com.bakdata.conquery.models.query.queryplan.aggregators.specific.CountAggregator}.
 * @param <T>
 */
public abstract class ColumnAggregator<T> implements Aggregator<T> {

	@Override
	public void collectRequiredTables(Set<TableId> out) {
		for (Column column : getRequiredColumns()) {
			out.add(column.getTable().getId());
		}
	}

	public abstract Column[] getRequiredColumns();

	@Override
	public abstract void aggregateEvent(Block block, int event);

	@Override
	public abstract ColumnAggregator<T> clone();

	@Override
	public String toString(){
		return getClass().getSimpleName();
	}
}
