package com.bakdata.conquery.models.events;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.serializer.Int2IntArrayMapDeserializer;
import com.bakdata.conquery.io.jackson.serializer.Int2IntMapSerializer;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.stores.root.BooleanStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.DateRangeStore;
import com.bakdata.conquery.models.events.stores.root.DateStore;
import com.bakdata.conquery.models.events.stores.root.DecimalStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.MoneyStore;
import com.bakdata.conquery.models.events.stores.root.RealStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * Contains data from possibly multiple entities, loaded in a single import.
 */
@Slf4j
@FieldNameConstants
@Getter
@Setter
@ToString(of = {"numberOfEvents", "stores"}, callSuper = true)
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
public class Bucket extends IdentifiableImpl<BucketId> implements NamespacedIdentifiable<BucketId> {

	@Min(0)
	private final int bucket;
	@Min(0)
	private final int numberOfEvents;
	private final ColumnStore[] stores;

	//TODO migrate these back to arrays again, like CBlock#getEntityIndex (and unify those)
	/**
	 * start of each Entity in {@code stores}.
	 */
	@JsonSerialize(using = Int2IntMapSerializer.class)
	@JsonDeserialize(using = Int2IntArrayMapDeserializer.class)
	private final Int2IntMap start;
	/**
	 * Number of events per Entity in {@code stores}.
	 */
	@JsonSerialize(using = Int2IntMapSerializer.class)
	@JsonDeserialize(using = Int2IntArrayMapDeserializer.class)
	private final Int2IntMap length;



	@NsIdRef
	private final Import imp;

	@JsonIgnore
	public Table getTable() {
		return imp.getTable();
	}

	@Override
	public BucketId createId() {
		return new BucketId(imp.getId(), bucket);
	}

	/**
	 * Iterate entities
	 */
	public Collection<Integer> entities() {
		return start.keySet();
	}

	public boolean containsEntity(int entity) {
		return start.containsKey(entity);
	}

	public Iterable<BucketEntry> entries() {
		return () -> start.keySet()
						  .stream()
						  .flatMap(entity -> IntStream.range(getEntityStart(entity), getEntityEnd(entity))
													  .mapToObj(e -> new BucketEntry(entity, e))
						  )
						  .iterator();
	}

	public int getEntityStart(int entityId) {
		return start.get(entityId);
	}

	public int getEntityEnd(int entityId) {
		return start.get(entityId) + length.get(entityId);
	}

	public final boolean has(int event, Column column) {
		return getStore(column).has(event);
	}

	public int getString(int event, @NotNull Column column) {
		return ((StringStore) getStore(column)).getString(event);
	}

	public ColumnStore getStore(@NotNull Column column) {
		return stores[column.getPosition()];
	}

	public long getInteger(int event, @NotNull Column column) {
		return ((IntegerStore) getStore(column)).getInteger(event);
	}

	public boolean getBoolean(int event, @NotNull Column column) {
		return ((BooleanStore) getStore(column)).getBoolean(event);
	}

	public double getReal(int event, @NotNull Column column) {
		return ((RealStore) getStore(column)).getReal(event);
	}

	public BigDecimal getDecimal(int event, @NotNull Column column) {
		return ((DecimalStore) getStore(column)).getDecimal(event);
	}

	public long getMoney(int event, @NotNull Column column) {
		return ((MoneyStore) getStore(column)).getMoney(event);
	}

	public int getDate(int event, @NotNull Column column) {
		return ((DateStore) getStore(column)).getDate(event);
	}

	public CDateRange getDateRange(int event, Column column) {
		return ((DateRangeStore) getStore(column)).getDateRange(event);
	}

	public boolean eventIsContainedIn(int event, Column column, CDateSet dateRanges) {
		return dateRanges.intersects(getAsDateRange(event, column));
	}

	public CDateRange getAsDateRange(int event, Column column) {
		switch (column.getType()) {
			case DATE:
				return CDateRange.exactly(((DateStore) getStore(column)).getDate(event));
			case DATE_RANGE:
				return getDateRange(event, column);
			default:
				throw new IllegalStateException("Column is not of DateCompatible type.");
		}
	}

	public Object createScriptValue(int event, @NotNull Column column) {
		return getStore(column).createScriptValue(event);
	}

	public Map<String, Object> calculateMap(int event) {
		Map<String, Object> out = new HashMap<>(stores.length);

		for (int i = 0; i < stores.length; i++) {
			ColumnStore store = stores[i];
			if (!store.has(event)) {
				continue;
			}
			out.put(getTable().getColumns()[i].getName(), store.createScriptValue(event));
		}

		return out;
	}

	@JsonIgnore
	@Override
	public Dataset getDataset() {
		return getTable().getDataset();
	}
}
