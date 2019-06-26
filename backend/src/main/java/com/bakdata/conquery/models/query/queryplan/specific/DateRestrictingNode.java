package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Map;
import java.util.Objects;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.entity.EntityRow;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

public class DateRestrictingNode extends QPChainNode {

	private final CDateSet restriction;
	private Column validityDateColumn;
	private Map<BucketId, EntityRow> preCurrentRow = null;

	public DateRestrictingNode(CDateSet restriction, QPNode child) {
		super(child);
		this.restriction = restriction;
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		//if there was no date restriction we can just use the restriction CDateSet
		if(ctx.getDateRestriction().isAll()) {
			ctx = ctx.withDateRestriction(CDateSet.create(restriction));
		}
		else {
			CDateSet dateRestriction = CDateSet.create(ctx.getDateRestriction());
			dateRestriction.retainAll(restriction);
			ctx = ctx.withDateRestriction(dateRestriction);
		}
		super.nextTable(ctx, currentTable);


		validityDateColumn = Objects.requireNonNull(context.getValidityDateColumn());
		preCurrentRow = entity.getCBlockPreSelect(context.getConnector().getId());

		if (!validityDateColumn.getType().isDateCompatible()) {
			throw new IllegalStateException("The validityDateColumn " + validityDateColumn + " is not a DATE TYPE");
		}
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		EntityRow currentRow = Objects.requireNonNull(preCurrentRow.get(bucket.getId()));
		CBlock cBlock = currentRow.getCBlock();
		int localId = bucket.toLocal(entity.getId());
		if(cBlock.getMinDate()[localId] > cBlock.getMaxDate()[localId]) {
			return false;
		}
		CDateRange range = CDateRange.of(
			cBlock.getMinDate()[localId],
			cBlock.getMaxDate()[localId]
		);
		if(!restriction.intersects(range)) {
			return false;
		}
		return super.isOfInterest(bucket);
	}

	@Override
	public void nextEvent(Bucket bucket, int event) {
		if (bucket.eventIsContainedIn(event, validityDateColumn, restriction)) {
			getChild().nextEvent(bucket, event);
		}
	}

	@Override
	public boolean isContained() {
		return getChild().isContained();
	}
	
	@Override
	public QPNode doClone(CloneContext ctx) {
		return new DateRestrictingNode(restriction, getChild().clone(ctx));
	}
}
