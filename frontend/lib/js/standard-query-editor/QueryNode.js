// @flow

import React from "react";
import { findDOMNode } from "react-dom";
import styled from "@emotion/styled";
import T from "i18n-react";
import { DragSource, type ConnectDragSource } from "react-dnd";

import { dndTypes } from "../common/constants";
import { ErrorMessage } from "../error-message";
import { nodeHasActiveFilters } from "../model/node";
import { isQueryExpandable } from "../model/query";

import QueryNodeActions from "./QueryNodeActions";

import type { QueryNodeType, DraggedNodeType, DraggedQueryType } from "./types";

const Root = styled("div")`
  position: relative;
  width: 100%;
  margin: 0 auto;
  background-color: white;
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  padding: 7px;
  font-size: ${({ theme }) => theme.font.sm};
  cursor: pointer;
  text-align: left;
  border-radius: ${({ theme }) => theme.borderRadius};
  transition: border ${({ theme }) => theme.transitionTime};
  border: ${({ theme, hasActiveFilters }) =>
    hasActiveFilters
      ? `2px solid ${theme.col.blueGrayDark}`
      : `1px solid ${theme.col.grayMediumLight}`};
  &:hover {
    border: ${({ theme, hasActiveFilters }) =>
      hasActiveFilters
        ? `2px solid ${theme.col.blueGrayDark}`
        : `1px solid ${theme.col.blueGrayDark}`};
  }
`;

const Node = styled("div")`
  flex-grow: 1;
  padding-top: 2px;
`;

const Label = styled("p")`
  margin: 0;
  word-break: break-word;
  line-height: 1.2;
  font-size: ${({ theme }) => theme.font.md};
`;
const Description = styled("p")`
  margin: 3px 0 0;
  word-break: break-word;
  line-height: 1.2;
  text-transform: uppercase;
  font-size: ${({ theme }) => theme.font.xs};
`;

const PreviousQueryLabel = styled("p")`
  margin: 0 0 3px;
  line-height: 1.2;
  font-size: ${({ theme }) => theme.font.xs};
  text-transform: uppercase;
  font-weight: 700;
  color: ${({ theme }) => theme.col.blueGrayDark};
`;

const StyledErrorMessage = styled(ErrorMessage)`
  margin: 0;
`;

type PropsType = {
  node: QueryNodeType,
  onDeleteNode: Function,
  onEditClick: Function,
  onToggleTimestamps: Function,
  onExpandClick: Function,
  connectDragSource: Function,
  andIdx: number,
  orIdx: number,
  connectDragSource: ConnectDragSource
};

// Has to be a class because of https://github.com/react-dnd/react-dnd/issues/530
class QueryNode extends React.Component {
  props: PropsType;

  render() {
    const {
      node,
      connectDragSource,
      onExpandClick,
      onEditClick,
      onDeleteNode,
      onToggleTimestamps
    } = this.props;

    return (
      <Root
        ref={instance => connectDragSource(instance)}
        hasActiveFilters={!node.error && nodeHasActiveFilters(node)}
        onClick={!node.error && onEditClick}
      >
        <Node>
          {node.isPreviousQuery && (
            <PreviousQueryLabel>
              {T.translate("queryEditor.previousQuery")}
            </PreviousQueryLabel>
          )}
          {node.error ? (
            <StyledErrorMessage message={node.error} />
          ) : (
            <>
              <Label>{node.label || node.id}</Label>
              {node.description && (!node.ids || node.ids.length === 1) && (
                <Description>{node.description}</Description>
              )}
            </>
          )}
        </Node>
        <QueryNodeActions
          excludeTimestamps={node.excludeTimestamps}
          onEditClick={onEditClick}
          onDeleteNode={onDeleteNode}
          onToggleTimestamps={onToggleTimestamps}
          isExpandable={isQueryExpandable(node)}
          onExpandClick={() => {
            if (!node.query) return;

            onExpandClick(node.query);
          }}
          previousQueryLoading={node.loading}
          error={node.error}
        />
      </Root>
    );
  }
}

/**
 * Implements the drag source contract.
 */
const nodeSource = {
  beginDrag(props, monitor, component): DraggedNodeType | DraggedQueryType {
    // Return the data describing the dragged item
    // NOT using `...node` since that would also spread `children` in.
    // This item may stem from either:
    // 1) A concept (dragged from CategoryTreeNode)
    // 2) A previous query (dragged from PreviousQueries)
    const { node, andIdx, orIdx } = props;
    const { height, width } = findDOMNode(component).getBoundingClientRect();

    const draggedNode = {
      moved: true,
      andIdx,
      orIdx,

      width,
      height,

      label: node.label,
      excludeTimestamps: node.excludeTimestamps,

      loading: node.loading,
      error: node.error
    };

    if (node.isPreviousQuery)
      return {
        ...draggedNode,
        id: node.id,
        query: node.query,
        isPreviousQuery: true
      };
    else
      return {
        ...draggedNode,
        ids: node.ids,
        description: node.description,
        tree: node.tree,
        tables: node.tables,
        selects: node.selects
      };
  }
};

/**
 * Specifies the dnd-related props to inject into the component.
 */
const collect = (connect, monitor) => ({
  connectDragSource: connect.dragSource(),
  isDragging: monitor.isDragging()
});

export default DragSource(dndTypes.QUERY_NODE, nodeSource, collect)(QueryNode);
