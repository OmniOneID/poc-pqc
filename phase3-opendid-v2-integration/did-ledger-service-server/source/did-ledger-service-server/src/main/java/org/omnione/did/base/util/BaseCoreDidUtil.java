package org.omnione.did.base.util;

import org.omnione.did.core.data.rest.SignatureParams;
import org.omnione.did.core.manager.DidManager;
import org.omnione.did.data.model.did.DidDocument;
import org.omnione.did.data.model.did.VerificationMethod;

import java.util.List;

public class BaseCoreDidUtil {

    /**
     * Parse DID document
     *
     * @author        : yklee0911
     * @since         : 2024/07/03
     */
    public static DidManager parseDidDoc(String didDoc) {
        DidManager didManager = new DidManager();
        didManager.parse(didDoc);

        return didManager;
    }

    /**
     * Parse DID document
     *
     * @author        : yklee0911
     * @since         : 2024/07/15
     */
    public static DidManager parseDidDoc(DidDocument didDocument) {
        DidManager didManager = new DidManager();
        didManager.parse(didDocument.toJson());

        return didManager;
    }


    /**
     * Get all sign key ids
     *
     * @author        : yklee0911
     * @since         : 2024/07/03
     */
    public static List<String> getAllSignKeyIdList(DidManager didmanager) {
        try {
            return didmanager.getAllSignKeyIdList();
        } catch (Exception e) {
            System.out.println("Failed to get sign key ids: " + e.getMessage());
            throw new RuntimeException("Failed to get sign key ids: " + e.getMessage());
        }
    }

    /**
     * Get all sign key ids
     *
     * @author        : yklee0911
     * @since         : 2024/07/15
     */
    public static List<String> getAllSignKeyIdList(DidDocument didDocument) {
        try {
            DidManager didManager = parseDidDoc(didDocument);
            return didManager.getAllSignKeyIdList();
        } catch (Exception e) {
            System.out.println("Failed to get sign key ids: " + e.getMessage());
            throw new RuntimeException("Failed to get sign key ids: " + e.getMessage());
        }
    }

    /**
     * Get all sign data list
     *
     * @author        : yklee0911
     * @since         : 2024/07/03
     */
    public static List<SignatureParams> getAllSignDataList(DidManager didManager, List<String> keyIdList) {
        try {
            return didManager.getOriginDataForSign(keyIdList);
        } catch (Exception e) {
            System.out.println("Failed to get sign data: " + e.getMessage());
            throw new RuntimeException("Failed to get sign data: " + e.getMessage());
        }
    }

    /**
     * Get all sign data list
     *
     * @author        : yklee0911
     * @since         : 2024/07/15
     */
    public static List<SignatureParams> getAllSignDataList(DidDocument ownerDidDoc, List<String> keyIdList) {
        try {
            DidManager didManager = parseDidDoc(ownerDidDoc.toJson());
            return didManager.getOriginDataForSign(keyIdList);
        } catch (Exception e) {
            System.out.println("Failed to get sign data: " + e.getMessage());
            throw new RuntimeException("Failed to get sign data: " + e.getMessage());
        }
    }

    /**
     * Get all sign data list
     *
     * @author        : yklee0911
     * @since         : 2024/07/03
     */
    public static List<SignatureParams> getAllSignDataList(DidManager didManager) {
        try {
            List<String> allSignKeyIdList = getAllSignKeyIdList(didManager);
            return getAllSignDataList(didManager, allSignKeyIdList);
        }  catch (Exception e) {
            System.out.println("Failed to get sign data: " + e.getMessage());
            throw new RuntimeException("Failed to get sign data: " + e.getMessage());
        }
    }

    /**
     * Get all sign data list
     *
     * @author        : yklee0911
     * @since         : 2024/07/16
     */
    public static List<SignatureParams> getAllSignDataList(DidDocument ownerDidDoc) {
        try {
            DidManager didManager = parseDidDoc(ownerDidDoc.toJson());
            List<String> allSignKeyIdList = getAllSignKeyIdList(didManager);
            return getAllSignDataList(didManager, allSignKeyIdList);
        }  catch (Exception e) {
            System.out.println("Failed to get sign data: " + e.getMessage());
            throw new RuntimeException("Failed to get sign data: " + e.getMessage());
        }
    }

    /**
     * Add proofs to DID document
     *
     * @author        : yklee0911
     * @since         : 2024/07/03
     */
    public static void addProofsToDidDoc(DidManager didManager, List<SignatureParams> signatureParamsList) {
        try {
            didManager.addProof(signatureParamsList);
        } catch (Exception e) {
            System.out.println("Failed to add proof to DID document: " + e.getMessage());
            throw new RuntimeException("Failed to add proof to DID document: " + e.getMessage());
        }
    }

}
