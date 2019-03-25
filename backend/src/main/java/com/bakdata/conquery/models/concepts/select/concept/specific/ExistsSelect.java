package com.bakdata.conquery.models.concepts.select.concept.specific;

import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.concepts.select.concept.UniversalSelect;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.ExistsAggregator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;

@CPSType(id = "EXISTS", base = Select.class)
public class ExistsSelect extends UniversalSelect {

	@JsonIgnore @Getter(lazy=true)
	private final Set<TableId> requiredTables = collectRequiredTables();
	
	@Override
	public ExistsAggregator createAggregator() {
		return new ExistsAggregator(getRequiredTables());
	}

	private Set<TableId> collectRequiredTables() {
		return this.getHolder().findConcept().getConnectors().stream().map(Connector::getTable).map(Table::getId).collect(Collectors.toSet());
	}
}
