// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.25;

import {ISigVerifier} from "InterfaceVerifier/IVerifier.sol";

// Local alias: re-exports ZKNOX ISigVerifier so the rest of the codebase keeps a stable name.
// verify returns bytes4(IERC7913.verify.selector) on success, 0xffffffff on failure.
interface IDilithiumVerifier is ISigVerifier {}
