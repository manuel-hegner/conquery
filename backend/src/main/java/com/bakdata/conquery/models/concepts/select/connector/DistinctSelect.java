package com.bakdata.conquery.models.concepts.select.connector;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.AllValuesAggregator;
import com.fasterxml.jackson.annotation.JsonCreator;

@CPSType(id = "DISTINCT", base = Select.class)
public class DistinctSelect extends SingleColumnSelect {

	@JsonCreator
	public DistinctSelect(@NsIdRef Column column) {
		super(column);
	}

	@Override
	public Aggregator<?> createAggregator() {
		return new AllValuesAggregator<>(getColumn());
	}
}
