package com.bakdata.conquery.models.query.results;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
@CPSType(id="MULTILINE_CONTAINED", base=EntityResult.class)
public class MultilineContainedEntityResult implements ContainedEntityResult {

	//this is needed because of https://github.com/FasterXML/jackson-databind/issues/2024
	public MultilineContainedEntityResult(int entityId, List<Object[]> values) {
		this.entityId = entityId;
		this.values = Objects.requireNonNullElse(values, Collections.emptyList());
	}

	@Min(0)
	private final int entityId;
	@NotNull
	private final List<Object[]> values;

	@Override
	public Stream<Object[]> streamValues() {
		return values.stream();
	}
}
