package com.bakdata.conquery.util.io;

import java.io.IOException;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.types.specific.AStringType;

public class Cloner {
	public static ConqueryConfig clone(ConqueryConfig config) {
		try {
			ConqueryConfig clone = Jackson.BINARY_MAPPER.readValue(
				Jackson.BINARY_MAPPER.writeValueAsBytes(config),
				ConqueryConfig.class
			);
			clone.setLoggingFactory(config.getLoggingFactory());
			return clone;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to clone a conquery config "+config, e);
		}
	}
	
	public static AStringType<?> clone(AStringType<?> type) {
		try {
			AStringType<?> clone = Jackson.BINARY_MAPPER.readValue(
				Jackson.BINARY_MAPPER.writeValueAsBytes(type),
				AStringType.class
			);
			return clone;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to clone a type "+type, e);
		}
	}
}