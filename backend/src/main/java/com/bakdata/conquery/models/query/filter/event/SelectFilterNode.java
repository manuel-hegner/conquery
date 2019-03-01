package com.bakdata.conquery.models.query.filter.event;

import com.bakdata.conquery.models.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.types.specific.IStringType;

/**
 * Event is included if column is equal to a selected string.
 */
public class SelectFilterNode extends AbstractEventFilterNode<FilterValue.CQSelectFilter, SelectFilter> {
	private final String selected;
	private int selectedId = -1;

	public SelectFilterNode(SelectFilter filter, FilterValue.CQSelectFilter filterValue) {
		super(filter, filterValue);
		this.selected = filterValue.getValue();
	}


	@Override
	public void nextBlock(Block block) {
		//you can then also skip the block if the id is -1
		selectedId = ((IStringType) filter.getColumn().getTypeFor(block)).getStringId(selected);
	}

	@Override
	public SelectFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new SelectFilterNode(filter, filterValue);
	}

	@Override
	public boolean checkEvent(Block block, int event) {
		if (selectedId == -1 || !block.has(event, filter.getColumn())) {
			return false;
		}

		int value = block.getString(event, filter.getColumn());

		return value == selectedId;

	}


}
