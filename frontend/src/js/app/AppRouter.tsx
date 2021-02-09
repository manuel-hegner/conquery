import React from "react";
import { ReactKeycloakProvider } from "@react-keycloak/web";
import keycloak from "../../keycloak";
import { BrowserRouter as Router, Route, Switch } from "react-router-dom";
import type { TabT } from "../pane/types";

import LoginPage from "../authorization/LoginPage";
import WithAuthToken from "../authorization/WithAuthToken";

import App from "./App";
import { basename } from "../environment";

interface PropsT {
  rightTabs: TabT[];
}

const AppRouter = (props: PropsT) => {
  return (
    <ReactKeycloakProvider
      authClient={keycloak}
      initOptions={{
        onLoad: "check-sso",
        pkceMethod: "S256",
      }}
    >
      <Router basename={basename()}>
        <Switch>
          <Route path="/login" component={LoginPage} />
          <Route
            path="/*"
            render={(routeProps) => (
              <WithAuthToken {...routeProps}>
                <App {...props} />
              </WithAuthToken>
            )}
          />
        </Switch>
      </Router>
    </ReactKeycloakProvider>
  );
};

export default AppRouter;
