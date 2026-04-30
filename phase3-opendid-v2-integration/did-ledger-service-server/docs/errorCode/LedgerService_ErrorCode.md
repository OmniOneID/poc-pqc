---
puppeteer:
    pdf:
        format: A4
        displayHeaderFooter: true
        landscape: false
        scale: 0.8
        margin:
            top: 1.2cm
            right: 1cm
            bottom: 1cm
            left: 1cm
    image:
        quality: 100
        
        fullPage: false
---

DID Ledger Service Error Code
==

- Date: 2025-06-12
- Version: v2.0.0

Table of Contents
---
<!-- TOC tocDepth:2..3 chapterDepth:2..6 -->

- [1. Overview](#1-overview)
- [2. Error Code Format](#2-error-code-format)
- [3. Error Code Categories](#3-error-code-categories)
- [4. API Error Codes](#4-api-error-codes)
  - [4.1. DID Document Registration Errors](#41-did-document-registration-errors)
  - [4.2. DID Document Retrieval Errors](#42-did-document-retrieval-errors)
  - [4.3. DID Document Update Errors](#43-did-document-update-errors)
  - [4.4. VC Metadata Registration Errors](#44-vc-metadata-registration-errors)
  - [4.5. VC Metadata Retrieval Errors](#45-vc-metadata-retrieval-errors)
  - [4.6. Authentication/Authorization Errors](#46-authenticationauthorization-errors)
  - [4.7. Blockchain Transaction Errors](#47-blockchain-transaction-errors)
  - [4.8. System Errors](#48-system-errors)

<!-- /TOC -->

## 1. Overview

This document defines the error codes used by the DID Ledger Service Server. Each error code provides specific information about failures that may occur during API operations, including blockchain transactions, DID Document management, and VC Metadata operations.

<div style="page-break-after: always; margin-top: 50px;"></div>

## 2. Error Code Format

The DID Ledger Service uses a standardized error code format:

**Format**: `SRVLDG{Category}{Number}`

- **SRVLDG**: Service identifier for DID Ledger Service
- **Category**: 2-digit category identifier
- **Number**: 3-digit sequential number within the category

**Example**: `SRVLDG00100` - Invalid DID Document format

<div style="page-break-after: always; margin-top: 50px;"></div>

## 3. Error Code Categories

| Category | Range        | Description                           |
| -------- | ------------ | ------------------------------------- |
| 00       | 00000-00999  | DID Document related errors          |
| 01       | 01000-01999  | VC Metadata related errors           |
| 02       | 02000-02999  | Authentication/Authorization errors  |
| 03       | 03000-03999  | Blockchain transaction errors        |
| 04       | 04000-04999  | System and infrastructure errors     |
| 05       | 05000-05999  | Validation and format errors         |
| 99       | 99000-99999  | Unknown and unexpected errors        |

<div style="page-break-after: always; margin-top: 50px;"></div>

## 4. API Error Codes

### 4.1. DID Document Registration Errors

| Error Code   | HTTP Status | Description                          | Solution                                    |
| ------------ | ----------- | ------------------------------------ | ------------------------------------------- |
| SRVLDG00100  | 400         | Invalid DID Document format         | Check DID Document JSON structure          |
| SRVLDG00101  | 400         | Invalid digital signature           | Verify signature with correct private key  |
| SRVLDG00102  | 400         | DID already exists                  | Use update API for existing DID            |
| SRVLDG00103  | 400         | Invalid invoker nonce               | Provide unique nonce for transaction       |
| SRVLDG00104  | 400         | DID Document validation failed      | Ensure DID Document follows W3C standards  |
| SRVLDG00200  | 500         | Failed to register DID Document     | Check blockchain connectivity              |
| SRVLDG00201  | 500         | Database transaction failed         | Retry or contact system administrator      |

### 4.2. DID Document Retrieval Errors

| Error Code   | HTTP Status | Description                          | Solution                                    |
| ------------ | ----------- | ------------------------------------ | ------------------------------------------- |
| SRVLDG00300  | 400         | DID does not exist                  | Verify DID identifier is correct           |
| SRVLDG00301  | 400         | Invalid DID format                  | Check DID syntax follows did:omn:ledger    |
| SRVLDG00302  | 400         | DID is deactivated                  | Cannot retrieve deactivated DID Document   |
| SRVLDG00400  | 500         | Failed to retrieve DID Document     | Check system status and retry              |
| SRVLDG00401  | 500         | Blockchain query failed             | Check blockchain node connectivity         |

### 4.3. DID Document Update Errors

| Error Code   | HTTP Status | Description                          | Solution                                    |
| ------------ | ----------- | ------------------------------------ | ------------------------------------------- |
| SRVLDG00500  | 400         | DID does not exist                  | Register DID first before updating         |
| SRVLDG00501  | 400         | Invalid signature                   | Verify signature with DID owner's key      |
| SRVLDG00502  | 400         | Unauthorized update attempt         | Only DID owner can update                  |
| SRVLDG00503  | 400         | Invalid version update              | Check version sequencing                   |
| SRVLDG00504  | 400         | Update after deactivation          | Cannot update deactivated DID Document     |
| SRVLDG00600  | 500         | Failed to update DID Document       | Check blockchain transaction status        |
| SRVLDG00601  | 500         | Version conflict detected          | Retry with latest version                  |

### 4.4. VC Metadata Registration Errors

| Error Code   | HTTP Status | Description                          | Solution                                    |
| ------------ | ----------- | ------------------------------------ | ------------------------------------------- |
| SRVLDG00700  | 400         | Invalid VC Metadata format          | Check VC Metadata JSON structure           |
| SRVLDG00701  | 400         | VC already exists                   | Use unique VC identifier                   |
| SRVLDG00702  | 400         | Invalid signature                   | Verify signature with issuer's key         |
| SRVLDG00703  | 400         | Invalid VC identifier               | Use valid UUID format                      |
| SRVLDG00704  | 400         | Missing required fields             | Include all mandatory VC Metadata fields   |
| SRVLDG00800  | 500         | Failed to register VC Metadata      | Check system status and retry              |
| SRVLDG00801  | 500         | Metadata storage failed             | Check database connectivity                |

### 4.5. VC Metadata Retrieval Errors

| Error Code   | HTTP Status | Description                          | Solution                                    |
| ------------ | ----------- | ------------------------------------ | ------------------------------------------- |
| SRVLDG00900  | 400         | VC does not exist                   | Verify VC identifier is correct            |
| SRVLDG00901  | 400         | Invalid VC ID format                | Use valid UUID format                      |
| SRVLDG00902  | 400         | VC is revoked                       | Cannot retrieve revoked VC Metadata        |
| SRVLDG01000  | 500         | Failed to retrieve VC Metadata      | Check system status and retry              |
| SRVLDG01001  | 500         | Database query failed               | Check database connectivity                |

### 4.6. Authentication/Authorization Errors

| Error Code   | HTTP Status | Description                          | Solution                                    |
| ------------ | ----------- | ------------------------------------ | ------------------------------------------- |
| SRVLDG02000  | 401         | Authentication required             | Provide valid authentication token         |
| SRVLDG02001  | 401         | Invalid authentication token        | Renew authentication token                 |
| SRVLDG02002  | 401         | Expired authentication token        | Obtain new authentication token            |
| SRVLDG02003  | 403         | Insufficient permissions            | Contact administrator for permission       |
| SRVLDG02004  | 403         | Access denied                       | Verify user has required access rights     |
| SRVLDG02005  | 403         | IP address not allowed              | Access from authorized IP address          |

### 4.7. Blockchain Transaction Errors

| Error Code   | HTTP Status | Description                          | Solution                                    |
| ------------ | ----------- | ------------------------------------ | ------------------------------------------- |
| SRVLDG03000  | 500         | Blockchain connection failed        | Check blockchain node status              |
| SRVLDG03001  | 500         | Transaction timeout                 | Retry transaction or check network         |
| SRVLDG03002  | 500         | Insufficient gas                    | Increase gas limit for transaction         |
| SRVLDG03003  | 500         | Transaction reverted                | Check transaction parameters               |
| SRVLDG03004  | 500         | Block confirmation failed          | Wait for block confirmation               |
| SRVLDG03005  | 500         | Smart contract error                | Check contract state and parameters        |

### 4.8. System Errors

| Error Code   | HTTP Status | Description                          | Solution                                    |
| ------------ | ----------- | ------------------------------------ | ------------------------------------------- |
| SRVLDG04000  | 500         | Internal server error               | Contact system administrator              |
| SRVLDG04001  | 500         | Database connection failed          | Check database connectivity               |
| SRVLDG04002  | 500         | Configuration error                 | Verify system configuration               |
| SRVLDG04003  | 503         | Service temporarily unavailable     | Retry after service restoration           |
| SRVLDG04004  | 500         | Memory allocation failed            | Check system resources                    |
| SRVLDG04005  | 500         | File system error                   | Check disk space and permissions          |

**■ General Error Response Format**

All API errors follow this standard response format:

```json
{
  "error": {
    "code": "SRVLDG00100",
    "message": "Invalid DID Document format",
    "detail": "Missing required field: '@context'",
    "timestamp": "2025-06-12T10:00:00Z",
    "requestId": "req-12345"
  }
}
```

**■ Error Handling Best Practices**

1. **Retry Logic**: For 5xx errors, implement exponential backoff retry
2. **Logging**: Log error details for debugging and monitoring
3. **User Feedback**: Provide meaningful error messages to end users
4. **Monitoring**: Set up alerts for critical error patterns
5. **Documentation**: Keep error documentation updated with API changes