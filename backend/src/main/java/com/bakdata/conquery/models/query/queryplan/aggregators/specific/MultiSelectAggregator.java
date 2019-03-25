package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.types.specific.IStringType;


public class MultiSelectAggregator extends SingleColumnAggregator<Map<String, Integer>> {

	private final String[] selection;
	private final int[] hits;
	private int[] selectedValues;

	public MultiSelectAggregator(Column column, String[] selection) {
		super(column);
		this.selection = selection;
		this.selectedValues = new int[selection.length];
		this.hits = new int[selection.length];
	}

	@Override
	public void nextBlock(Block block) {
		IStringType type = (IStringType) getColumn().getTypeFor(block);

		for (int index = 0; index < selection.length; index++) {
			selectedValues[index] = type.getStringId(selection[index]);
		}
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (!block.has(event, getColumn())) {
			return;
		}

		int stringToken = block.getString(event, getColumn());

		for (int index = 0; index < selectedValues.length; index++) {
			if (selectedValues[index] == stringToken) {
				hits[index]++;
				return;
			}
		}
	}

	@Override
	public Map<String, Integer> getAggregationResult() {
		Map<String, Integer> out = new HashMap<>();

		for (int i = 0; i < hits.length; i++) {
			int hit = hits[i];
			if (hit > 0) {
				out.merge(selection[i], hit, (a,b)->a+b);
			}
		}

		return out;
	}

	@Override
	public MultiSelectAggregator doClone(CloneContext ctx) {
		return new MultiSelectAggregator(getColumn(), selection);
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}
}
