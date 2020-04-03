/// <reference types="react-scripts" />

declare namespace NodeJS {
  interface ProcessEnv {
    NODE_ENV: "development" | "production";
    REACT_APP_API_URL?: string;
    REACT_APP_DISABLE_LOGIN?: boolean;
    REACT_APP_LANG?: "de" | "en";
    PORT?: string;
  }
}