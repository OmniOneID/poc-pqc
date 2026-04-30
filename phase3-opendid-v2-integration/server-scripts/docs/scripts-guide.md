# OpenDID PoC 환경 스크립트 사용 가이드

> 최종 수정: 2026-03-23

---

## 목차

1. [개요](#1-개요)
2. [디렉토리 구조](#2-디렉토리-구조)
3. [사전 준비](#3-사전-준비)
4. [데이터베이스 설정](#4-데이터베이스-설정)
5. [서버 URL 설정](#5-서버-url-설정)
6. [서버 시작/중지](#6-서버-시작중지)
7. [서버 관리](#7-서버-관리)
8. [자주 묻는 질문 (FAQ)](#8-자주-묻는-질문-faq)
9. [상황별 설정 가이드](#9-상황별-설정-가이드)
10. [서버별 자동 업데이트 설정 항목](#10-서버별-자동-업데이트-설정-항목)

---

## 1. 개요

OpenDID PoC 환경은 12개의 서버로 구성되며, 이를 쉽게 관리하기 위한 자동화 스크립트를 제공합니다.

### 주요 스크립트

| 스크립트 | 용도 | 실행 시점 |
|---------|------|----------|
| `install-postgres.sh` | PostgreSQL 설치 | 최초 1회 (로컬 DB 사용 시) |
| `setup-local-db.sh` | 로컬 DB 생성 및 설정 | 최초 1회 (로컬 DB 사용 시) |
| `setup-external-db.sh` | 외부 DB 연결 설정 | 최초 1회 (외부 DB 사용 시) |
| `setup-server-urls.sh` | **멀티 머신** 서버 URL 설정 | IP가 다른 경우 |
| `start-all.sh` | 전체 서버 시작 | 여러 번 |
| `stop-all.sh` | 전체 서버 중지 | 여러 번 |
| `status.sh` | 서버 상태 확인 | 여러 번 |
| `server.sh` | 개별 서버 관리 | 여러 번 |
| `servers.sh` | 여러 서버 선택 관리 | 여러 번 |

---

## 2. 디렉토리 구조

```
opendid-poc-env/
├── scripts/                         # 실행 스크립트
│   ├── install-postgres.sh          # PostgreSQL 설치
│   ├── setup-local-db.sh            # 로컬 DB 설정
│   ├── setup-external-db.sh         # 외부 DB 설정
│   ├── setup-server-urls.sh         # 멀티 머신 서버 URL 설정
│   ├── start-all.sh                 # 전체 서버 시작
│   ├── stop-all.sh                  # 전체 서버 중지
│   ├── status.sh                    # 서버 상태 확인
│   ├── server.sh                    # 개별 서버 관리
│   ├── servers.sh                   # 여러 서버 선택 관리
│   │
│   ├── postgres/                    # PostgreSQL 스크립트 (내부)
│   ├── tool/                        # 지갑 생성 도구 (내부)
│   ├── config/                      # 설정 업데이트 (내부)
│   └── server/                      # 서버 관리 라이브러리 (내부)
│
├── jars/                            # 서버 JAR 파일
│   ├── TA/                          # TAS 서버
│   ├── CA/                          # CAS 서버
│   ├── Issuer/
│   ├── Verifier/
│   ├── Wallet/
│   ├── OP/
│   ├── LSS/
│   ├── API/
│   ├── VerificationServer/
│   ├── Proxy/
│   ├── J-Verifier/
│   └── PORTAL/
│
├── configs/                         # 서버 설정 파일
│   ├── tas/application.yml
│   ├── cas/application.yml
│   ├── issuer/application.yml
│   ├── verifier/application.yml
│   ├── wallet/application.yml
│   ├── op/application.yml
│   ├── lss/application.yml
│   ├── api/application.yml
│   ├── verification-server/application.yml
│   ├── proxy/application.yml
│   ├── j-verifier/application.yml
│   └── portal/application.yml
│
├── logs/                            # 서버 로그
├── .db-config                       # DB 설정 (자동 생성)
├── .postgres-config                 # PostgreSQL 설정 (자동 생성)
├── .server-urls-config              # 멀티 머신 URL 설정 (자동 생성)
└── .initialized                     # 초기화 마커 (자동 생성)
```

---

## 3. 사전 준비

### 필수 소프트웨어

| 소프트웨어 | 최소 버전 | 확인 명령어 |
|-----------|----------|------------|
| Docker | 20.x 이상 | `docker --version` |
| Java (JDK/JRE) | 17 이상 | `java -version` |
| Bash | 3.x 이상 | `bash --version` |

### JAR 파일 준비

각 서버의 JAR 파일을 아래 경로에 위치시켜야 합니다.

| 서버 | JAR 저장 경로 | 포트 | DB 사용 |
|------|-------------|------|---------|
| TAS | `jars/TA/*.jar` | 4040 | ✅ |
| CAS | `jars/CA/*.jar` | 5151 | ✅ |
| Issuer | `jars/Issuer/*.jar` | 4242 | ✅ |
| Verifier | `jars/Verifier/*.jar` | 6060 | ✅ |
| Wallet | `jars/Wallet/*.jar` | 5252 | ✅ |
| OP | `jars/OP/*.jar` | 7272 | ✅ |
| LSS | `jars/LSS/*.jar` | 4141 | ✅ |
| API | `jars/API/*.jar` | 6363 | ❌ |
| VerificationServer | `jars/VerificationServer/*.jar` | 7171 | ❌ |
| Proxy | `jars/Proxy/*.jar` | 6262 | ❌ |
| J-Verifier | `jars/J-Verifier/*.jar` | 7070 | ✅ (verifier DB 공유) |
| PORTAL | `jars/PORTAL/*.jar` | 5050 | ❌ |

### 스크립트 실행 권한 부여

프로젝트 루트에서 한 번만 실행합니다.

```bash
chmod +x scripts/*.sh
```

---

## 4. 데이터베이스 설정

### 옵션 A: 로컬 PostgreSQL 사용 (권장)

**1단계: PostgreSQL 설치**

```bash
cd scripts
./install-postgres.sh
```

**실행 내용:**
- PostgreSQL 16 Docker 컨테이너 생성
- 컨테이너명과 이미지 입력 프롬프트
- 포트 5430 (기본값, 5432 충돌 방지)

**2단계: 데이터베이스 생성 및 설정**

```bash
./setup-local-db.sh
```

**실행 내용:**
- PostgreSQL 실행 확인
- 7개 데이터베이스 생성 (tas, cas, issuer, verifier, wallet, op, lss)
- `.db-config` 파일 저장
- `application.yml` 파일에 DB 설정 추가

**생성되는 데이터베이스:**

| DB 이름 | 사용 서버 |
|---------|----------|
| `tas` | TAS |
| `cas` | CAS |
| `issuer` | Issuer |
| `verifier` | Verifier, J-Verifier |
| `wallet` | Wallet |
| `op` | OP |
| `lss` | LSS |

> **참고:** API, VerificationServer, Proxy, PORTAL은 DB를 사용하지 않습니다.

---

### 옵션 B: 외부 PostgreSQL 사용

**사전 준비:**
- 외부 PostgreSQL 서버 준비
- 7개 데이터베이스 수동 생성 필요
- 접속 권한 확인

**1단계: 외부 DB 연결 설정**

```bash
cd scripts
./setup-external-db.sh
```

**입력 정보:**
- Database host (예: `192.168.1.100`)
- Database port (기본값: `5432`)
- Database username (기본값: `omn`)
- Database password

**실행 내용:**
- `.db-config` 파일 저장
- `application.yml` 파일에 DB 설정 추가

---

## 5. 서버 URL 설정

서버 URL 설정은 배포 환경에 따라 두 가지 방식으로 나뉩니다.

### 옵션 A: 단일 머신 (모든 서버가 같은 IP)

`start-all.sh` 실행 시 Step 2에서 자동으로 처리됩니다.
또는 별도로 실행:

```bash
cd scripts/config
./update-configs.sh
```

**입력 정보:**
- 서버 기본 URL 1개 (예: `http://10.0.1.10`)

**적용 내용:**
- 모든 서버의 `tas.url`, `lss.url` → `{BASE_URL}:{포트}`
- 모든 서버의 `springdoc.server.url` → `{BASE_URL}:{포트}`
- 지갑 파일 절대경로 업데이트

---

### 옵션 B: 멀티 머신 (AWS 등 서버마다 IP가 다른 경우)

서버가 여러 장비에 분산 배포된 경우 `setup-server-urls.sh`를 사용합니다.

```bash
cd scripts
./setup-server-urls.sh
```

**입력 정보:**

각 서버가 실행될 장비의 IP를 개별 입력합니다.

```
TAS      (포트 4040): 10.0.1.10
LSS      (포트 4141): 10.0.1.10
CAS      (포트 5151): 10.0.2.20
Issuer   (포트 4242): 10.0.2.20
Verifier (포트 6060): 10.0.3.30
Wallet   (포트 5252): 10.0.3.30
OP       (포트 7272): 10.0.4.40
API      (포트 6363): 10.0.4.40
```

> 같은 장비에 있는 서버는 동일한 IP를 입력합니다.

**적용 내용:**

| 서버 | 업데이트 항목 |
|------|-------------|
| TAS | `lss.url`, `springdoc.server.url` |
| LSS | `tas.url`, `springdoc.server.url` |
| CAS, Issuer, Verifier, OP, API | `tas.url`, `lss.url`, `springdoc.server.url` |
| Wallet | `lss.url`, `springdoc.server.url` |
| Proxy | `issuer.url` |

**설정 저장:**
- 입력값은 `.server-urls-config`에 저장되어 재실행 시 재사용됩니다.
- Enter 키로 기존 값을 유지할 수 있습니다.

**지갑 경로 업데이트:**
- `setup-server-urls.sh`는 지갑 경로를 업데이트하지 않습니다.
- 지갑 경로는 `scripts/config/update-configs.sh`를 별도 실행하거나 `start-all.sh`를 통해 업데이트됩니다.

---

## 6. 서버 시작/중지

### 전체 서버 시작

```bash
cd scripts
./start-all.sh
```

**실행 단계:**
1. DB 설정 확인 (`.db-config` 없으면 에러)
2. 지갑 및 DID 생성 (이미 존재하면 SKIP)
3. 서버 URL 설정
4. 전체 서버 의존성 순서로 기동

**서버 기동 순서:**

```
1.  LSS
2.  TAS
3.  LSS (재시작 - TAS DID 동기화)
4.  Issuer
5.  Verifier
6.  CAS
7.  Wallet
8.  OP
9.  API
10. VerificationServer
11. Proxy
12. J-Verifier
13. PORTAL
```

> **중요:** LSS는 TAS 기동 후 재시작하여 TAS DID를 동기화합니다.

---

### 전체 서버 중지

```bash
./stop-all.sh
```

모든 OpenDID 서버 프로세스를 종료합니다. PostgreSQL 컨테이너는 종료되지 않습니다.

---

### 서버 상태 확인

```bash
./status.sh
```

**출력 예시:**

```
============================================================
>> OpenDID PoC Server Status
============================================================
  lss                   [RUNNING]  PID: 12301
  tas                   [RUNNING]  PID: 12450
  issuer                [STOPPED]
  verifier              [STOPPED]
  cas                   [NO JAR ]  (JAR not found)
  wallet                [RUNNING]  PID: 13102
  op                    [STOPPED]
  api                   [RUNNING]  PID: 13210
  verification-server   [RUNNING]  PID: 13310
  proxy                 [STOPPED]
  j-verifier            [RUNNING]  PID: 13420
  portal                [STOPPED]
============================================================
```

| 상태 | 의미 |
|------|------|
| `[RUNNING]` | 서버 프로세스 실행 중 |
| `[STOPPED]` | 프로세스 없음 (JAR은 존재) |
| `[NO JAR ]` | JAR 파일을 찾을 수 없음 |

---

## 7. 서버 관리

### 개별 서버 관리 (`server.sh`)

```bash
./server.sh <action> <server_name>
```

**지원 서버 이름:**
```
lss  tas  issuer  verifier  cas  wallet  op
api  verification-server  proxy  j-verifier  portal
```

**예시:**

```bash
# 개별 서버 시작
./server.sh start tas
./server.sh start issuer

# 개별 서버 중지
./server.sh stop tas
./server.sh stop issuer

# 개별 서버 재시작
./server.sh restart lss
./server.sh restart issuer

# 개별 서버 상태 확인
./server.sh status tas
```

---

### 여러 서버 선택 관리 (`servers.sh`)

```bash
./servers.sh <action> <server1> [server2] [server3] ...
```

**예시:**

```bash
# 여러 서버 시작
./servers.sh start tas issuer verifier
./servers.sh start lss cas wallet

# 여러 서버 중지
./servers.sh stop tas issuer verifier

# 여러 서버 재시작
./servers.sh restart tas verifier

# 전체 서버 시작
./servers.sh start all

# 전체 서버 중지
./servers.sh stop all

# 전체 서버 상태 확인
./servers.sh status
```

---

## 8. 자주 묻는 질문 (FAQ)

### 지갑 비밀번호

기본 비밀번호: `omnioneopendid12!@`

커스텀 비밀번호 사용:

```bash
./start-all.sh your_custom_password
```

---

### Q. DB 설정을 잘못했어요. 어떻게 재설정하나요?

**로컬 DB:**
```bash
rm .db-config
./setup-local-db.sh
```

**외부 DB:**
```bash
rm .db-config
./setup-external-db.sh
```

---

### Q. PostgreSQL 컨테이너를 재설치하고 싶어요.

```bash
# 1. 기존 설정 삭제
rm .postgres-config

# 2. PostgreSQL 재설치
./install-postgres.sh

# 3. DB 재설정
./setup-local-db.sh
```

---

### Q. 서버 URL을 변경하고 싶어요.

**단일 머신:**
```bash
# 1. 초기화 마커 삭제
rm .initialized

# 2. start-all.sh 실행 시 URL 재입력
./start-all.sh
```

**멀티 머신 (AWS 등):**
```bash
./setup-server-urls.sh
# 변경할 서버의 IP만 새로 입력하고, 나머지는 Enter로 유지
```

---

### Q. 특정 서버만 재시작하고 싶어요.

```bash
./server.sh restart tas
./server.sh restart lss
```

---

### Q. 서버 로그를 실시간으로 보고 싶어요.

```bash
tail -f logs/tas.log
tail -f logs/lss.log

# 여러 서버 동시에
tail -f logs/*.log
```

---

### Q. LSS를 TAS 이후에 재시작하는 이유는 무엇인가요?

TAS가 기동되면서 자신의 DID를 LSS에 등록합니다. LSS는 이 등록 내용을 반영하기 위해 TAS 기동 완료 후 한 번 재시작이 필요합니다. `start-all.sh`는 이 순서를 자동으로 처리합니다.

---

### Q. `[NO JAR]`가 표시돼요.

해당 서버의 JAR 파일이 지정된 폴더에 없다는 의미입니다. [사전 준비](#3-사전-준비) 섹션을 참고해 JAR 파일을 올바른 위치에 복사하세요.

---

### Q. 여러 서버를 동시에 재시작하고 싶어요.

```bash
./servers.sh restart tas issuer verifier
```

> **참고:** `restart all`은 지원하지 않습니다. 전체 재시작이 필요할 경우:
> ```bash
> ./stop-all.sh && ./start-all.sh
> ```

---

### Q. 데이터베이스 포트를 변경하고 싶어요.

**로컬 DB 사용 시:**

PostgreSQL 설치 전에 환경변수 설정:

```bash
export POSTGRES_PORT=5433
./install-postgres.sh
./setup-local-db.sh
```

**외부 DB 사용 시:**

`setup-external-db.sh` 실행 시 원하는 포트 입력

---

### Q. HTTPS를 사용하고 싶어요.

`start-all.sh` 실행 시 URL 입력에서:

```bash
Enter server base URL: https://example.com
```

모든 서버 URL이 HTTPS로 설정됩니다.

---

### Q. DB 설정을 완료했는데 start-all.sh가 에러를 내요.

`.db-config` 파일이 있는지 확인:

```bash
cat .db-config
```

없거나 손상되었다면 재설정:

```bash
rm .db-config
./setup-local-db.sh  # 또는 ./setup-external-db.sh
```

---

## 9. 상황별 설정 가이드

어떤 상황인지에 따라 실행할 스크립트와 입력 항목이 다릅니다.

---

### 상황 1: 처음 설치 — 로컬 DB, 서버 1대

모든 서버가 같은 장비에서 실행되고, DB도 로컬에 설치하는 경우입니다.

**실행 순서:**

```bash
cd scripts
./install-postgres.sh    # 1단계
./setup-local-db.sh      # 2단계
./start-all.sh           # 3단계
```

**각 스크립트에서 입력하는 것:**

| 스크립트 | 입력 항목 | 설명 |
|---------|---------|------|
| `install-postgres.sh` | 컨테이너명, 이미지명, 포트 | 기본값 있음, Enter로 건너뜀 |
| `setup-local-db.sh` | Spring 프로파일 | 기본값 `dev` |
| `start-all.sh` | 서버 기본 URL | 예: `http://10.0.1.10` |

**`start-all.sh`에서 URL을 입력하면:**
- 모든 서버 간 통신 URL (`tas.url`, `lss.url` 등) 자동 설정
- Swagger UI 주소 자동 설정

---

### 상황 2: 처음 설치 — 외부 DB, 서버 1대

AWS RDS 등 외부 DB를 사용하고, 서버는 1대에서 실행하는 경우입니다.

**실행 순서:**

```bash
cd scripts
./setup-external-db.sh   # 1단계
./start-all.sh           # 2단계
```

**각 스크립트에서 입력하는 것:**

| 스크립트 | 입력 항목 | 예시 |
|---------|---------|------|
| `setup-external-db.sh` | DB 호스트 | `mydb.rds.amazonaws.com` |
| | DB 포트 | `5432` |
| | DB 사용자명 | `omn` |
| | DB 비밀번호 | `omn` |
| | Spring 프로파일 | `dev` |
| `start-all.sh` | 서버 기본 URL | `http://10.0.1.10` |

---

### 상황 3: 처음 설치 — 외부 DB, 서버 여러 대 (AWS 등)

서버들이 각기 다른 장비(IP)에 분산 배포되는 경우입니다.

**실행 순서:**

```bash
cd scripts
./setup-external-db.sh   # 1단계: DB 설정
./setup-server-urls.sh   # 2단계: 서버별 URL 설정
./start-all.sh           # 3단계: 서버 시작
```

**각 스크립트에서 입력하는 것:**

| 스크립트 | 입력 항목 | 설명 |
|---------|---------|------|
| `setup-external-db.sh` | DB 호스트, 포트, 사용자명, 비밀번호, 프로파일 | 위 상황 2와 동일 |
| `setup-server-urls.sh` | 각 서버가 실행될 장비의 IP | 8개 서버별로 각각 입력 |
| `start-all.sh` | 서버 기본 URL (지갑 경로 업데이트용) | 아무 서버의 IP 입력 |

**`setup-server-urls.sh` 입력 예시 (장비 4대인 경우):**

```
TAS      (포트 4040): 10.0.1.10   ← 1번 장비
LSS      (포트 4141): 10.0.1.10   ← 1번 장비
Issuer   (포트 4242): 10.0.2.20   ← 2번 장비
CAS      (포트 5151): 10.0.2.20   ← 2번 장비
Verifier (포트 6060): 10.0.3.30   ← 3번 장비
Wallet   (포트 5252): 10.0.3.30   ← 3번 장비
OP       (포트 7272): 10.0.4.40   ← 4번 장비
API      (포트 6363): 10.0.4.40   ← 4번 장비
```

이렇게 입력하면 각 서버의 `application.yml`에 올바른 URL이 자동으로 기록됩니다.

---

### 상황 4: IP나 URL을 바꿔야 하는 경우

**서버 1대 (단일 머신):**

```bash
rm .initialized          # 기존 URL 초기화
./start-all.sh           # 새 URL 입력
```

**서버 여러 대 (멀티 머신):**

```bash
cd scripts
./setup-server-urls.sh   # IP가 바뀐 서버만 새로 입력, 나머지는 Enter
```

---

### 상황 5: DB 설정을 바꿔야 하는 경우

```bash
cd scripts
./setup-external-db.sh   # 또는 ./setup-local-db.sh
# "Do you want to reconfigure? yes" 입력 후 새 값 입력
```

기존 설정이 있어도 `yes`를 입력하면 재설정할 수 있습니다.

---

## 10. 서버별 자동 업데이트 설정 항목


각 서버의 `application.yml`에서 스크립트가 자동으로 업데이트하는 설정 항목을 정리합니다.

> **전제 조건:** 스크립트는 설정 파일을 **생성하지 않습니다**. `application.yml`이 미리 존재해야 하며, 존재하는 키만 업데이트합니다.

### 업데이트 시점

| 스크립트 | 실행 시점 | 업데이트 내용 |
|---------|----------|-------------|
| `setup-local-db.sh` / `setup-external-db.sh` | 최초 1회 | DB 접속 정보, Spring 프로파일 |
| `update-configs.sh` (= `start-all.sh` Step 2) | 단일 머신 | 지갑 경로, 서버 URL, springdoc URL |
| `setup-server-urls.sh` | 멀티 머신 | tas.url, lss.url, springdoc URL |

---

### TAS (포트 4040)

| 설정 키 | 단일 머신 | 멀티 머신 |
|--------|---------|---------|
| `spring.datasource.*` | `setup-local/external-db.sh` | `setup-local/external-db.sh` |
| `wallet.file-path` | `update-configs.sh` | `update-configs.sh` |
| `lss.url` | `update-configs.sh` → `{BASE_URL}:4141` | `setup-server-urls.sh` |
| `springdoc.server.url` | `update-configs.sh` → `{BASE_URL}:4040` | `setup-server-urls.sh` |

---

### CAS (포트 5151)

| 설정 키 | 단일 머신 | 멀티 머신 |
|--------|---------|---------|
| `spring.datasource.*` | `setup-local/external-db.sh` | `setup-local/external-db.sh` |
| `wallet.file-path` | `update-configs.sh` | `update-configs.sh` |
| `tas.url` | `update-configs.sh` → `{BASE_URL}:4040` | `setup-server-urls.sh` |
| `lss.url` | `update-configs.sh` → `{BASE_URL}:4141` | `setup-server-urls.sh` |
| `springdoc.server.url` | `update-configs.sh` → `{BASE_URL}:5151` | `setup-server-urls.sh` |

---

### Issuer (포트 4242)

| 설정 키 | 단일 머신 | 멀티 머신 |
|--------|---------|---------|
| `spring.datasource.*` | `setup-local/external-db.sh` | `setup-local/external-db.sh` |
| `wallet.file-path` | `update-configs.sh` | `update-configs.sh` |
| `tas.url` | `update-configs.sh` → `{BASE_URL}:4040` | `setup-server-urls.sh` |
| `lss.url` | `update-configs.sh` → `{BASE_URL}:4141` | `setup-server-urls.sh` |
| `springdoc.server.url` | `update-configs.sh` → `{BASE_URL}:4242` | `setup-server-urls.sh` |

---

### Verifier (포트 6060)

| 설정 키 | 단일 머신 | 멀티 머신 |
|--------|---------|---------|
| `spring.datasource.*` | `setup-local/external-db.sh` | `setup-local/external-db.sh` |
| `wallet.file-path` | `update-configs.sh` | `update-configs.sh` |
| `tas.url` | `update-configs.sh` → `{BASE_URL}:4040` | `setup-server-urls.sh` |
| `lss.url` | `update-configs.sh` → `{BASE_URL}:4141` | `setup-server-urls.sh` |
| `springdoc.server.url` | `update-configs.sh` → `{BASE_URL}:6060` | `setup-server-urls.sh` |

---

### Wallet (포트 5252)

| 설정 키 | 단일 머신 | 멀티 머신 |
|--------|---------|---------|
| `spring.datasource.*` | `setup-local/external-db.sh` | `setup-local/external-db.sh` |
| `wallet.file-path` | `update-configs.sh` | `update-configs.sh` |
| `lss.url` | `update-configs.sh` → `{BASE_URL}:4141` | `setup-server-urls.sh` |
| `springdoc.server.url` | `update-configs.sh` → `{BASE_URL}:5252` | `setup-server-urls.sh` |

---

### OP (포트 7272)

| 설정 키 | 단일 머신 | 멀티 머신 |
|--------|---------|---------|
| `spring.datasource.*` | `setup-local/external-db.sh` | `setup-local/external-db.sh` |
| `wallet.file-path` | `update-configs.sh` | `update-configs.sh` |
| `tas.url` | `update-configs.sh` → `{BASE_URL}:4040` | `setup-server-urls.sh` |
| `lss.url` | `update-configs.sh` → `{BASE_URL}:4141` | `setup-server-urls.sh` |
| `springdoc.server.url` | `update-configs.sh` → `{BASE_URL}:7272` | `setup-server-urls.sh` |

---

### LSS (포트 4141)

| 설정 키 | 단일 머신 | 멀티 머신 |
|--------|---------|---------|
| `spring.datasource.*` | `setup-local/external-db.sh` | `setup-local/external-db.sh` |
| `tas.url` | `update-configs.sh` → `{BASE_URL}:4040` | `setup-server-urls.sh` |
| `springdoc.server.url` | `update-configs.sh` → `{BASE_URL}:4141` | `setup-server-urls.sh` |

---

### API (포트 6363)

| 설정 키 | 단일 머신 | 멀티 머신 |
|--------|---------|---------|
| `spring.profiles.active` | `setup-local/external-db.sh` (기본: `dev`) | `setup-local/external-db.sh` |
| `tas.url` | `update-configs.sh` → `{BASE_URL}:4040` | `setup-server-urls.sh` |
| `lss.url` | `update-configs.sh` → `{BASE_URL}:4141` | `setup-server-urls.sh` |
| `springdoc.server.url` | `update-configs.sh` → `{BASE_URL}:6363` | `setup-server-urls.sh` |

---

### VerificationServer (포트 7171)

| 설정 키 | 스크립트 | 값 |
|--------|---------|---|
| `resolver.api-gateway.url` | `update-configs.sh` | 호스트 부분 교체 |

---

### Proxy (포트 6262)

| 설정 키 | 단일 머신 | 멀티 머신 |
|--------|---------|---------|
| `issuer.url` | `update-configs.sh` | `setup-server-urls.sh` |

---

### J-Verifier (포트 7070)

| 설정 키 | 스크립트 | 값 |
|--------|---------|---|
| `spring.datasource.*` | `setup-local/external-db.sh` | DB: `verifier` (Verifier와 공유) |
| `resolver.api-gateway.url` | `update-configs.sh` | 호스트 부분 교체 |
| `verification.base-url` | `update-configs.sh` | 호스트 부분 교체 |

> **참고:** `spring.profiles.active`는 `jpa`로 고정되어 있으며 스크립트가 변경하지 않습니다.

---

### PORTAL (포트 5050)

| 설정 키 | 스크립트 | 값 |
|--------|---------|---|
| `server.port` | 수동 설정 | `5050` |

---

## 요약

### 최초 설치 (1회만)

**로컬 DB, 단일 머신:**
```bash
cd scripts
./install-postgres.sh      # PostgreSQL 설치
./setup-local-db.sh        # DB 생성 및 설정
./start-all.sh             # 서버 시작 (URL 설정 포함)
```

**외부 DB, 단일 머신:**
```bash
cd scripts
./setup-external-db.sh     # 외부 DB 설정
./start-all.sh             # 서버 시작 (URL 설정 포함)
```

**외부 DB, 멀티 머신 (AWS 등):**
```bash
cd scripts
./setup-external-db.sh     # 외부 DB 설정
./setup-server-urls.sh     # 서버별 IP 입력
./start-all.sh             # 서버 시작
```

---

### 일상 사용

```bash
cd scripts
./start-all.sh              # 서버 시작
./stop-all.sh               # 서버 중지
./status.sh                 # 상태 확인
./server.sh restart tas     # 개별 서버 재시작
```

---

**문의 및 피드백:**
이슈가 있으면 개발팀에 문의하세요.
