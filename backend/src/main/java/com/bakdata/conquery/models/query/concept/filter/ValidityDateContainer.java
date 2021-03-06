package com.bakdata.conquery.models.query.concept.filter;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.concepts.ValidityDate;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(onConstructor_ = @JsonCreator )
public class ValidityDateContainer {
	@NsIdRef
	private final ValidityDate value;
}
