package com.bakdata.conquery.models.query.filter;

import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

/**
 * Entitiy is included, when aggregation result is above zero.
 */
public class ValueAboveZeroFilterNode<FILTER_VALUE extends FilterValue<?>, FILTER extends Filter<FILTER_VALUE>> extends AggregationResultFilterNode<Aggregator<Long>, FILTER_VALUE, FILTER> {


	public ValueAboveZeroFilterNode(FILTER filter, FILTER_VALUE filterValue, Aggregator<Long> aggregator) {
		super(aggregator, filter, filterValue);
	}

	@Override
	public FilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new ValueAboveZeroFilterNode(filter, filterValue, getAggregator().clone());
	}

	@Override
	public boolean isContained() {
		return getAggregator().getAggregationResult() > 0;
	}
}
