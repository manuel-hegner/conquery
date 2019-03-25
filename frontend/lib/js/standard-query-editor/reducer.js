// @flow

import T from "i18n-react";
import difference from "lodash.difference";

import {
  getConceptsByIdsWithTablesAndSelects,
  getConceptById
} from "../category-trees/globalTreeStoreHelper";

import { isEmpty, objectWithoutKey } from "../common/helpers";

import { type DateRangeType } from "../common/types/backend";

import { resetAllFiltersInTables } from "../model/table";

import {
  QUERY_GROUP_MODAL_SET_DATE,
  QUERY_GROUP_MODAL_RESET_ALL_DATES
} from "../query-group-modal/actionTypes";

import {
  LOAD_PREVIOUS_QUERY_START,
  LOAD_PREVIOUS_QUERY_SUCCESS,
  LOAD_PREVIOUS_QUERY_ERROR,
  RENAME_PREVIOUS_QUERY_SUCCESS
} from "../previous-queries/list/actionTypes";

import { UPLOAD_CONCEPT_LIST_MODAL_ACCEPT } from "../upload-concept-list-modal/actionTypes";

import {
  INTEGER_RANGE,
  REAL_RANGE,
  MONEY_RANGE
} from "../form-components/filterTypes";

import type { StateType } from "../query-runner/reducer";

import {
  DROP_AND_NODE,
  DROP_OR_NODE,
  DELETE_NODE,
  DELETE_GROUP,
  TOGGLE_EXCLUDE_GROUP,
  LOAD_QUERY,
  CLEAR_QUERY,
  EXPAND_PREVIOUS_QUERY,
  SELECT_NODE_FOR_EDITING,
  DESELECT_NODE,
  UPDATE_NODE_LABEL,
  ADD_CONCEPT_TO_NODE,
  REMOVE_CONCEPT_FROM_NODE,
  TOGGLE_TABLE,
  SET_FILTER_VALUE,
  SET_TABLE_SELECTS,
  SET_SELECTS,
  RESET_ALL_FILTERS,
  SWITCH_FILTER_MODE,
  TOGGLE_TIMESTAMPS,
  LOAD_FILTER_SUGGESTIONS_START,
  LOAD_FILTER_SUGGESTIONS_SUCCESS,
  LOAD_FILTER_SUGGESTIONS_ERROR,
  SET_RESOLVED_FILTER_VALUES,
  TOGGLE_INCLUDE_SUBNODES
} from "./actionTypes";

import type {
  QueryNodeType,
  QueryGroupType,
  StandardQueryType,
  DraggedNodeType,
  DraggedQueryType
} from "./types";

const initialState: StandardQueryType = [];

export const withDefaultValues = arr => {
  if (!arr) return arr;

  return arr.map(obj => {
    // Tables passed
    if (obj.selects) return { ...obj, selects: withDefaultValues(obj.selects) };

    // Selects passed
    return { ...obj, selected: !!obj.default };
  });
};

const filterItem = (
  item: DraggedNodeType | DraggedQueryType
): QueryNodeType => {
  // This sort of mapping might be a problem when adding new optional properties to
  // either Nodes or Queries: Flow won't complain when we omit those optional
  // properties here. But we can't use a spread operator either...

  if (item.isPreviousQuery)
    return {
      label: item.label,
      excludeTimestamps: item.excludeTimestamps,
      loading: item.loading,
      error: item.error,

      id: item.id,
      // eslint-disable-next-line no-use-before-define
      query: item.query,
      isPreviousQuery: item.isPreviousQuery
    };
  else
    return {
      ids: item.ids,
      tables: withDefaultValues(item.tables),
      selects: withDefaultValues(item.selects),
      tree: item.tree,

      label: item.label,
      excludeTimestamps: item.excludeTimestamps,
      loading: item.loading,
      error: item.error,

      isPreviousQuery: item.isPreviousQuery
    };
};

const setGroupProperties = (node, andIdx, properties) => {
  return [
    ...node.slice(0, andIdx),
    {
      ...node[andIdx],
      ...properties
    },
    ...node.slice(andIdx + 1)
  ];
};

const setElementProperties = (node, andIdx, orIdx, properties) => {
  const groupProperties = {
    elements: [
      ...node[andIdx].elements.slice(0, orIdx),
      {
        ...node[andIdx].elements[orIdx],
        ...properties
      },
      ...node[andIdx].elements.slice(orIdx + 1)
    ]
  };

  return setGroupProperties(node, andIdx, groupProperties);
};

const setAllElementsProperties = (node, properties) => {
  return node.map(group => ({
    ...group,
    elements: group.elements.map(element => ({
      ...element,
      ...properties
    }))
  }));
};

const dropAndNode = (
  state,
  action: {
    payload: {
      item: DraggedNodeType | DraggedQueryType,
      dateRange?: DateRangeType
    }
  }
) => {
  const group = state[state.length - 1];
  const dateRangeOfLastGroup = group ? group.dateRange : null;
  const { item, dateRange = dateRangeOfLastGroup } = action.payload;

  const nextState = [
    ...state,
    {
      elements: [filterItem(item)],
      dateRange: dateRange
    }
  ];

  return item.moved
    ? deleteNode(nextState, {
        payload: { andIdx: item.andIdx, orIdx: item.orIdx }
      })
    : nextState;
};

const dropOrNode = (
  state,
  action: {
    payload: {
      item: DraggedNodeType | DraggedQueryType,
      andIdx: number
    }
  }
) => {
  const { item, andIdx } = action.payload;

  const nextState = [
    ...state.slice(0, andIdx),
    {
      ...state[andIdx],
      elements: [filterItem(item), ...state[andIdx].elements]
    },
    ...state.slice(andIdx + 1)
  ];

  return item.moved
    ? item.andIdx === andIdx
      ? deleteNode(nextState, {
          payload: { andIdx: item.andIdx, orIdx: item.orIdx + 1 }
        })
      : deleteNode(nextState, {
          payload: { andIdx: item.andIdx, orIdx: item.orIdx }
        })
    : nextState;
};

// Delete a single Node (concept inside a group)
const deleteNode = (
  state,
  action: { payload: { andIdx: number, orIdx: number } }
) => {
  const { andIdx, orIdx } = action.payload;

  return [
    ...state.slice(0, andIdx),
    {
      ...state[andIdx],
      elements: [
        ...state[andIdx].elements.slice(0, orIdx),
        ...state[andIdx].elements.slice(orIdx + 1)
      ]
    },
    ...state.slice(andIdx + 1)
  ].filter(and => !!and.elements && and.elements.length > 0);
};

const deleteGroup = (state, action) => {
  const { andIdx } = action.payload;

  return [...state.slice(0, andIdx), ...state.slice(andIdx + 1)];
};

const toggleExcludeGroup = (state, action) => {
  const { andIdx } = action.payload;

  return [
    ...state.slice(0, andIdx),
    {
      ...state[andIdx],
      exclude: state[andIdx].exclude ? undefined : true
    },
    ...state.slice(andIdx + 1)
  ];
};

const loadQuery = (state, action) => {
  // In case there is no query, keep state the same
  if (!action.payload.query) return state;

  return action.payload.query;
};

const updateNodeTable = (state, andIdx, orIdx, tableIdx, table) => {
  const node = state[andIdx].elements[orIdx];
  const tables = [
    ...node.tables.slice(0, tableIdx),
    table,
    ...node.tables.slice(tableIdx + 1)
  ];

  return updateNodeTables(state, andIdx, orIdx, tables);
};

const updateNodeTables = (state, andIdx, orIdx, tables) => {
  return setElementProperties(state, andIdx, orIdx, { tables });
};

const toggleNodeTable = (state, action) => {
  const { tableIdx, isExcluded } = action.payload;

  const nodePosition = selectEditedNode(state);
  if (!nodePosition) return state;

  const { andIdx, orIdx } = nodePosition;
  const node = state[andIdx].elements[orIdx];
  const table = {
    ...node.tables[tableIdx],
    exclude: isExcluded
  };

  return updateNodeTable(state, andIdx, orIdx, tableIdx, table);
};

const selectEditedNode = state => {
  const selectedNodes = state
    .reduce(
      (acc, group, andIdx) => [
        ...acc,
        ...group.elements.map((element, orIdx) => ({ andIdx, orIdx, element }))
      ],
      []
    )
    .filter(({ element }) => element.isEditing)
    .map(({ andIdx, orIdx }) => ({ andIdx, orIdx }));

  return selectedNodes.length ? selectedNodes[0] : null;
};

const setNodeFilterProperties = (state, action, properties) => {
  const { tableIdx, filterIdx } = action.payload;

  const node = selectEditedNode(state);

  if (!node) return state;

  const { andIdx, orIdx } = node;
  const table = state[andIdx].elements[orIdx].tables[tableIdx];
  const { filters } = table;

  if (!filters) return state;

  const filter = filters[filterIdx];

  const newTable = {
    ...table,
    filters: [
      ...filters.slice(0, filterIdx),
      {
        ...filter,
        ...properties
      },
      ...filters.slice(filterIdx + 1)
    ]
  };

  return updateNodeTable(state, andIdx, orIdx, tableIdx, newTable);
};

const setNodeFilterValue = (state, action) => {
  const { value, formattedValue } = action.payload;

  // "action" is weirdly split if half here.
  // Check if formattedValue is actually needed and refactor.
  return setNodeFilterProperties(state, action, {
    value,
    formattedValue
  });
};

const setNodeTableSelects = (state, action) => {
  const { tableIdx, value } = action.payload;
  const { andIdx, orIdx } = selectEditedNode(state);
  const table = state[andIdx].elements[orIdx].tables[tableIdx];
  const { selects } = table;

  // value contains the selects that have now been selected
  const newTable = {
    ...table,
    selects: selects.map(select => ({
      ...select,
      selected: !!value.find(selectedValue => selectedValue.value === select.id)
    }))
  };

  return updateNodeTable(state, andIdx, orIdx, tableIdx, newTable);
};

const setNodeSelects = (state, action) => {
  const { value } = action.payload;
  const { andIdx, orIdx } = selectEditedNode(state);
  const { selects } = state[andIdx].elements[orIdx];

  return setElementProperties(state, andIdx, orIdx, {
    selects: selects.map(select => ({
      ...select,
      selected: !!value.find(selectedValue => selectedValue.value === select.id)
    }))
  });
};

const switchNodeFilterMode = (state, action) => {
  const { mode } = action.payload;

  return setNodeFilterProperties(state, action, {
    mode,
    value: null,
    formattedValue: null
  });
};

const resetNodeAllFilters = (state, action) => {
  const nodeIdx = selectEditedNode(state);
  if (!nodeIdx) return state;

  const { andIdx, orIdx } = nodeIdx;
  const node = state[andIdx].elements[orIdx];

  const newState = setElementProperties(state, andIdx, orIdx, {
    excludeTimestamps: false,
    selects: node.selects
      ? node.selects.map(select => ({
          ...select,
          selected: false
        }))
      : null
  });

  if (!node.tables) return newState;

  const tables = resetAllFiltersInTables(node.tables);

  return updateNodeTables(newState, andIdx, orIdx, tables);
};

const setGroupDate = (state, action) => {
  const { andIdx, date } = action.payload;

  return setGroupProperties(state, andIdx, { dateRange: date });
};

const resetGroupDates = (state, action) => {
  const { andIdx } = action.payload;

  return setGroupProperties(state, andIdx, { dateRange: null });
};

// Merges filter values from `table` into declared filters from `savedTable`
//
// `savedTable` may define filters, but it won't have any filter values,
// since `savedTables` comes from a `savedConcept` in a `categoryTree`. Such a
// `savedConcept` is never modified and only declares possible filters.
// Since `table` comes from a previous query, it may have set filter values
// if so, we will need to merge them in.
const mergeFiltersFromSavedConcept = (savedTable, table) => {
  if (!table || !table.filters) return savedTable.filters || null;

  if (!savedTable.filters) return null;

  return savedTable.filters.map(filter => {
    // TODO: Improve the api and don't use `.filter`, but `.id` or `.filterId`
    const matchingFilter =
      table.filters.find(f => f.filter === filter.id) || {};

    const filterModeWithValue =
      matchingFilter.type === INTEGER_RANGE ||
      matchingFilter.type === REAL_RANGE ||
      matchingFilter.type === MONEY_RANGE
        ? matchingFilter.value &&
          !isEmpty(matchingFilter.value.min) &&
          !isEmpty(matchingFilter.value.max) &&
          matchingFilter.value.min === matchingFilter.value.max
          ? { mode: "exact", value: { exact: matchingFilter.value.min } }
          : { mode: "range", value: matchingFilter.value }
        : matchingFilter;

    return {
      ...filter,
      ...filterModeWithValue // => this one may contain a "value" property
    };
  });
};

const mergeSelects = (savedSelects, conceptOrTable) => {
  if (!conceptOrTable || !conceptOrTable.selects) return savedSelects || null;

  if (!savedSelects) return null;

  return savedSelects.map(select => {
    const selectedSelect = conceptOrTable.selects.find(id => id === select.id);

    return { ...select, selected: !!selectedSelect };
  });
};

const mergeTables = (savedTables, concept) => {
  return savedTables
    ? savedTables.map(savedTable => {
        // Find corresponding table in previous queryObject
        // TODO: Disentangle id / connectorId mixing
        const table = concept.tables.find(t => t.id === savedTable.connectorId);
        const filters = mergeFiltersFromSavedConcept(savedTable, table);
        const selects = mergeSelects(savedTable.selects, table);

        return {
          ...savedTable,
          exclude: !table,
          filters,
          selects
        };
      })
    : [];
};

// Look for tables in the already savedConcept. If they were not included in the
// respective query concept, exclude them.
// Also, apply all necessary filters
const mergeFromSavedConcept = (savedConcept, concept) => {
  const tables = mergeTables(savedConcept.tables, concept);
  const selects = mergeSelects(savedConcept.selects, concept);

  return { selects, tables };
};

const expandNode = (rootConcepts, node) => {
  switch (node.type) {
    case "OR":
      return {
        ...node,
        elements: node.children.map(c => expandNode(rootConcepts, c))
      };
    case "SAVED_QUERY":
      return {
        ...node,
        id: node.query,
        query: node.resolvedQuery,
        isPreviousQuery: true
      };
    case "DATE_RESTRICTION":
      return {
        dateRange: node.dateRange,
        ...expandNode(rootConcepts, node.child)
      };
    case "NEGATION":
      return {
        exclude: true,
        ...expandNode(rootConcepts, node.child)
      };
    default:
      const ids = node.ids || [node.id];
      const lookupResult = getConceptsByIdsWithTablesAndSelects(
        ids,
        rootConcepts
      );

      if (!lookupResult)
        return {
          ...node,
          error: T.translate("queryEditor.couldNotExpandNode")
        };

      const { tables, selects } = mergeFromSavedConcept(lookupResult, node);
      const label = node.label || lookupResult.concepts[0].label;

      return {
        ...node,
        label,
        tables,
        selects,
        tree: lookupResult.root
      };
  }
};

// Completely override all groups in the editor with the previous groups, but
// a) merge elements with concept data from category trees (esp. "tables")
// b) load nested previous queries contained in that query,
//    so they can also be expanded
const expandPreviousQuery = (
  state,
  action: { payload: { groups: QueryGroupType[] } }
) => {
  const { rootConcepts, query } = action.payload;

  if (!query.root || query.root.type !== "AND") {
    throw new Error("Cant expand query, because root is not AND");
  }

  return query.root.children.map(child => expandNode(rootConcepts, child));
};

const findPreviousQueries = (state, action) => {
  // Find all nodes that are previous queries and have the correct id
  const queries = state
    .map((group, andIdx) => {
      return group.elements
        .map((concept, orIdx) => ({ ...concept, orIdx }))
        .filter(
          concept =>
            concept.isPreviousQuery && concept.id === action.payload.queryId
        )
        .map(concept => ({
          andIdx,
          orIdx: concept.orIdx,
          node: objectWithoutKey("orIdx")(concept)
        }));
    })
    .filter(group => group.length > 0);

  return [].concat.apply([], queries);
};

const updatePreviousQueries = (state, action, attributes) => {
  const queries = findPreviousQueries(state, action);

  return queries.reduce((nextState, query) => {
    const { node, andIdx, orIdx } = query;

    return [
      ...nextState.slice(0, andIdx),
      {
        ...nextState[andIdx],
        elements: [
          ...nextState[andIdx].elements.slice(0, orIdx),
          {
            ...node,
            ...attributes
          },
          ...nextState[andIdx].elements.slice(orIdx + 1)
        ]
      },
      ...nextState.slice(andIdx + 1)
    ];
  }, state);
};

const loadPreviousQueryStart = (state, action) => {
  return updatePreviousQueries(state, action, { loading: true });
};
const loadPreviousQuerySuccess = (state, action) => {
  const label = action.payload.data.label
    ? { label: action.payload.data.label }
    : {};

  return updatePreviousQueries(state, action, {
    ...label,
    id: action.payload.data.id,
    loading: false,
    query: action.payload.data.query.query // TODO: Backend bug, here should be only "query"
  });
};
const loadPreviousQueryError = (state, action) => {
  return updatePreviousQueries(state, action, {
    loading: false,
    error: action.payload.message
  });
};
const renamePreviousQuery = (state, action) => {
  return updatePreviousQueries(state, action, {
    loading: false,
    label: action.payload.label
  });
};

const toggleTimestamps = (state, action) => {
  const { isExcluded } = action.payload;

  const nodePosition = selectEditedNode(state);
  if (!nodePosition) return state;

  const { andIdx, orIdx } = nodePosition;

  return setElementProperties(state, andIdx, orIdx, {
    excludeTimestamps: isExcluded
  });
};

const loadFilterSuggestionsStart = (state, action) =>
  setNodeFilterProperties(state, action, { isLoading: true });

const loadFilterSuggestionsSuccess = (state, action) => {
  // When [] comes back from the API, don't touch the current options
  if (!action.payload.data || action.payload.data.length === 0)
    return setNodeFilterProperties(state, action, { isLoading: false });

  return setNodeFilterProperties(state, action, {
    isLoading: false,
    options: action.payload.data
  });
};

const loadFilterSuggestionsError = (state, action) =>
  setNodeFilterProperties(state, action, { isLoading: false });

const createQueryNodeFromConceptListUploadResult = (
  label,
  rootConcepts,
  resolvedConcepts,
  selectedConceptRootNode
): DraggedNodeType => {
  const lookupResult = getConceptsByIdsWithTablesAndSelects(
    resolvedConcepts,
    rootConcepts
  );

  return lookupResult
    ? {
        label,
        ids: resolvedConcepts,
        tables: lookupResult.tables,
        selects: lookupResult.selects,
        tree: lookupResult.root
      }
    : null;
};

const insertUploadedConceptList = (state, action) => {
  const {
    label,
    rootConcepts,
    resolvedConcepts,
    selectedConceptRootNode
  } = action.payload;

  const queryElement = createQueryNodeFromConceptListUploadResult(
    label,
    rootConcepts,
    resolvedConcepts,
    selectedConceptRootNode
  );

  // TODO: Re-enable soon
  // if (parameters.andIdx != null)
  //   return dropOrNode(state, {
  //     payload: { item: queryElement, andIdx: parameters.andIdx }
  //   });

  return queryElement
    ? dropAndNode(state, {
        payload: { item: queryElement }
      })
    : state;
};

const selectNodeForEditing = (state, { payload: { andIdx, orIdx } }) => {
  return setElementProperties(state, andIdx, orIdx, { isEditing: true });
};

const deselectNode = (state, action) => {
  return setAllElementsProperties(state, { isEditing: false });
};

const updateNodeLabel = (state, action) => {
  const node = selectEditedNode(state);

  if (!node) return state;

  const { andIdx, orIdx } = node;

  return setElementProperties(state, andIdx, orIdx, {
    label: action.payload.label
  });
};

const addConceptToNode = (state, action) => {
  const nodePosition = selectEditedNode(state);

  if (!nodePosition) return state;

  const { andIdx, orIdx } = nodePosition;
  const node = state[andIdx].elements[orIdx];

  return setElementProperties(state, andIdx, orIdx, {
    ids: [...action.payload.concept.ids, ...node.ids]
  });
};

const removeConceptFromNode = (state, action) => {
  const nodePosition = selectEditedNode(state);

  if (!nodePosition) return state;

  const { andIdx, orIdx } = nodePosition;
  const node = state[andIdx].elements[orIdx];

  return setElementProperties(state, andIdx, orIdx, {
    ids: node.ids.filter(id => id !== action.payload.conceptId)
  });
};

const setResolvedFilterValues = (state: StateType, action: Object) => {
  const {
    tableIdx,
    filterIdx,
    data: {
      resolvedFilter: { value }
    }
  } = action.payload;

  return setNodeFilterProperties(
    state,
    {
      payload: {
        tableIdx,
        filterIdx
      }
    },
    {
      value
    }
  );
};

const toggleIncludeSubnodes = (state: StateType, action: Object) => {
  const { includeSubnodes } = action.payload;

  const nodePosition = selectEditedNode(state);
  if (!nodePosition) return state;

  const { andIdx, orIdx } = nodePosition;

  const node = state[andIdx].elements[orIdx];

  const concept = getConceptById(node.ids);

  const childIds = [];
  const elements = concept.children.map(childId => {
    const child = getConceptById(childId);
    childIds.push(childId);
    return {
      ids: [childId],
      label: child.label,
      tables: node.tables,
      tree: node.tree
    };
  });

  const groupProps = {
    elements: [
      ...state[andIdx].elements.slice(0, orIdx),
      {
        ...state[andIdx].elements[orIdx],
        includeSubnodes: includeSubnodes
      },
      ...state[andIdx].elements.slice(orIdx + 1)
    ]
  };

  if (includeSubnodes) groupProps.elements.push(...elements);
  else
    groupProps.elements = groupProps.elements.filter(element => {
      return !(difference(element.ids, childIds).length === 0);
    });

  return setGroupProperties(state, andIdx, groupProps);
};

// Query is an array of "groups" (a AND b and c)
// where a, b, c are objects, that (can) have properites,
// like `dateRange` or `exclude`.
// But the main property is "elements" - an array of objects
// that contain at least an ID.
// An element may contain an array of tables that may
// either be excluded, or contain an array of filters with values.
//
// Example:
// [
//   {
//     elements: [
//       { id: 9, tables: [{ id: 1}] },
//       {
//         id: 10,
//         tables: [
//           { id: 213, exclude: true },
//           {
//             id: 452,
//             filters: [
//               { id: 52, type: 'INTEGER_RANGE', value: { min: 2, max: 3 } }
//               { id: 53, type: 'SELECT', value: "Some example filter value" }
//             ]
//           }
//         ]
//       }
//     ]
//   }, {
//     elements: [
//      {id: 6, tables: []}, {id: 7, tables: []}, {id: 5, tables: []}
//     ]
//   }
// ]
const query = (
  state: StandardQueryType = initialState,
  action: Object
): StandardQueryType => {
  switch (action.type) {
    case DROP_AND_NODE:
      return dropAndNode(state, action);
    case DROP_OR_NODE:
      return dropOrNode(state, action);
    case DELETE_NODE:
      return deleteNode(state, action);
    case DELETE_GROUP:
      return deleteGroup(state, action);
    case TOGGLE_EXCLUDE_GROUP:
      return toggleExcludeGroup(state, action);
    case LOAD_QUERY:
      return loadQuery(state, action);
    case CLEAR_QUERY:
      return initialState;
    case SELECT_NODE_FOR_EDITING:
      return selectNodeForEditing(state, action);
    case DESELECT_NODE:
      return deselectNode(state, action);
    case UPDATE_NODE_LABEL:
      return updateNodeLabel(state, action);
    case ADD_CONCEPT_TO_NODE:
      return addConceptToNode(state, action);
    case REMOVE_CONCEPT_FROM_NODE:
      return removeConceptFromNode(state, action);
    case TOGGLE_TABLE:
      return toggleNodeTable(state, action);
    case SET_FILTER_VALUE:
      return setNodeFilterValue(state, action);
    case SET_TABLE_SELECTS:
      return setNodeTableSelects(state, action);
    case SET_SELECTS:
      return setNodeSelects(state, action);
    case RESET_ALL_FILTERS:
      return resetNodeAllFilters(state, action);
    case SWITCH_FILTER_MODE:
      return switchNodeFilterMode(state, action);
    case TOGGLE_TIMESTAMPS:
      return toggleTimestamps(state, action);
    case QUERY_GROUP_MODAL_SET_DATE:
      return setGroupDate(state, action);
    case QUERY_GROUP_MODAL_RESET_ALL_DATES:
      return resetGroupDates(state, action);
    case EXPAND_PREVIOUS_QUERY:
      return expandPreviousQuery(state, action);
    case LOAD_PREVIOUS_QUERY_START:
      return loadPreviousQueryStart(state, action);
    case LOAD_PREVIOUS_QUERY_SUCCESS:
      return loadPreviousQuerySuccess(state, action);
    case LOAD_PREVIOUS_QUERY_ERROR:
      return loadPreviousQueryError(state, action);
    case RENAME_PREVIOUS_QUERY_SUCCESS:
      return renamePreviousQuery(state, action);
    case LOAD_FILTER_SUGGESTIONS_START:
      return loadFilterSuggestionsStart(state, action);
    case LOAD_FILTER_SUGGESTIONS_SUCCESS:
      return loadFilterSuggestionsSuccess(state, action);
    case LOAD_FILTER_SUGGESTIONS_ERROR:
      return loadFilterSuggestionsError(state, action);
    case UPLOAD_CONCEPT_LIST_MODAL_ACCEPT:
      return insertUploadedConceptList(state, action);
    case SET_RESOLVED_FILTER_VALUES:
      return setResolvedFilterValues(state, action);
    case TOGGLE_INCLUDE_SUBNODES:
      return toggleIncludeSubnodes(state, action);
    default:
      return state;
  }
};

export default query;
