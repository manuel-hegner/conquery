// @flow

import React from "react";
import styled from "@emotion/styled";
import { connect } from "react-redux";
import T from "i18n-react";

import { PREVIOUS_QUERY, TIMEBASED_NODE } from "../common/constants/dndTypes";
import Dropzone from "../form-components/Dropzone";

import { removeTimebasedNode } from "./actions";

type PropsType = {
  onDropNode: () => void,
  onRemoveTimebasedNode: () => void
};

const StyledDropzone = styled(Dropzone)`
  width: 150px;
  text-align: center;
  background-color: ${({ theme }) => theme.col.bg};
`;

const DROP_TYPES = [PREVIOUS_QUERY, TIMEBASED_NODE];

const TimebasedQueryEditorDropzone = ({
  onRemoveTimebasedNode,
  onDropNode
}: PropsType) => {
  const onDrop = (props, monitor) => {
    const item = monitor.getItem();

    const { moved } = item;

    if (moved) {
      const { conditionIdx, resultIdx } = item;

      onRemoveTimebasedNode(conditionIdx, resultIdx, moved);
      onDropNode(item.node, moved);
    } else {
      onDropNode(item, false);
    }
  };

  return (
    <StyledDropzone acceptedDropTypes={DROP_TYPES} onDrop={onDrop}>
      {T.translate("dropzone.dragQuery")}
    </StyledDropzone>
  );
};

export default connect(
  () => ({}),
  dispatch => ({
    onRemoveTimebasedNode: (conditionIdx, resultIdx, moved) =>
      dispatch(removeTimebasedNode(conditionIdx, resultIdx, moved))
  })
)(TimebasedQueryEditorDropzone);
