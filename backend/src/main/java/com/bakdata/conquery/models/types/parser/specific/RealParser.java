package com.bakdata.conquery.models.types.parser.specific;

import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.stores.base.DoubleStore;
import com.bakdata.conquery.models.events.stores.base.FloatStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Decision;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.specific.RealTypeDouble;
import com.bakdata.conquery.models.types.specific.RealTypeFloat;
import com.bakdata.conquery.util.NumberParsing;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@ToString(callSuper = true)
public class RealParser extends Parser<Double> {

	private final double requiredPrecision;

	private double floatULP = Float.NEGATIVE_INFINITY;

	public RealParser(ParserConfig config) {
		requiredPrecision = config.getMinPrecision();
	}

	@Override
	protected Double parseValue(String value) throws ParsingException {
		return NumberParsing.parseDouble(value);
	}

	/**
	 * Collect ULP of all values
	 * @see Math#ulp(float) for an explanation.
	 */
	@Override
	protected void registerValue(Double v) {
		floatULP = Math.max(floatULP, Math.ulp(v.floatValue()));
	}

	/**
	 * If values are within a margin of precision, we store them as floats.
	 */
	@Override
	protected Decision<? extends CType<Double, ?>> decideType() {
		log.debug("Max ULP = {}", floatULP);

		if(floatULP < requiredPrecision){
			return new Decision<>(
					new RealTypeFloat(FloatStore.create(getLines()))
			);
		}

		return new Decision<>(new RealTypeDouble(DoubleStore.create(getLines())));
	}
}
