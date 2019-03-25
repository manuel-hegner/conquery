package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.google.common.collect.PeekingIterator;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor @Getter @EqualsAndHashCode(callSuper=false)
public abstract class SelectId extends AId<Select> implements NamespacedId {

	private final String select;

	@Override
	public void collectComponents(List<Object> components) {
		components.add(select);
	}

	public static enum Parser implements IId.Parser<SelectId> {
		INSTANCE;
		
		@Override
		public SelectId parse(PeekingIterator<String> parts) {
			ConnectorId parent = ConnectorId.Parser.INSTANCE.parse(parts);
			if(!parts.hasNext()) {
				return new ConceptSelectId(parent.getConcept(), parent.getConnector());
			}
			else {
				return new ConnectorSelectId(parent, parts.next());
			}
		}
	}

	public String toStringWithoutDataset() {
		return StringUtils.removeStart(toString(), getDataset().toString()+IId.JOIN_CHAR);
	}
}