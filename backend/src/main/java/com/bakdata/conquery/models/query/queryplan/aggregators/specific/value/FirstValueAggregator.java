package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

/**
 * Returns the earliest value of the column with regard to its validity date column.
 * @param <VALUE>
 */
public class FirstValueAggregator<VALUE> extends SingleColumnAggregator<VALUE> {

	private Object value;
	private Block block;

	private int date = Integer.MAX_VALUE;

	private Column validityDateColumn;

	public FirstValueAggregator(Column column) {
		super(column);
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		validityDateColumn = ctx.getValidityDateColumn();
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (!block.has(event, getColumn()) || ! block.has(event, validityDateColumn)) {
			return;
		}

		int next = block.getAsDateRange(event, validityDateColumn).getMinValue();

		if (next < date) {
			date = next;
			value = block.getAsObject(event, getColumn());
			this.block = block;
		}
	}

	@Override
	public VALUE getAggregationResult() {
		if (block == null) {
			return null;
		}

		return (VALUE) getColumn().getTypeFor(block).createPrintValue(value);
	}

	@Override
	public FirstValueAggregator clone() {
		return new FirstValueAggregator(getColumn());
	}
}
