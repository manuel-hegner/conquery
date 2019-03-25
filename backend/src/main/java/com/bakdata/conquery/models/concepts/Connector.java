package com.bakdata.conquery.models.concepts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.validation.ConstraintValidatorContext;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.NotImplementedException;

import com.bakdata.conquery.io.jackson.serializer.NsIdReferenceDeserializer;
import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.concepts.filters.specific.ValidityDateSelectionFilter;
import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.exceptions.validators.DetailedValid;
import com.bakdata.conquery.models.exceptions.validators.DetailedValid.ValidationMethod2;
import com.bakdata.conquery.models.identifiable.IdMap;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset.Entry;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * A connector represents the connection between a column and a concept.
 */
@Getter @Setter @DetailedValid
public abstract class Connector extends Labeled<ConnectorId> implements Serializable, SelectHolder<Select> {

	private static final long serialVersionUID = 1L;

	@NotNull
	private List<ValidityDate> validityDates = new ArrayList<>();
	@JsonManagedReference
	private ValidityDateSelectionFilter dateSelectionFilter;

	@JsonBackReference
	private Concept<?> concept;

	@JsonIgnore @Getter(AccessLevel.NONE)
	private transient IdMap<FilterId, Filter<?>> allFiltersMap;

	@NotNull @Getter @Setter @JsonManagedReference
	private List<Select> selects = new ArrayList<>();

	@Override
	public Concept<?> findConcept() {
		return concept;
	}
	
	@JsonDeserialize(contentUsing = NsIdReferenceDeserializer.class)
	public void setSelectableDates(List<Column> cols) {
		this.setValidityDates(
				cols
				.stream()
				.map(c -> {
					ValidityDate sd = new ValidityDate();
					sd.setColumn(c);
					sd.setName(c.getName());
					return sd;
				})
				.collect(Collectors.toList())
		);
	}

	@Override
	public ConnectorId createId() {
		return new ConnectorId(concept.getId(), getName());
	}

	public abstract Table getTable();

	@JsonIgnore
	public Column getSelectableDate(String name) {
		return validityDates
						.stream()
						.filter(vd -> vd.getName().equals(name))
						.map(ValidityDate::getColumn)
						.findAny()
						.orElseThrow(() -> new IllegalArgumentException("Unable to find date " + name));
	}

	public CDateRange extractValidityDates(Block block, int event) {
		throw new NotImplementedException("extractValidityDates");
		/*validityDates.stream()
				.map(ValidityDate::getColumn)
				.map(record::get)
				.flatMap(DateHelper::streamDatesOfDateObject)
				.map(Range::singleton)
				.reduce(Range::span)
				.orElse(null);
				*/
		//see #157
	}

	@ValidationMethod2
	public boolean validateFilters(ConstraintValidatorContext context) {
		boolean passed = true;

		for(Filter<?> f:collectAllFilters()) {
			for(Column c:f.getRequiredColumns()) {
				if (c != null && c.getTable() != getTable()) {
					context
						.buildConstraintViolationWithTemplate("The filter "+f.getId()+" must be of the same table "+this.getTable().getId()+" as its connector "+this.getId())
						.addConstraintViolation();
					passed = false;
				}
			}
		}

		for(Entry<String> e:collectAllFilters().stream().map(Filter::getName).collect(ImmutableMultiset.toImmutableMultiset()).entrySet()) {
			if(e.getCount()>1) {
				passed = false;
				context
					.buildConstraintViolationWithTemplate("The filter name "+e.getElement()+" is used "+e.getCount()+" time in "+this.getId())
					.addConstraintViolation();
			}
		}

		return passed;
	}

	@ValidationMethod2
	public boolean validateSelectableDates(ConstraintValidatorContext context) {
		if (validityDates == null) {
			return true;
		}
		boolean passed = true;
		for (ValidityDate sd : validityDates) {
			Column col = sd.getColumn();
			if (!col.getType().isDateCompatible()) {
				passed = false;
				context
					.buildConstraintViolationWithTemplate("The validity date column "+col.getId()+" of the connector "+this.getId()+" is not of type DATE or DATERANGE")
					.addConstraintViolation();
			}
			if (!col.getTable().equals(getTable())) {
				passed = false;
				context
					.buildConstraintViolationWithTemplate("The validity date column "+col.getId()+" is not of the same table "+this.getTable().getId()+" as its connector "+this.getId())
					.addConstraintViolation();
			}
		}
		return passed;
	}

	public Filter<?> getFilterByName(String name) {
		return collectAllFilters().stream().filter(f->name.equals(f.getName())).findAny().orElseThrow(() -> new IllegalArgumentException("Unable to find filter " + name));
	}

	@JsonIgnore
	public abstract List<Filter<?>> collectAllFilters();

	public <T extends Filter> T getFilter(FilterId id) {
		if(allFiltersMap==null) {
			allFiltersMap = new IdMap<>(collectAllFilters());
		}
		return (T)allFiltersMap.getOrFail(id);
	}

	public Column getValidityDateColumn(String name) {
		for(ValidityDate vDate:validityDates) {
			if(vDate.getName().equals(name))
				return vDate.getColumn();
		}
		throw new NoSuchElementException("There is no validityDate called '"+name+"' in "+this);
	}

	public synchronized void addImport(Import imp) {
		for(Filter<?> f : collectAllFilters()) {
			f.addImport(imp);
		}
	}

	//public abstract EventProcessingResult processEvent(Event r) throws ConceptConfigurationException;
}
