import type {
  ConceptIdT,
  QueryIdT,
  RangeFilterT,
  MultiSelectFilterT,
  MultiSelectFilterValueT,
  SelectFilterT,
  SelectFilterValueT,
  SelectorT,
  TableT,
  DateRangeT,
  DateColumnT,
  QueryT,
  BigMultiSelectFilterT,
} from "../api/types";

// A concept that is part of a query node in the editor
export interface ConceptType {
  id: string;
  label: string;
  description?: string;
  matchingEntries?: number;
}

export interface InfoType {
  key: string;
  value: string;
}

export type RangeFilterWithValueType = RangeFilterT;

export interface MultiSelectFilterWithValueType extends MultiSelectFilterT {
  value?: MultiSelectFilterValueT;
}
export interface BigMultiSelectFilterWithValueType
  extends BigMultiSelectFilterT {
  value?: MultiSelectFilterValueT;
}

export interface SelectFilterWithValueType extends SelectFilterT {
  value?: SelectFilterValueT;
}

export type FilterWithValueType =
  | SelectFilterWithValueType
  | MultiSelectFilterWithValueType
  | BigMultiSelectFilterWithValueType
  | RangeFilterWithValueType;

export interface SelectedSelectorT extends SelectorT {
  selected?: boolean;
}

export interface SelectedDateColumnT extends DateColumnT {
  value?: string;
}

export interface TableWithFilterValueT
  extends Omit<TableT, "filters" | "selects" | "dateColumn"> {
  filters: FilterWithValueType[] | null;
  selects?: SelectedSelectorT[];
  dateColumn?: SelectedDateColumnT;
}

export interface DraggedQueryType {
  id: QueryIdT;

  // drag info;
  type: string;
  width: number;
  height: number;

  // eslint-disable-next-line no-use-before-define
  query?: PreviousQueryType;
  label: string;
  excludeTimestamps?: boolean;

  moved?: boolean;
  andIdx?: number;
  orIdx?: number; // These two only exist if moved === true

  loading?: boolean;
  error?: string;

  files?: void;
  isPreviousQuery: boolean; // true

  canExpand?: boolean;

  secondaryId?: string | null;
  availableSecondaryIds?: string[];
  excludeFromSecondaryIdQuery?: boolean;
}

// A Query Node that is being dragged from the tree or within the standard editor.
// Corresponds to CONCEPT_TREE_NODE and QUERY_NODE drag-and-drop types.
export interface DraggedNodeType {
  ids: ConceptIdT[];
  tables: TableWithFilterValueT[];
  selects: SelectedSelectorT[];
  tree: ConceptIdT;
  label: string;
  excludeTimestamps?: boolean;
  excludeFromSecondaryIdQuery?: boolean;

  additionalInfos: Object;
  matchingEntries: number;
  dateRange: Object;

  moved?: boolean;
  andIdx?: number;
  orIdx?: number; // These two only exist if moved === true

  loading?: boolean;
  error?: string;

  files?: void;
  isPreviousQuery?: void;
}

export interface ConceptQueryNodeType {
  ids: ConceptIdT[];
  tables: TableWithFilterValueT[];
  selects: SelectedSelectorT[];
  tree: ConceptIdT;

  label: string;
  description?: string;
  excludeTimestamps?: boolean;
  excludeFromSecondaryIdQuery?: boolean;
  loading?: boolean;
  error?: string;

  isEditing?: boolean;
  isPreviousQuery?: void | false;
}

export interface PreviousQueryQueryNodeType {
  label: string;
  excludeTimestamps?: boolean;
  excludeFromSecondaryIdQuery?: boolean;
  loading?: boolean;
  error?: string;

  id: QueryIdT;
  // eslint-disable-next-line no-use-before-define
  query?: QueryT;
  isPreviousQuery: true;
  canExpand?: boolean;
  isEditing?: boolean;

  secondaryId?: string | null;
  availableSecondaryIds?: string[];
}

export type StandardQueryNodeT =
  | ConceptQueryNodeType
  | PreviousQueryQueryNodeType;

export interface QueryGroupType {
  elements: StandardQueryNodeT[];
  dateRange?: DateRangeT;
  exclude?: boolean;
}
