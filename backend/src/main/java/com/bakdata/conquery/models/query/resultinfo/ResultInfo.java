package com.bakdata.conquery.models.query.resultinfo;

import java.util.HashMap;
import java.util.Objects;

import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.ColumnDescriptor;
import com.bakdata.conquery.models.query.PrintSettings;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
@Slf4j
public abstract class ResultInfo {

	private final static int UNSET_PREFIX = -1;
	/**
	 * Calculated same name index for this column. If 0, the postfix can be omitted.
	 */
	@EqualsAndHashCode.Exclude
	private int postfix = UNSET_PREFIX;

	/**
	 * Is injected by the {@link ResultInfoCollector} for {@link SelectResultInfo}s.
	 */
	@EqualsAndHashCode.Exclude
	private HashMap<String, Integer> ocurrenceCounter;
	private ClassToInstanceMap<Object> appendices = MutableClassToInstanceMap.create();

	@NonNull
	@JsonIgnore
	public final String getUniqueName(PrintSettings settings) {
		@NonNull String label = Objects.requireNonNullElse(userColumnName(settings), defaultColumnName(settings));
		if (ocurrenceCounter == null) {
			return label;
		}
		// lookup if prefix is needed and computed it if necessary
		synchronized (ocurrenceCounter) {
			if (postfix == UNSET_PREFIX) {
				postfix = ocurrenceCounter.compute(label, (k, v) -> (v == null) ? 0 : ++v);
			}
		}
		String uniqueName = (postfix > 0) ? label + "_" + postfix : label;
		if (ocurrenceCounter.containsKey(uniqueName) && ocurrenceCounter.get(uniqueName) > 0) {
			log.warn(
				"Even with postfixing the result will contain column name duplicates. This might be caused by another column that is having a number postfix by default.");
		}
		return uniqueName;
	}

	public abstract String userColumnName(PrintSettings printSettings);

	/**
	 * Use default label schema which ignores user labels.
	 */
	public abstract String defaultColumnName(PrintSettings printSettings);

	@ToString.Include
	public abstract ResultType getType();

	public <T> void addAppendix(Class<T> cl, T obj) {
		appendices.putInstance(cl, obj);
	}

	public ColumnDescriptor asColumnDescriptor(PrintSettings settings) {
		return ColumnDescriptor.builder()
				.label(getUniqueName(settings))
				.defaultLabel(defaultColumnName(settings))
				.userConceptLabel(userColumnName(settings))
				.type(getType().typeInfo())
				.build();
	}
}
