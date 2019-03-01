package com.bakdata.conquery.models.query.filter.event;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.concepts.filters.specific.DateDistanceFilter;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

/**
 * Event is included as long as the Column is within a specified distance to the {@code dateRestriction}s beginning.
 */
public class DateDistanceFilterNode extends AbstractEventFilterNode<FilterValue.CQIntegerRangeFilter, DateDistanceFilter> {

	private CDateSet dateRestriction;

	public DateDistanceFilterNode(DateDistanceFilter dateDistanceFilter, FilterValue.CQIntegerRangeFilter filterValue) {
		super(dateDistanceFilter, filterValue);
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		dateRestriction = ctx.getDateRestriction();
	}

	@Override
	public DateDistanceFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new DateDistanceFilterNode(filter, filterValue);
	}

	@Override
	public boolean checkEvent(Block block, int event) {
		if (!block.has(event, filter.getColumn())) {
			return false;
		}

		int date = block.getDate(event, filter.getColumn());

		if (date <= dateRestriction.getMinValue()) {
			return filterValue.getValue().contains(dateRestriction.getMinValue() - date);
		}
		else {
			return filterValue.getValue().contains(dateRestriction.getMaxValue() - date);
		}
	}
}