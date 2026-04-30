// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.25;

import {IDilithiumVerifier} from "./IDilithiumVerifier.sol";
import {ISigVerifier} from "InterfaceVerifier/IVerifier.sol";
import {PqcDidRegistry} from "./PqcDidRegistry.sol";

contract PqcVcVerifier {
    PqcDidRegistry public immutable registry;
    IDilithiumVerifier public immutable verifier;

    constructor(PqcDidRegistry registry_, IDilithiumVerifier verifier_) {
        registry = registry_;
        verifier = verifier_;
    }

    function verifyVc(string calldata issuerDid, bytes32 vcDigest, bytes calldata signature)
        external
        view
        returns (bool)
    {
        bytes memory pk = registry.pubKeyOf(issuerDid);
        return verifier.verify(pk, vcDigest, signature) == ISigVerifier.verify.selector;
    }
}
