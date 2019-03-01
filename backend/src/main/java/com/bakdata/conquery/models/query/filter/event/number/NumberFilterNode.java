package com.bakdata.conquery.models.query.filter.event.number;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.filter.event.AbstractEventFilterNode;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Abstract class testing where events are included if they are within a given {@link IRange}.
 *
 * See: {@link FilterValue}.
 *
 * @param <RANGE> The type of the specified range.
 */
public abstract class NumberFilterNode<RANGE extends IRange<?, ?>> extends AbstractEventFilterNode<FilterValue<RANGE>, SingleColumnFilter<FilterValue<RANGE>>> {

	@Getter(AccessLevel.PROTECTED)
	private RANGE range;

	public NumberFilterNode(SingleColumnFilter filter, FilterValue<RANGE> filterValue) {
		super(filter, filterValue);
		range = filterValue.getValue();
	}

	@Override
	public final boolean checkEvent(Block block, int event) {
		if (!block.has(event, filter.getColumn())) {
			return false;
		}

		return contains(block, event);
	}

	public abstract boolean contains(Block block, int event);

}
