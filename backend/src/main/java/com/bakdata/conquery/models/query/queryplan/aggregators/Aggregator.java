package com.bakdata.conquery.models.query.queryplan.aggregators;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.EventIterating;

/**
 * Class operating on {@link Block}s, aggregating over their events.
 *
 * @param <T> The return type of the Aggregation.
 */
public interface Aggregator<T> extends Cloneable, EventIterating {

	/**
	 * Accept a new event and update internal state.
	 * @param block the Block of events
	 * @param event the specific event to be processed.
	 */
	void aggregateEvent(Block block, int event);

	/**
	 * Calculates the result of the Aggregation.
	 *
	 * Note: This should not change the state of the aggregation.
	 */
	T getAggregationResult();

	/**
	 * Create a clone of this aggregator completely reset.
	 * @return A new aggregator of the same type.
	 */
	Aggregator<T> clone();
}
