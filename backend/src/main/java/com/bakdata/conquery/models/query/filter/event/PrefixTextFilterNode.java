package com.bakdata.conquery.models.query.filter.event;

import com.bakdata.conquery.models.concepts.filters.specific.PrefixTextFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

/**
 * Event is included when value in column starts with a specified prefix.
 */
public class PrefixTextFilterNode extends AbstractEventFilterNode<FilterValue.CQStringFilter, PrefixTextFilter> {

	private final String prefix;

	public PrefixTextFilterNode(PrefixTextFilter filter, FilterValue.CQStringFilter filterValue) {
		super(filter, filterValue);
		this.prefix = filterValue.getValue();
	}


	@Override
	public FilterNode<?, ?> clone(QueryPlan plan, QueryPlan clone) {
		return new PrefixTextFilterNode(filter, filterValue);
	}

	@Override
	public boolean checkEvent(Block block, int event) {
		if (!block.has(event, filter.getColumn())) {
			return false;
		}

		int stringToken = block.getString(event, filter.getColumn());

		String value = (String) filter.getColumn().getTypeFor(block).createScriptValue(stringToken);

		//if performance is a problem we could find the prefix once in the dictionary and then only check the values
		return value.startsWith(prefix);
	}

}
