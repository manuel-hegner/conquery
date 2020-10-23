package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.CTypeVarInt;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@CPSType(base = CType.class, id = "MONEY_VARINT")
@Getter
@Setter
public class MoneyTypeVarInt extends CTypeVarInt<Long> {

	@JsonCreator
	public MoneyTypeVarInt(VarIntType numberType) {
		super(MajorTypeId.MONEY, numberType);
	}


	@Override
	public Long createScriptValue(Number value) {
		return (long) numberType.createScriptValue(value.longValue());
	}

	@Override
	public Object createPrintValue(Number value) {
		return createScriptValue(value);
	}

	@Override
	public ColumnStore<Number> select(int[] starts, int[] length) {
		return null;
	}

	@Override
	public void set(int event, Number value) {

	}

	@Override
	public Number get(int event) {
		return null;
	}

	@Override
	public boolean has(int event) {
		return false;
	}
}
