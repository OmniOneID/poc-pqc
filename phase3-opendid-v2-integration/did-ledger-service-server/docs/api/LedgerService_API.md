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

DID Ledger Service API
==

- Date: 2025-06-12
- Version: v2.0.0
  
Table of Contents
---
<!-- TOC tocDepth:2..3 chapterDepth:2..6 -->

- [1. Overview](#1-overview)
- [2. Terminology](#2-terminology)
- [3. API List](#3-api-list)
    - [3.1. Sequential API](#31-sequential-api)
    - [3.2. Single Call API](#32-single-call-api)
- [4. Single Call API](#4-single-call-api)
    - [4.1. Register DID Document](#41-register-did-document)
    - [4.2. Get DID Document](#42-get-did-document)
    - [4.3. Update DID Document](#43-update-did-document)
    - [4.4. Register VC Metadata](#44-register-vc-metadata)
    - [4.5. Get VC Metadata](#45-get-vc-metadata)

<!-- /TOC -->


## 1. Overview

This document defines the APIs provided by the DID Ledger Service Server. The DID Ledger Service provides functionality for managing DID Documents and VC Metadata on the blockchain ledger.

<div style="page-break-after: always; margin-top: 50px;"></div>

## 2. Terminology
- Protocol
  - A set of `Sequential APIs` that must be called in a predetermined order to perform a specific function. The API call sequence must be strictly followed, as incorrect ordering may lead to unexpected results.
- Sequential API
  - A series of APIs that are called in a predetermined order to perform a specific function (protocol). Each API must be called sequentially, and incorrect ordering may prevent proper operation.
  - However, some protocols may have APIs with the same calling sequence, in which case one API can be selected and called.
- Single Call API
  - An API that can be called independently regardless of order, like typical REST APIs.
- Standard API
  - APIs clearly defined in the API documentation that must be provided consistently across all implementations. Standard APIs ensure interoperability between systems and must operate according to predefined specifications.
- Non-Standard API
  - APIs that can be defined or customized differently according to the needs of each implementation. The non-standard APIs provided in this document are just one example and may be implemented differently for each implementation. In this case, separate documentation for each implementation is required.

<div style="page-break-after: always; margin-top: 50px;"></div>

## 3. API List

### 3.1. Sequential API
The DID Ledger Service currently has no protocols defined for performing specific functions, and therefore does not provide sequential APIs.

<div style="page-break-after: always; margin-top: 40px;"></div>

### 3.2. Single Call API

| API                    | URL                           | Description                           | Standard API |
| ---------------------- | ----------------------------- | ------------------------------------- | ------------ |
| `register-diddoc`      | /api/v1/diddoc/register       | Register DID Document to ledger      | N            |
| `get-diddoc`           | /api/v1/diddoc                | Retrieve DID Document from ledger    | N            |
| `update-diddoc`        | /api/v1/diddoc/update         | Update DID Document on ledger        | N            |
| `register-vcmeta`      | /api/v1/vcmeta/register       | Register VC Metadata to ledger       | N            |
| `get-vcmeta`           | /api/v1/vcmeta                | Retrieve VC Metadata from ledger     | N            |

<div style="page-break-after: always; margin-top: 50px;"></div>

## 4. Single Call API

Single Call APIs are independent APIs that perform a specific function.
Therefore, they are not Sequential APIs (aka protocols) which are groups of APIs that must be called in order, so no protocol number is assigned.
The list of Single Call APIs provided by the DID Ledger Service is shown in the table below.

| API                    | URL                           | Description                           | Standard API |
| ---------------------- | ----------------------------- | ------------------------------------- | ------------ |
| `register-diddoc`      | /api/v1/diddoc/register       | Register DID Document to ledger      | N            |
| `get-diddoc`           | /api/v1/diddoc                | Retrieve DID Document from ledger    | N            |
| `update-diddoc`        | /api/v1/diddoc/update         | Update DID Document on ledger        | N            |
| `register-vcmeta`      | /api/v1/vcmeta/register       | Register VC Metadata to ledger       | N            |
| `get-vcmeta`           | /api/v1/vcmeta                | Retrieve VC Metadata from ledger     | N            |

■ Authorization

The DID Ledger Service APIs include authorization mechanisms to verify caller permissions.
Authentication and authorization are handled through:

- API Key based authentication for service-to-service communication
- Digital signature verification for DID operations
- Role-based access control for administrative functions

### 4.1. Register DID Document

Register a new DID Document to the blockchain ledger.

| Item          | Description                   | Remarks |
| ------------- | ----------------------------- | ------- |
| Method        | `POST`                        |         |
| Path          | `/api/v1/diddoc/register`     |         |
| Authorization | Required                      |         |

#### 4.1.1. Request

**■ HTTP Headers**

| Header           | Value                            | Remarks |
| ---------------- | -------------------------------- | ------- |
| + `Content-Type` | `application/json;charset=utf-8` |         |
| + `Authorization`| `Bearer {token}`                 |         |

**■ Path Parameters**

N/A

**■ Query Parameters**

N/A

**■ HTTP Body**

```json
{
  "didDoc": "string",           // Multibase encoded DID Document
  "signature": "string",        // Digital signature of the DID Document
  "invokerNonce": "string"      // Unique nonce for transaction
}
```

<div style="page-break-after: always; margin-top: 30px;"></div>

#### 4.1.2. Response

**■ Process**
1. Validate DID Document format and signature
2. Register DID Document to blockchain ledger
3. Return transaction result

**■ Status 200 - Success**

```json
{
  "txId": "string",             // Transaction ID on blockchain
  "did": "string",              // Registered DID
  "status": "SUCCESS"
}
```

**■ Status 400 - Client error**

| Code         | Description                      |
| ------------ | -------------------------------- |
| SRVLDG00100  | Invalid DID Document format     |
| SRVLDG00101  | Invalid signature                |
| SRVLDG00102  | DID already exists              |

**■ Status 500 - Server error**

| Code         | Description                      |
| ------------ | -------------------------------- |
| SRVLDG00200  | Failed to register DID Document |

#### 4.1.3. Example

**■ Request**

```shell
curl -v -X POST "http://${Host}:${Port}/api/v1/diddoc/register" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "didDoc": "meyJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvbnMv...",
    "signature": "mH1Xl2pIzwN8...",
    "invokerNonce": "1234567890"
  }'
```

**■ Response**

```http
HTTP/1.1 200 OK
Content-Type: application/json;charset=utf-8

{
  "txId": "0x123456789abcdef",
  "did": "did:omn:ledger:12345",
  "status": "SUCCESS"
}
```

<div style="page-break-after: always; margin-top: 40px;"></div>

### 4.2. Get DID Document

Retrieve DID Document from the blockchain ledger.

| Item          | Description      | Remarks |
| ------------- | ---------------- | ------- |
| Method        | `GET`           |         |
| Path          | `/api/v1/diddoc` |         |
| Authorization | -                |         |

#### 4.2.1. Request

**■ HTTP Headers**

| Header           | Value                            | Remarks |
| ---------------- | -------------------------------- | ------- |
| + `Content-Type` | `application/json;charset=utf-8` |         |

**■ Path Parameters**

N/A

**■ Query Parameters**

| name    | Description | Remarks |
| ------- | ----------- | ------- |
| + `did` | DID identifier |       |

**■ HTTP Body**

N/A

<div style="page-break-after: always; margin-top: 30px;"></div>

#### 4.2.2. Response

**■ Process**
1. Retrieve DID Document from blockchain ledger by DID

**■ Status 200 - Success**

```json
{
  "didDoc": "string",           // Multibase encoded DID Document
  "status": "ACTIVE",           // DID Document status
  "versionId": "string",        // Version identifier
  "created": "2025-06-12T10:00:00Z",
  "updated": "2025-06-12T10:00:00Z"
}
```

**■ Status 400 - Client error**

| Code         | Description              |
| ------------ | ------------------------ |
| SRVLDG00300  | DID does not exist       |
| SRVLDG00301  | Invalid DID format       |

**■ Status 500 - Server error**

| Code         | Description                   |
| ------------ | ----------------------------- |
| SRVLDG00400  | Failed to retrieve DID Document |

#### 4.2.3. Example

**■ Request**

```shell
curl -v -X GET "http://${Host}:${Port}/api/v1/diddoc?did=did%3Aomn%3Aledger%3A12345"
```

**■ Response**

```http
HTTP/1.1 200 OK
Content-Type: application/json;charset=utf-8

{
  "didDoc": "meyJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvbnMv...",
  "status": "ACTIVE",
  "versionId": "v1.0",
  "created": "2025-06-12T10:00:00Z",
  "updated": "2025-06-12T10:00:00Z"
}
```

<div style="page-break-after: always; margin-top: 40px;"></div>

### 4.3. Update DID Document

Update an existing DID Document on the blockchain ledger.

| Item          | Description                   | Remarks |
| ------------- | ----------------------------- | ------- |
| Method        | `PUT`                         |         |
| Path          | `/api/v1/diddoc/update`       |         |
| Authorization | Required                      |         |

#### 4.3.1. Request

**■ HTTP Headers**

| Header           | Value                            | Remarks |
| ---------------- | -------------------------------- | ------- |
| + `Content-Type` | `application/json;charset=utf-8` |         |
| + `Authorization`| `Bearer {token}`                 |         |

**■ Path Parameters**

N/A

**■ Query Parameters**

N/A

**■ HTTP Body**

```json
{
  "did": "string",              // DID to update
  "didDoc": "string",           // Updated DID Document (multibase encoded)
  "signature": "string",        // Digital signature
  "invokerNonce": "string"      // Unique nonce for transaction
}
```

#### 4.3.2. Response

**■ Process**
1. Validate DID Document and signature
2. Update DID Document on blockchain ledger
3. Return transaction result

**■ Status 200 - Success**

```json
{
  "txId": "string",
  "did": "string",
  "versionId": "string",
  "status": "SUCCESS"
}
```

**■ Status 400 - Client error**

| Code         | Description                      |
| ------------ | -------------------------------- |
| SRVLDG00500  | DID does not exist              |
| SRVLDG00501  | Invalid signature                |
| SRVLDG00502  | Unauthorized update attempt     |

**■ Status 500 - Server error**

| Code         | Description                      |
| ------------ | -------------------------------- |
| SRVLDG00600  | Failed to update DID Document   |

<div style="page-break-after: always; margin-top: 40px;"></div>

### 4.4. Register VC Metadata

Register VC (Verifiable Credential) Metadata to the blockchain ledger.

| Item          | Description                   | Remarks |
| ------------- | ----------------------------- | ------- |
| Method        | `POST`                        |         |
| Path          | `/api/v1/vcmeta/register`     |         |
| Authorization | Required                      |         |

#### 4.4.1. Request

**■ HTTP Headers**

| Header           | Value                            | Remarks |
| ---------------- | -------------------------------- | ------- |
| + `Content-Type` | `application/json;charset=utf-8` |         |
| + `Authorization`| `Bearer {token}`                 |         |

**■ Path Parameters**

N/A

**■ Query Parameters**

N/A

**■ HTTP Body**

```json
{
  "vcId": "string",             // VC identifier
  "vcMeta": "string",           // Multibase encoded VC Metadata
  "signature": "string",        // Digital signature
  "invokerNonce": "string"      // Unique nonce for transaction
}
```

#### 4.4.2. Response

**■ Process**
1. Validate VC Metadata and signature
2. Register VC Metadata to blockchain ledger
3. Return transaction result

**■ Status 200 - Success**

```json
{
  "txId": "string",
  "vcId": "string",
  "status": "SUCCESS"
}
```

**■ Status 400 - Client error**

| Code         | Description                      |
| ------------ | -------------------------------- |
| SRVLDG00700  | Invalid VC Metadata format      |
| SRVLDG00701  | VC already exists               |
| SRVLDG00702  | Invalid signature               |

**■ Status 500 - Server error**

| Code         | Description                      |
| ------------ | -------------------------------- |
| SRVLDG00800  | Failed to register VC Metadata  |

<div style="page-break-after: always; margin-top: 40px;"></div>

### 4.5. Get VC Metadata

Retrieve VC Metadata from the blockchain ledger.

| Item          | Description      | Remarks |
| ------------- | ---------------- | ------- |
| Method        | `GET`            |         |
| Path          | `/api/v1/vcmeta` |         |
| Authorization | -                |         |

#### 4.5.1. Request

**■ HTTP Headers**

| Header           | Value                            | Remarks |
| ---------------- | -------------------------------- | ------- |
| + `Content-Type` | `application/json;charset=utf-8` |         |

**■ Path Parameters**

N/A

**■ Query Parameters**

| name     | Description | Remarks |
| -------- | ----------- | ------- |
| + `vcId` | VC identifier |       |

**■ HTTP Body**

N/A

#### 4.5.2. Response

**■ Process**
1. Retrieve VC Metadata from blockchain ledger by VC ID

**■ Status 200 - Success**

```json
{
  "vcId": "string",
  "vcMeta": "string",           // Multibase encoded VC Metadata
  "status": "ACTIVE",
  "created": "2025-06-12T10:00:00Z"
}
```

**■ Status 400 - Client error**

| Code         | Description             |
| ------------ | ----------------------- |
| SRVLDG00900  | VC does not exist       |
| SRVLDG00901  | Invalid VC ID format    |

**■ Status 500 - Server error**

| Code         | Description                   |
| ------------ | ----------------------------- |
| SRVLDG01000  | Failed to retrieve VC Metadata |

#### 4.5.3. Example

**■ Request**

```shell
curl -v -X GET "http://${Host}:${Port}/api/v1/vcmeta?vcId=c184fb29-e6e1-4144-bae0-ccc44a3770df"
```

**■ Response**

```http
HTTP/1.1 200 OK
Content-Type: application/json;charset=utf-8

{
  "vcId": "c184fb29-e6e1-4144-bae0-ccc44a3770df",
  "vcMeta": "meyJjcmVkZW50aWFsU2NoZW1hIjp7ImlkIjoiaHR0cDovLzE...",
  "status": "ACTIVE",
  "created": "2025-06-12T10:00:00Z"
}
```
