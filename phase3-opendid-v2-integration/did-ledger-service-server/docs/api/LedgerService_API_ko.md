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

- 일자: 2025-06-12
- 버전: v2.0.0
  
목차
---
<!-- TOC tocDepth:2..3 chapterDepth:2..6 -->

- [1. 개요](#1-개요)
- [2. 용어 설명](#2-용어-설명)
- [3. API 목록](#3-api-목록)
    - [3.1. 순차 API](#31-순차-api)
    - [3.2. 단일호출 API](#32-단일호출-api)
- [4. 단일 호출 API](#4-단일-호출-api)
    - [4.1. DID Document 등록](#41-did-document-등록)
    - [4.2. DID Document 조회](#42-did-document-조회)
    - [4.3. DID Document 수정](#43-did-document-수정)
    - [4.4. VC Metadata 등록](#44-vc-metadata-등록)
    - [4.5. VC Metadata 조회](#45-vc-metadata-조회)

<!-- /TOC -->


## 1. 개요

본 문서는 DID Ledger Service Server가 제공하는 API를 정의한다. DID Ledger Service는 블록체인 원장에서 DID Document와 VC Metadata를 관리하는 기능을 제공한다.

<div style="page-break-after: always; margin-top: 50px;"></div>

## 2. 용어 설명
- 프로토콜 (Protocol)
  - 특정 기능을 수행하기 위해 정해진 순서에 따라 호출해야 하는 `순차 API`의 집합이다. API 호출 순서를 엄격히 따라야 하며, 순서가 잘못될 경우 예상하지 못한 결과가 발생할 수 있다.
- 순차 API (Sequential API)
  - 특정 기능(프로토콜)을 수행하기 위해 정해진 순서대로 호출하는 일련의 API를 말한다. 각 API는 순차적으로 호출되어야 하며, 순서가 잘못될 경우 제대로 동작하지 않을 수 있다.
  - 그러나 일부 프로토콜에서는 같은 호출 순서를 가진 API가 존재할 수 있으며, 이 경우 하나의 API를 선택하여 호출할 수 있다.
- 단일 호출 API (Single Call API)
  - 일반적인 REST API처럼 순서에 관계없이 독립적으로 호출 가능한 API를 의미한다.
- 표준 API (Standard API)
  - API 문서에서 명확하게 정의된 API로, 모든 구현체에서 일관된 방식으로 제공되어야 한다. 표준 API는 시스템 간 상호 운용성을 보장하며, 사전에 정의된 스펙에 따라 동작해야 한다.
- 비표준 API (Non-Standard API)
  - 구현체마다 필요에 따라 다르게 정의되거나 커스터마이징될 수 있는 API이다. 본 문서에서 제공하는 비표준 API는 한 가지 예시일 뿐이며, 각 구현체에 맞춰 다르게 구현될 수 있다. 이 경우, 구현체별 별도의 문서화가 필요하다.

<div style="page-break-after: always; margin-top: 50px;"></div>

## 3. API 목록

### 3.1. 순차 API
DID Ledger Service는 현재 특정 기능을 수행하기 위한 프로토콜이 정의되어 있지 않으며, 따라서 순차 API도 제공하지 않는다.

<div style="page-break-after: always; margin-top: 40px;"></div>

### 3.2. 단일호출 API

| API                    | URL                           | Description                    | 표준API |
| ---------------------- | ----------------------------- | ------------------------------ | ------- |
| `register-diddoc`      | /api/v1/diddoc/register       | DID Document 원장 등록        | N       |
| `get-diddoc`           | /api/v1/diddoc                | DID Document 원장 조회        | N       |
| `update-diddoc`        | /api/v1/diddoc/update         | DID Document 원장 수정        | N       |
| `register-vcmeta`      | /api/v1/vcmeta/register       | VC Metadata 원장 등록         | N       |
| `get-vcmeta`           | /api/v1/vcmeta                | VC Metadata 원장 조회         | N       |

<div style="page-break-after: always; margin-top: 50px;"></div>

## 4. 단일 호출 API

단일 호출 API는 특정 기능을 수행하는 하나의 독립된 API이다.
따라서 순서대로 호출해야 하는 API의 집단인 순차 API(aka, 프로토콜)이 아니므로 프로토콜 번호가 부여되지 않는다.
DID Ledger Service가 제공하는 단일 호출 API 목록은 아래 표와 같다.

| API                    | URL                           | Description                    | 표준API |
| ---------------------- | ----------------------------- | ------------------------------ | ------- |
| `register-diddoc`      | /api/v1/diddoc/register       | DID Document 원장 등록        | N       |
| `get-diddoc`           | /api/v1/diddoc                | DID Document 원장 조회        | N       |
| `update-diddoc`        | /api/v1/diddoc/update         | DID Document 원장 수정        | N       |
| `register-vcmeta`      | /api/v1/vcmeta/register       | VC Metadata 원장 등록         | N       |
| `get-vcmeta`           | /api/v1/vcmeta                | VC Metadata 원장 조회         | N       |

■ Authorization

DID Ledger Service API는 호출자의 권한을 확인하는 인증 메커니즘을 포함하고 있다.
인증 및 권한 부여는 다음을 통해 처리된다:

- 서비스 간 통신을 위한 API Key 기반 인증
- DID 작업을 위한 디지털 서명 검증
- 관리 기능을 위한 역할 기반 접근 제어

### 4.1. DID Document 등록

새로운 DID Document를 블록체인 원장에 등록한다.

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
  "didDoc": "string",           // Multibase 인코딩된 DID Document
  "signature": "string",        // DID Document의 디지털 서명
  "invokerNonce": "string"      // 트랜잭션 고유 nonce
}
```

<div style="page-break-after: always; margin-top: 30px;"></div>

#### 4.1.2. Response

**■ Process**
1. DID Document 형식 및 서명 검증
2. 블록체인 원장에 DID Document 등록
3. 트랜잭션 결과 반환

**■ Status 200 - Success**

```json
{
  "txId": "string",             // 블록체인 트랜잭션 ID
  "did": "string",              // 등록된 DID
  "status": "SUCCESS"
}
```

**■ Status 400 - Client error**

| Code         | Description                      |
| ------------ | -------------------------------- |
| SRVLDG00100  | 유효하지 않은 DID Document 형식  |
| SRVLDG00101  | 유효하지 않은 서명               |
| SRVLDG00102  | 이미 존재하는 DID                |

**■ Status 500 - Server error**

| Code         | Description                      |
| ------------ | -------------------------------- |
| SRVLDG00200  | DID Document 등록에 실패했습니다 |

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

### 4.2. DID Document 조회

블록체인 원장에서 DID Document를 조회한다.

| Item          | Description      | Remarks |
| ------------- | ---------------- | ------- |
| Method        | `GET`            |         |
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
| + `did` | DID 식별자  |         |

**■ HTTP Body**

N/A

<div style="page-break-after: always; margin-top: 30px;"></div>

#### 4.2.2. Response

**■ Process**
1. DID로 블록체인 원장에서 DID Document 조회

**■ Status 200 - Success**

```json
{
  "didDoc": "string",           // Multibase 인코딩된 DID Document
  "status": "ACTIVE",           // DID Document 상태
  "versionId": "string",        // 버전 식별자
  "created": "2025-06-12T10:00:00Z",
  "updated": "2025-06-12T10:00:00Z"
}
```

**■ Status 400 - Client error**

| Code         | Description              |
| ------------ | ------------------------ |
| SRVLDG00300  | 존재하지 않는 DID        |
| SRVLDG00301  | 유효하지 않은 DID 형식   |

**■ Status 500 - Server error**

| Code         | Description                   |
| ------------ | ----------------------------- |
| SRVLDG00400  | DID Document 조회에 실패했습니다 |

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

### 4.3. DID Document 수정

블록체인 원장에서 기존 DID Document를 수정한다.

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
  "did": "string",              // 수정할 DID
  "didDoc": "string",           // 수정된 DID Document (multibase 인코딩)
  "signature": "string",        // 디지털 서명
  "invokerNonce": "string"      // 트랜잭션 고유 nonce
}
```

#### 4.3.2. Response

**■ Process**
1. DID Document 및 서명 검증
2. 블록체인 원장에서 DID Document 수정
3. 트랜잭션 결과 반환

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
| SRVLDG00500  | 존재하지 않는 DID                |
| SRVLDG00501  | 유효하지 않은 서명               |
| SRVLDG00502  | 권한 없는 수정 시도              |

**■ Status 500 - Server error**

| Code         | Description                      |
| ------------ | -------------------------------- |
| SRVLDG00600  | DID Document 수정에 실패했습니다 |

<div style="page-break-after: always; margin-top: 40px;"></div>

### 4.4. VC Metadata 등록

VC(Verifiable Credential) Metadata를 블록체인 원장에 등록한다.

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
  "vcId": "string",             // VC 식별자
  "vcMeta": "string",           // Multibase 인코딩된 VC Metadata
  "signature": "string",        // 디지털 서명
  "invokerNonce": "string"      // 트랜잭션 고유 nonce
}
```

#### 4.4.2. Response

**■ Process**
1. VC Metadata 및 서명 검증
2. 블록체인 원장에 VC Metadata 등록
3. 트랜잭션 결과 반환

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
| SRVLDG00700  | 유효하지 않은 VC Metadata 형식   |
| SRVLDG00701  | 이미 존재하는 VC                 |
| SRVLDG00702  | 유효하지 않은 서명               |

**■ Status 500 - Server error**

| Code         | Description                      |
| ------------ | -------------------------------- |
| SRVLDG00800  | VC Metadata 등록에 실패했습니다  |

<div style="page-break-after: always; margin-top: 40px;"></div>

### 4.5. VC Metadata 조회

블록체인 원장에서 VC Metadata를 조회한다.

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
| + `vcId` | VC 식별자   |         |

**■ HTTP Body**

N/A

#### 4.5.2. Response

**■ Process**
1. VC ID로 블록체인 원장에서 VC Metadata 조회

**■ Status 200 - Success**

```json
{
  "vcId": "string",
  "vcMeta": "string",           // Multibase 인코딩된 VC Metadata
  "status": "ACTIVE",
  "created": "2025-06-12T10:00:00Z"
}
```

**■ Status 400 - Client error**

| Code         | Description             |
| ------------ | ----------------------- |
| SRVLDG00900  | 존재하지 않는 VC        |
| SRVLDG00901  | 유효하지 않은 VC ID 형식 |

**■ Status 500 - Server error**

| Code         | Description                   |
| ------------ | ----------------------------- |
| SRVLDG01000  | VC Metadata 조회에 실패했습니다 |

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
