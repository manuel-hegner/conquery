import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import { exists } from "../common/helpers/exists";
import ReactSelect from "../form-components/ReactSelect";
import type { StandardQueryStateT } from "../standard-query-editor/queryReducer";
import WithTooltip from "../tooltip/WithTooltip";

import { useSelectDataset } from "./actions";
import { DatasetT } from "./reducer";

const becauseWeCantStyleReactSelectWithGenericProps = css`
  > div {
    min-width: 300px;
  }
`;
const Root = styled("label")`
  color: ${({ theme }) => theme.col.black};
  display: flex;
  align-items: center;
  justify-content: flex-end;
  ${becauseWeCantStyleReactSelectWithGenericProps};
`;

const Headline = styled("span")`
  font-size: ${({ theme }) => theme.col.grayLight};
  padding-right: 20px;
`;

const DatasetSelector: FC = () => {
  const { t } = useTranslation();
  const selectedDatasetId = useSelector<StateT, string | null>(
    (state) => state.datasets.selectedDatasetId,
  );
  const datasets = useSelector<StateT, DatasetT[]>(
    (state) => state.datasets.data,
  );
  const error = useSelector<StateT, string | null>(
    (state) => state.datasets.error,
  );
  const query = useSelector<StateT, StandardQueryStateT>(
    (state) => state.queryEditor.query,
  );

  const selectDataset = useSelectDataset();

  const onSelectDataset = (datasetId: string | null) =>
    selectDataset(datasets, datasetId, selectedDatasetId, query);

  const options =
    datasets && datasets.map((db) => ({ value: db.id, label: db.label }));
  const selected = options.find((set) => selectedDatasetId === set.value);

  return (
    <WithTooltip text={t("help.datasetSelector")} lazy>
      <Root>
        <Headline>{t("datasetSelector.label")}</Headline>
        <ReactSelect<false>
          name="dataset-selector"
          value={error ? null : selected}
          onChange={(value) =>
            exists(value) ? onSelectDataset(value.value) : onSelectDataset(null)
          }
          placeholder={
            error ? t("datasetSelector.error") : t("reactSelect.placeholder")
          }
          isDisabled={!!error}
          options={options}
          small
        />
      </Root>
    </WithTooltip>
  );
};

export default DatasetSelector;
