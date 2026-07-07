// SPDX-License-Identifier: Apache-2.0
pragma solidity ^0.8.25;

import "forge-std/Test.sol";
import {PqcDidRegistry} from "../src/PqcDidRegistry.sol";
import {PqcVcVerifier} from "../src/PqcVcVerifier.sol";
import {IDilithiumVerifier} from "../src/IDilithiumVerifier.sol";
import {ZKNOX_ethdilithium} from "ETHDILITHIUM/ZKNOX_ethdilithium.sol";
import {PythonSigner} from "ETHDILITHIUM/ZKNOX_PythonSigner.sol";

contract PqcPhase1Test is Test {
    string constant ISSUER = "did:omn:issuer";
    string constant SEED_STR = "cafecafecafecafecafecafecafecafecafecafecafecafecafecafecafecafe";
    string constant PYREF = "lib/ETHDILITHIUM/pythonref";

    PqcDidRegistry registry;
    PqcVcVerifier vcVerifier;
    ZKNOX_ethdilithium realVerifier;
    PythonSigner pySigner;
    bytes pkAddrBytes;

    function setUp() public {
        registry = new PqcDidRegistry();
        realVerifier = new ZKNOX_ethdilithium();
        vcVerifier = new PqcVcVerifier(registry, IDilithiumVerifier(address(realVerifier)));
        pySigner = new PythonSigner();

        // Derive ML-DSA-44 (ETH-friendly variant) public key off-chain from a deterministic
        // seed and store it on-chain via SSTORE2. setKey returns the SSTORE2 pointer address
        // which is what the verifier will dereference at verify time.
        bytes memory pkBytes = pySigner.getPubKey(PYREF, "ETH", SEED_STR);
        pkAddrBytes = realVerifier.setKey(pkBytes);
        registry.register(ISSUER, pkAddrBytes);
    }

    function test_DidRegister_storesPkPointer() public view {
        bytes memory pk = registry.pubKeyOf(ISSUER);
        assertEq(pk.length, 20);
    }

    function test_VcVerify_realPqcSignature_passes() public {
        bytes32 vcDigest = keccak256("vc-digest");
        (bytes memory cTilde, bytes memory z, bytes memory h) =
            pySigner.sign(PYREF, vm.toString(vcDigest), "ETH", SEED_STR);
        bytes memory sig = abi.encodePacked(cTilde, z, h);

        uint256 gasStart = gasleft();
        bool ok = vcVerifier.verifyVc(ISSUER, vcDigest, sig);
        uint256 gasUsed = gasStart - gasleft();

        emit log_named_uint("VC verify gas used", gasUsed);
        emit log_named_uint("signature size (bytes)", sig.length);
        assertTrue(ok);
    }

    function test_VcVerify_tamperedSignature_fails() public {
        bytes32 vcDigest = keccak256("vc-digest");
        (bytes memory cTilde, bytes memory z, bytes memory h) =
            pySigner.sign(PYREF, vm.toString(vcDigest), "ETH", SEED_STR);
        bytes memory sig = abi.encodePacked(cTilde, z, h);
        sig[100] = bytes1(uint8(sig[100]) ^ 0x01);

        bool ok = vcVerifier.verifyVc(ISSUER, vcDigest, sig);
        assertFalse(ok);
    }

    function test_VcVerify_wrongDigest_fails() public {
        bytes32 vcDigest = keccak256("vc-digest");
        bytes32 otherDigest = keccak256("vc-digest-tampered");
        (bytes memory cTilde, bytes memory z, bytes memory h) =
            pySigner.sign(PYREF, vm.toString(vcDigest), "ETH", SEED_STR);
        bytes memory sig = abi.encodePacked(cTilde, z, h);

        bool ok = vcVerifier.verifyVc(ISSUER, otherDigest, sig);
        assertFalse(ok);
    }

    function test_baseline_ecdsa_verify() public {
        bytes32 digest = keccak256("hello");
        uint256 sk = 0xA11CE;
        (uint8 v, bytes32 r, bytes32 s) = vm.sign(sk, digest);
        address recovered = ecrecover(digest, v, r, s);
        assertEq(recovered, vm.addr(sk));
    }
}
