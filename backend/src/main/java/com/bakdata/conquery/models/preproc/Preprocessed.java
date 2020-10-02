package com.bakdata.conquery.models.preproc;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.io.HCFile;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.ImportColumn;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.types.parser.Transformer;
import com.bakdata.conquery.models.types.parser.specific.DateParser;
import com.bakdata.conquery.models.types.parser.specific.DateRangeParser;
import com.bakdata.conquery.models.types.parser.specific.string.StringParser;
import com.esotericsoftware.kryo.io.Output;
import io.dropwizard.util.Size;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class Preprocessed {
	
	private final InputFile file;
	private final String name;
	private final PPColumn primaryColumn;
	private final PPColumn[] columns;
	private final TableImportDescriptor descriptor;
	private long rows = 0;
	private CDateRange eventRange;
	private long writtenGroups = 0;
	private Int2ObjectMap<List<Object[]>> entries = new Int2ObjectAVLTreeMap<>();
	
	private final Output buffer = new Output((int) Size.megabytes(50).toBytes());

	public Preprocessed(TableImportDescriptor descriptor) throws IOException {
		this.file = descriptor.getInputFile();
		this.name = descriptor.getName();
		this.descriptor = descriptor;
		
		TableInputDescriptor input = descriptor.getInputs()[0];
		columns = new PPColumn[input.getWidth()];
		
		
		primaryColumn = new PPColumn(input.getPrimary().getColumnDescription().getName());
		primaryColumn.setParser(input.getPrimary().getColumnDescription().getType().createParser());
		
		if(!(primaryColumn.getParser() instanceof StringParser)) {
			throw new IllegalStateException("The primary column must be an ENTITY_ID or STRING column");
		}

		for(int i=0;i<input.getWidth();i++) {
			ColumnDescription columnDescription = input.getColumnDescription(i);
			columns[i] = new PPColumn(columnDescription.getName());
			columns[i].setParser(columnDescription.getType().createParser());
		}
	}

	public void write(HCFile outFile) throws IOException {
		if(!outFile.isWrite()){
			throw new IllegalArgumentException("outfile was opened in read-only mode.");
		}

		// Write content to file
		Import imp = Import.createForPreprocessing(descriptor.getTable(), descriptor.getName(), columns);

		try (Output out = new Output(outFile.writeContent())) {
			for(int entityId = 0; entityId < entries.size(); entityId++) {
				List<Object[]> events = entries.getOrDefault(entityId, Collections.emptyList());

				if(!events.isEmpty()) {
					writeRowsToFile(out, imp, entityId, events);
				}
			}
		}

		// Then write headers.
		try (OutputStream out = outFile.writeHeader()) {
			int hash = descriptor.calculateValidityHash();

			PreprocessedHeader header = new PreprocessedHeader(
					descriptor.getName(),
					descriptor.getTable(),
					imp.getSuffix(),
					rows,
					writtenGroups,
					eventRange,
					primaryColumn,
					columns,
					hash
			);

			try {
				Jackson.BINARY_MAPPER.writeValue(out, header);
				primaryColumn.getType().writeHeader(out);
				for(PPColumn col:columns) {
					col.getType().writeHeader(out);
				}
				out.flush();
			} catch (Exception e) {
				throw new RuntimeException("Failed to serialize header "+header, e);
			}
		}
	}

	public synchronized int addPrimary(int primary) {
		primaryColumn.getParser().addLine(primary);
		return primary;
	}

	public synchronized void addRow(int primaryId, PPColumn[] columns, Object[] outRow) {
		entries.computeIfAbsent(primaryId, (id) -> new ArrayList<>())
			   .add(outRow);

		for (int i = 0; i < columns.length; i++) {
			log.trace("Registering `{}` for Column[{}]", outRow[i], columns[i].getName());
			columns[i].getParser().addLine(outRow[i]);
		}

		//update stats
		rows++;
		for (int i = 0; i < columns.length; i++) {
			if (outRow[i] == null) {
				continue;
			}

			if (columns[i].getParser() instanceof DateParser) {
				extendEventRange(CDateRange.exactly((Integer) outRow[i]));
			}
			else if (columns[i].getParser() instanceof DateRangeParser) {
				extendEventRange((CDateRange) outRow[i]);
			}
		}
		
	}

	/**
	 * Collect date span of all data.
	 */
	private void extendEventRange(CDateRange range) {
		if(eventRange == null) {
			eventRange = range;
		}
		else if(range != null) {
			eventRange = eventRange.spanClosed(range);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void writeRowsToFile(Output out, Import imp, int entityId, List<Object[]> rows) throws IOException {
		//transform values to their current subType
		//we can't map the primary column since we do a lot of work which would destroy any compression anyway
		//entityId = (Integer)primaryColumn.getType().transformFromMajorType(primaryColumn.getOriginalType(), Integer.valueOf(entityId));

		for(ImportColumn importColumn : imp.getColumns()) {

			PPColumn column = columns[importColumn.getPosition()];
			Transformer transformer = column.getTransformer();

			for(Object[] row : rows) {
				if (row[importColumn.getPosition()] == null) {
					continue;
				}

				row[importColumn.getPosition()] = transformer.transform(row[importColumn.getPosition()]);
			}
			transformer.finishTransform();
		}
		
		Bucket bucket = imp.getBlockFactory().create(imp, rows);
		
		out.writeInt(entityId, true);
		bucket.writeContent(buffer);
		out.writeInt(buffer.position(), true);
		out.writeBytes(buffer.getBuffer(), 0, buffer.position());
		
		buffer.reset();
		writtenGroups++;
	}

}
