// @flow

import Papa from "papaparse";
import T from "i18n-react";
import { type Dispatch } from "redux-thunk";

import { type DatasetIdType } from "../../dataset/reducer";

import { defaultSuccess, defaultError } from "../../common/actions";

import api from "../../api";

import { loadPreviousQueries } from "../list/actions";

import {
  OPEN_UPLOAD_MODAL,
  CLOSE_UPLOAD_MODAL,
  UPLOAD_FILE_START,
  UPLOAD_FILE_SUCCESS,
  UPLOAD_FILE_ERROR
} from "./actionTypes";

export const openUploadModal = () => ({
  type: OPEN_UPLOAD_MODAL
});

export const closeUploadModal = () => ({
  type: CLOSE_UPLOAD_MODAL
});

export const uploadFileStart = () => ({ type: UPLOAD_FILE_START });
export const uploadFileSuccess = (success: any) =>
  defaultSuccess(UPLOAD_FILE_SUCCESS, success);
export const uploadFileError = (error: any, payload: Object) =>
  defaultError(UPLOAD_FILE_ERROR, error, {
    successful: payload.successful,
    unsuccessful: payload.unsuccessful
  });

export const uploadFile = (datasetId: DatasetIdType, file: any) => (
  dispatch: Dispatch
) => {
  dispatch(uploadFileStart());

  Papa.parse(file, {
    complete: results => {
      // TODO: THIS IS NOT COMPLETE
      // RESULTS NEED TO BE TRANSFORMED
      console.log(results.data);
      return api.postQueries(datasetId, results, "external").then(
        r => {
          dispatch(uploadFileSuccess(r));

          return dispatch(loadPreviousQueries(datasetId));
        },
        e =>
          dispatch(
            uploadFileError(
              { message: T.translate("uploadQueryResultsModal.uploadFailed") },
              e
            )
          )
      );
    }
  });
};
