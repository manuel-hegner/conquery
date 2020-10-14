package com.bakdata.conquery.models.preproc;

import java.util.StringJoiner;

import com.bakdata.conquery.models.datasets.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Header containing data about a Preprocessed Csv file. Generated by running {@link com.bakdata.conquery.commands.PreprocessorCommand}.
 *
 * @implSpec The Columns and their order must directly match the layout in the data.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Slf4j
public class PreprocessedHeader {
	/**
	 * The name/tag of an import.
	 */
	private String name;

	/**
	 * The specific table id to be loaded into.
	 */
	private String table;

	// TODO: 14.07.2020 FK: Is this actually used? It doesn't seem so.
	private String suffix;

	/**
	 * Number of rows in the Preprocessed file.
	 */
	private long rows;

	/**
	 * Name of the primary column.
	 *
	 * @implNote this is just used as a placeholder for dictionaries etc. about the Entities' primary columns.
	 */
	private PPColumn primaryColumn;

	/**
	 * The specific columns and their associated transformations+data.
	 */
	private PPColumn[] columns;

	/**
	 * A hash to check if any of the underlying files for generating this CQPP has changed.
	 */
	private int validityHash;


	/**
	 * Verify that the supplied table matches the preprocessed' data in shape.
	 */
	public void assertMatch(Table table) {
		StringJoiner errors = new StringJoiner("\n");

		if (!table.getPrimaryColumn().matches(getPrimaryColumn())) {
			errors.add(String.format("PrimaryColumn[%s] does not match table PrimaryColumn[%s]", getPrimaryColumn(), table.getPrimaryColumn()));
		}

		if (table.getColumns().length != getColumns().length) {
			errors.add(String.format("Length=`%d` does not match table Length=`%d`", getColumns().length, table.getColumns().length));
		}

		for (int i = 0; i < Math.min(table.getColumns().length, getColumns().length); i++) {
			if (!table.getColumns()[i].matches(getColumns()[i])) {
				errors.add(String.format("Column[%s] does not match table Column[%s]`", getColumns()[i], table.getColumns()[i]));
			}
		}

		if (errors.length() != 0) {
			log.error(errors.toString());
			throw new IllegalArgumentException(String.format("Headers[%s.%s.%s] do not match Table[%s]", getTable(), getName(), getSuffix(), table.getId()));
		}
	}
}
