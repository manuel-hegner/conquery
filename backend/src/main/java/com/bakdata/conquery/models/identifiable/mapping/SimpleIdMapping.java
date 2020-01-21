package com.bakdata.conquery.models.identifiable.mapping;

import com.bakdata.conquery.io.cps.CPSType;
import org.apache.commons.lang3.ArrayUtils;

@CPSType(base= IdMappingConfig.class, id="SIMPLE")
public class SimpleIdMapping extends IdMappingConfig {

	@Override
	public IdMappingAccessor[] getIdAccessors() {
		return new IdMappingAccessor[]{new DefaultIdMappingAccessor(this, new int[]{0}) {
			@Override
			public CsvEntityId getFallbackCsvId(String[] reorderedCsvLine) {
				return new CsvEntityId(reorderedCsvLine[0]);
			}

			@Override
			public int findIndexfromMappingHeader(String csvHeaderField) {
				return ArrayUtils.indexOf(getHeader(), csvHeaderField);
			}
		}};
	}

	@Override
	public String[] getPrintIdFields() {
		return new String[]{"result"};
	}

	@Override
	public String[] getHeader() {
		return new String[]{"id", "result"};
	}

}
