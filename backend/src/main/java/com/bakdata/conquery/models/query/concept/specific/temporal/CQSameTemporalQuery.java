package com.bakdata.conquery.models.query.concept.specific.temporal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.SameTemporalMatcher;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalQueryNode;

/**
 * Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} at the same time as the events of {@code index}. And the time where this has happened.
 */
@CPSType(id = "SAME", base = CQElement.class)
public class CQSameTemporalQuery extends CQAbstractTemporalQuery {

	public CQSameTemporalQuery(CQElement index, CQElement preceding, TemporalSampler sampler) {
		super(index, preceding, sampler);
	}

	@Override
	public QPNode createQueryPlan(QueryPlanContext registry, QueryPlan plan) {
		QueryPlan indexPlan = QueryPlan.create();
		indexPlan.setRoot(index.createQueryPlan(registry, plan));

		QueryPlan precedingPlan = QueryPlan.create();
		precedingPlan.setRoot(preceding.createQueryPlan(registry, plan));

		return new TemporalQueryNode(indexPlan, precedingPlan, getSampler(), new SameTemporalMatcher(), plan.getIncluded()) {
		};
	}
	
	@Override
	public CQSameTemporalQuery resolve(QueryResolveContext context) {
		return new CQSameTemporalQuery(index.resolve(context), preceding.resolve(context), sampler);
	}
}