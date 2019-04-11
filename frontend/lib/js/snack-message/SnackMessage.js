import React from "react";
import styled from "@emotion/styled";
import { connect } from "react-redux";
import onClickOutside from "react-onclickoutside";
import T from "i18n-react";

import FaIcon from "../icon/FaIcon";

const Root = styled("div")`
  position: fixed;
  z-index: 10;
  bottom: 20px;
  right: 20px;
  background-color: rgba(0, 0, 0, 0.5);
  color: white;
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  max-width: 500px;
  border-radius: 5px;
`;

const Relative = styled("div")`
  position: relative;
  padding: 12px 40px 12px 20px;
`;

const ClearZone = styled("div")`
  position: absolute;
  top: 12px;
  right: 18px;
  z-index: 11;
  cursor: pointer;
  opacity: 0.8;
  &:hover {
    opacity: 1;
  }
`;

class SnackMessage extends React.PureComponent {
  state = {
    show: false
  };

  handleClickOutside(e) {
    this.setState({ show: false });
  }

  componentDidUpdate(prevProps) {
    if (
      !!this.props.messageKey &&
      this.props.messageKey !== prevProps.messageKey
    ) {
      this.setState({ show: true });
    }
  }
  render() {
    // Must be an empty div here for onClickOutside to connect properly
    return (
      <div>
        {this.state.show && (
          <Root>
            <Relative>
              {this.props.messageKey && T.translate(this.props.messageKey)}
              <ClearZone onClick={() => this.setState({ show: false })}>
                <FaIcon white large icon="times" />
              </ClearZone>
            </Relative>
          </Root>
        )}
      </div>
    );
  }
}

export default connect(state => ({
  messageKey: state.snackMessage.messageKey
}))(onClickOutside(SnackMessage));
