package com.bakdata.conquery.models.types.parser.specific;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.parser.Parser;
import com.bakdata.conquery.models.types.specific.DateRangeTypeDateRange;
import com.bakdata.conquery.models.types.specific.DateRangeTypeQuarter;
import com.bakdata.conquery.util.DateFormats;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@ToString(callSuper = true)
public class DateRangeParser extends Parser<CDateRange> {

	private boolean onlyQuarters = true;
	private int maxValue = Integer.MIN_VALUE;
	private int minValue = Integer.MAX_VALUE;
	private boolean anyOpen;

	public DateRangeParser(ParserConfig config) {

	}

	@Override
	protected CDateRange parseValue(@Nonnull String value) throws ParsingException {
		return DateRangeParser.parseISORange(value);
	}

	public static CDateRange parseISORange(String value) throws ParsingException {
		if (value == null) {
			return null;
		}
		String[] parts = StringUtils.split(value, '/');
		if (parts.length != 2) {
			throw ParsingException.of(value, "daterange");
		}

		return CDateRange.of(
				DateFormats.parseToLocalDate(parts[0]),
				DateFormats.parseToLocalDate(parts[1])
		);
	}

	@Override
	protected void registerValue(CDateRange v) {
		onlyQuarters = onlyQuarters && v.isSingleQuarter();

		anyOpen = anyOpen || v.isOpen();

		if (v.hasUpperBound()) {
			maxValue = Math.max(maxValue, v.getMaxValue());
		}
		if (v.hasLowerBound()) {
			minValue = Math.min(minValue, v.getMinValue());
		}
	}

	@Override
	protected CType<CDateRange> decideType() {

		// Quarters cannot encode open ranges.
		if (!anyOpen && onlyQuarters) {
			final IntegerParser quarterParser = new IntegerParser();
			quarterParser.setLines(getLines());
			quarterParser.setMaxValue(maxValue);
			quarterParser.setMinValue(minValue);

			return new DateRangeTypeQuarter(quarterParser.decideType());
		}

		final IntegerParser parser = new IntegerParser(minValue, maxValue);
		parser.setNullLines(getNullLines());
		parser.setLines(getLines());

		return new DateRangeTypeDateRange(parser.decideType(), parser.decideType());
	}
}
