import React, { FC } from "react";
import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import TableFilters from "./TableFilters";
import TableSelects from "./TableSelects";
import ContentCell from "./ContentCell";
import DateColumnSelect from "./DateColumnSelect";
import type { ConceptQueryNodeType } from "../standard-query-editor/types";
import type { CurrencyConfigT, DatasetIdT } from "../api/types";
import type { PostPrefixForSuggestionsParams } from "../api/api";

const Column = styled("div")`
  display: flex;
  flex-direction: column;
  flex-grow: 1;
`;

const MaximizedCell = styled(ContentCell)`
  flex-grow: 1;
  padding-bottom: 30px;
`;

interface PropsT {
  node: ConceptQueryNodeType;
  selectedInputTableIdx: number;
  onShowDescription: (filterIdx: number) => void;
  datasetId: DatasetIdT;
  currencyConfig: CurrencyConfigT;

  onLoadFilterSuggestions: (
    params: PostPrefixForSuggestionsParams,
    tableIdx: number,
    filterIdx: number
  ) => void;

  onSelectTableSelects;
  onSetDateColumn;
  onSetFilterValue;
  onSwitchFilterMode;
}

const TableView: FC<PropsT> = ({
  node,
  selectedInputTableIdx,
  onShowDescription,
  datasetId,
  currencyConfig,

  onSelectTableSelects,
  onSetDateColumn,

  onSetFilterValue,
  onSwitchFilterMode,
  onLoadFilterSuggestions,
}) => {
  const { t } = useTranslation();

  const table = node.tables[selectedInputTableIdx];

  const displaySelects = !!table.selects && table.selects.length > 0;
  const displayDateColumnOptions =
    !!table.dateColumn && table.dateColumn.options.length > 0;
  const displayFilters = !!table.filters && table.filters.length > 0;

  return (
    <Column>
      {displaySelects && (
        <ContentCell headline={t("queryNodeEditor.selects")}>
          {table.selects && table.selects.length > 0 && (
            <TableSelects
              selects={table.selects}
              onSelectTableSelects={(value) =>
                onSelectTableSelects(selectedInputTableIdx, value)
              }
              excludeTable={table.exclude}
            />
          )}
        </ContentCell>
      )}
      {displayDateColumnOptions && (
        <ContentCell headline={t("queryNodeEditor.selectValidityDate")}>
          <DateColumnSelect
            dateColumn={table.dateColumn}
            onSelectDateColumn={(value) =>
              onSetDateColumn(selectedInputTableIdx, value)
            }
          />
        </ContentCell>
      )}
      {displayFilters && (
        <MaximizedCell headline={t("queryNodeEditor.filters")}>
          <TableFilters
            key={selectedInputTableIdx}
            filters={table.filters}
            excludeTable={table.exclude}
            context={{
              datasetId,
              treeId: node.tree,
              tableId: table.id,
            }}
            onSetFilterValue={(filterIdx: number, value: unknown) =>
              onSetFilterValue(selectedInputTableIdx, filterIdx, value)
            }
            onSwitchFilterMode={(filterIdx, mode) =>
              onSwitchFilterMode(selectedInputTableIdx, filterIdx, mode)
            }
            onLoadFilterSuggestions={(filterIdx, filterId, prefix) =>
              onLoadFilterSuggestions(
                {
                  datasetId: datasetId,
                  conceptId: node.tree,
                  tableId: table.id,
                  filterId,
                  prefix,
                },
                selectedInputTableIdx,
                filterIdx
              )
            }
            onShowDescription={onShowDescription}
            currencyConfig={currencyConfig}
          />
        </MaximizedCell>
      )}
    </Column>
  );
};

export default TableView;
