// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import { connect } from "react-redux";

import InputDateRange from "../form-components/InputDateRange";
import IconButton from "../button/IconButton";
import { Modal } from "../modal";

import {
  queryGroupModalClearNode,
  queryGroupModalSetDate,
  queryGroupModalResetAllDates
} from "./actions";

const Root = styled("div")`
  min-width: 460px;
`;

const Headline = styled("h3")`
  text-align: center;
`;

const HeadlinePart = styled("span")`
  padding: 0 0 0 5px;
`;

const ResetAll = styled(IconButton)`
  color: ${({ theme }) => theme.col.blueGrayDark};
  font-weight: 700;
  margin-left: 20px;
`;

type PropsType = {
  group: Object,
  andIdx: number,
  onClose: () => void,
  onSetDate: any => void,
  onResetAllDates: () => void
};

const QueryGroupModal = (props: PropsType) => {
  if (!props.group) return null;

  const { dateRange } = props.group;

  const minDate = dateRange ? dateRange.min : null;
  const maxDate = dateRange ? dateRange.max : null;
  const hasActiveDate = !!(minDate || maxDate);

  const { onSetDate } = props;

  return (
    <Modal onClose={props.onClose} doneButton tabIndex={3}>
      <Root>
        <Headline>
          {props.group.elements.reduce(
            (parts, concept, i, elements) => [
              ...parts,
              <HeadlinePart key={i + "-headline"}>
                {concept.label || concept.id}
              </HeadlinePart>,
              i !== elements.length - 1 ? (
                <span key={i + "-comma"}>, </span>
              ) : (
                ""
              )
            ],
            [
              <HeadlinePart key={-1}>
                {T.translate("queryGroupModal.headlineStart")}
              </HeadlinePart>
            ]
          )}
        </Headline>
        <InputDateRange
          large
          inline
          label={T.translate("queryGroupModal.explanation")}
          labelSuffix={
            <>
              {hasActiveDate && (
                <ResetAll bare onClick={props.onResetAllDates} icon="undo">
                  {T.translate("queryNodeEditor.reset")}
                </ResetAll>
              )}
            </>
          }
          input={{
            onChange: onSetDate,
            value: dateRange
          }}
        />
      </Root>
    </Modal>
  );
};

function findGroup(query, andIdx) {
  if (!query[andIdx]) return null;

  return query[andIdx];
}

const mapStateToProps = state => ({
  group: findGroup(state.queryEditor.query, state.queryGroupModal.andIdx),
  andIdx: state.queryGroupModal.andIdx
});

const mapDispatchToProps = (dispatch: any) => ({
  onClose: () => dispatch(queryGroupModalClearNode()),
  onSetDate: (andIdx, date) => dispatch(queryGroupModalSetDate(andIdx, date)),
  onResetAllDates: andIdx => dispatch(queryGroupModalResetAllDates(andIdx))
});

// Used to enhance the dispatchProps with the andIdx
const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...ownProps,
  ...stateProps,
  ...dispatchProps,
  onSetDate: date => dispatchProps.onSetDate(stateProps.andIdx, date),
  onResetAllDates: () => dispatchProps.onResetAllDates(stateProps.andIdx)
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
  mergeProps
)(QueryGroupModal);
