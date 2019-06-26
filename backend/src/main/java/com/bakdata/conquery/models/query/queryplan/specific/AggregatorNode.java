package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Set;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor @Getter @ToString(of = "aggregator")
public class AggregatorNode<T> extends QPNode  {

	private final Aggregator<T> aggregator;
	private boolean triggered = false;
	
	@Override
	public void nextEvent(Bucket bucket, int event) {
		triggered = true;
		aggregator.aggregateEvent(bucket, event);
	}

	@Override
	public boolean isContained() {
		return triggered;
	}
	
	@Override
	public AggregatorNode<T> doClone(CloneContext ctx) {
		return new AggregatorNode<>(aggregator.clone(ctx));
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		aggregator.collectRequiredTables(requiredTables);
	}
	
	@Override
	public void nextBlock(Bucket bucket) {
		aggregator.nextBlock(bucket);
	}
	
	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		aggregator.nextTable(ctx, currentTable);
	}
	
	@Override
	public boolean isOfInterest(Bucket bucket) {
		return true;
	}
}
