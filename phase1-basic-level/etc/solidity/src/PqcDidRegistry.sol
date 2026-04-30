// SPDX-License-Identifier: Apache-2.0
pragma solidity 0.8.27;

contract PqcDidRegistry {
    error DidNotFound(string did);
    error DidAlreadyRegistered(string did);

    struct DidDoc {
        bytes mlDsaPubKey;
        bool registered;
        bool deactivated;
    }

    mapping(string => DidDoc) private _dids;

    function register(string calldata did, bytes calldata pubKey) external {
        if (_dids[did].registered) revert DidAlreadyRegistered(did);
        _dids[did] = DidDoc({mlDsaPubKey: pubKey, registered: true, deactivated: false});
    }

    function pubKeyOf(string calldata did) external view returns (bytes memory) {
        if (!_dids[did].registered) revert DidNotFound(did);
        return _dids[did].mlDsaPubKey;
    }

    function deactivate(string calldata did) external {
        if (!_dids[did].registered) revert DidNotFound(did);
        _dids[did].deactivated = true;
    }
}
