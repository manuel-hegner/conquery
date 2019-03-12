// @flow

import React from "react";
import styled from "@emotion/styled";

import { connect } from "react-redux";
import type { Dispatch } from "redux";
import T from "i18n-react";
import { replace } from "react-router-redux";

import { queryGroupModalSetNode } from "../query-group-modal/actions";
import {
  loadPreviousQuery,
  loadAllPreviousQueriesInGroups
} from "../previous-queries/list/actions";
import type { DateRangeType } from "../common/types/backend";

import {
  dropAndNode,
  dropOrNode,
  deleteNode,
  deleteGroup,
  dropConceptListFile,
  toggleExcludeGroup,
  expandPreviousQuery,
  selectNodeForEditing
} from "./actions";
import type {
  StandardQueryType,
  DraggedNodeType,
  DraggedQueryType
} from "./types";
import { QueryEditorDropzone } from "./QueryEditorDropzone";
import QueryGroup from "./QueryGroup";

type PropsType = {
  query: StandardQueryType,
  isEmptyQuery: boolean,
  dropAndNode: (DraggedNodeType | DraggedQueryType, ?DateRangeType) => void,
  dropOrNode: (DraggedNodeType | DraggedQueryType, number) => void,
  deleteNode: Function,
  deleteGroup: Function,
  toggleExcludeGroup: Function,
  expandPreviousQuery: Function,
  loadPreviousQuery: Function,
  selectNodeForEditing: Function,
  queryGroupModalSetNode: Function,
  dateRange: Object
};

const Container = styled("div")`
  height: 100%;
`;

const Groups = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: flex-start;
`;

const QueryGroupConnector = styled("p")`
  padding: 70px 6px;
  font-size: ${({ theme }) => theme.font.sm};
  color: ${({ theme }) => theme.col.gray};
  text-align: center;
`;

const Query = (props: PropsType) => {
  return (
    <Container>
      {props.isEmptyQuery && (
        // Render a large Dropzone
        <QueryEditorDropzone
          isInitial
          onDropNode={item => props.dropAndNode(item)}
          onDropFile={props.dropConceptListFile}
          onLoadPreviousQuery={props.loadPreviousQuery}
        />
      )}
      <Groups>
        {!props.isEmptyQuery &&
          // Render all query groups plus individual AND / OR dropzones
          props.query
            .map((group, andIdx) => [
              <QueryGroup
                key={andIdx}
                group={group}
                andIdx={andIdx}
                onDropNode={item => props.dropOrNode(item, andIdx)}
                onDropFile={file => props.dropConceptListFile(file, andIdx)}
                onDeleteNode={orIdx => props.deleteNode(andIdx, orIdx)}
                onDeleteGroup={orIdx => props.deleteGroup(andIdx, orIdx)}
                onEditClick={orIdx => props.selectNodeForEditing(andIdx, orIdx)}
                onExpandClick={props.expandPreviousQuery}
                onExcludeClick={() => props.toggleExcludeGroup(andIdx)}
                onDateClick={() => props.queryGroupModalSetNode(andIdx)}
                onLoadPreviousQuery={props.loadPreviousQuery}
              />,
              <QueryGroupConnector key={`${andIdx}.and`}>
                {T.translate("common.and")}
              </QueryGroupConnector>
            ])
            .concat(
              <div className="dropzone-wrap" key={props.query.length + 1}>
                <QueryEditorDropzone
                  isAnd
                  onDropNode={item => props.dropAndNode(item, props.dateRange)}
                  onDropFile={file => props.dropConceptListFile(file)}
                  onLoadPreviousQuery={props.loadPreviousQuery}
                />
              </div>
            )}
      </Groups>
    </Container>
  );
};

function mapStateToProps(state) {
  return {
    query: state.panes.right.tabs.queryEditor.query,
    isEmptyQuery: state.panes.right.tabs.queryEditor.query.length === 0,

    // only used by other actions
    rootConcepts: state.categoryTrees.trees
  };
}

const mapDispatchToProps = (dispatch: Dispatch<*>) => ({
  dropAndNode: (item, dateRange) => dispatch(dropAndNode(item, dateRange)),
  dropConceptListFile: (file, andIdx) =>
    dispatch(dropConceptListFile(file, andIdx)),
  dropOrNode: (item, andIdx) => dispatch(dropOrNode(item, andIdx)),
  deleteNode: (andIdx, orIdx) => dispatch(deleteNode(andIdx, orIdx)),
  deleteGroup: (andIdx, orIdx) => dispatch(deleteGroup(andIdx, orIdx)),
  toggleExcludeGroup: andIdx => dispatch(toggleExcludeGroup(andIdx)),
  selectNodeForEditing: (andIdx, orIdx) =>
    dispatch(selectNodeForEditing(andIdx, orIdx)),
  queryGroupModalSetNode: andIdx => dispatch(queryGroupModalSetNode(andIdx)),
  expandPreviousQuery: (datasetId, rootConcepts, groups, queryId) => {
    dispatch(expandPreviousQuery(rootConcepts, groups));

    dispatch(loadAllPreviousQueriesInGroups(groups, datasetId));

    // dispatch(replace(toQuery(datasetId, queryId)));
  },
  loadPreviousQuery: (datasetId, queryId) =>
    dispatch(loadPreviousQuery(datasetId, queryId))
});

const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...stateProps,
  ...dispatchProps,
  ...ownProps,
  loadPreviousQuery: queryId =>
    dispatchProps.loadPreviousQuery(ownProps.selectedDatasetId, queryId),
  expandPreviousQuery: (groups, queryId) =>
    dispatchProps.expandPreviousQuery(
      ownProps.selectedDatasetId,
      stateProps.rootConcepts,
      groups,
      queryId
    )
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
  mergeProps
)(Query);
