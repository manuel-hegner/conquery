package com.bakdata.conquery.models.concepts.select.connector.specific;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.connector.SingleColumnSelect;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.MultiSelectAggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SelectAggregator;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

@CPSType(id = "COUNT_OCCURENCES", base = Select.class)
public class CountOccurencesSelect extends SingleColumnSelect {

	@Getter
	@Setter
	@NotNull
	private String[] selection;

	@JsonCreator
	public CountOccurencesSelect(@NsIdRef Column column, String[] selection) {
		super(column);
		this.selection = selection;
	}

	@Override
	public Aggregator<?> createAggregator() {
		if (selection.length == 1) {
			return new SelectAggregator(getColumn(), selection[0]);
		}

		return new MultiSelectAggregator(getColumn(), selection);
	}
}
