package com.bakdata.conquery.models.query.results;

import java.util.stream.Stream;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter @Setter @RequiredArgsConstructor(onConstructor_=@JsonCreator)
@CPSType(id="CONTAINED", base=EntityResult.class)
public class SinglelineContainedEntityResult implements ContainedEntityResult {

	private final int entityId;
	private final Object[] values;

	@Override
	public Stream<Object[]> streamValues() {
		return Stream.ofNullable(values);
	}
}
