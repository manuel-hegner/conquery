package com.bakdata.conquery.models.query.filter;

import java.util.Collection;

import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;

/**
 * Filter testing if an {@link Aggregator<Collection>}s aggregation result is not empty.
 */
public class CollectionNotEmptyFilterNode<FILTER_VALUE extends FilterValue<?>> extends AggregationResultFilterNode<Aggregator<Collection<?>>, FILTER_VALUE, Filter<FILTER_VALUE>> {

	public CollectionNotEmptyFilterNode(Filter<FILTER_VALUE> multiSelectFilter, FILTER_VALUE filterValue, Aggregator<Collection<?>> aggregator) {
		super(aggregator, multiSelectFilter, filterValue);
	}

	@Override
	public CollectionNotEmptyFilterNode<FILTER_VALUE> clone(QueryPlan plan, QueryPlan clone) {
		return new CollectionNotEmptyFilterNode<>(filter, filterValue, getAggregator().clone());
	}

	@Override
	public boolean isContained() {
		return !getAggregator().getAggregationResult().isEmpty();
	}
}
