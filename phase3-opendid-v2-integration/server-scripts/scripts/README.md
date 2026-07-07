# OpenDID PoC Scripts

이 폴더는 OpenDID PoC 환경을 한 번에 구성하고 실행하기 위한 스크립트를 포함합니다.

## 권장 실행 순서

처음부터 환경을 올릴 때는 아래 순서로 실행하면 됩니다.

1. `colima start`
2. `./install-postgres.sh`
3. `./reset-all.sh`

위 순서대로 실행하면 다음 작업이 일괄로 진행됩니다.

- Colima 기반 Docker 환경 시작
- PostgreSQL 설치 및 실행
- OpenDID 서버들이 사용할 데이터베이스 초기화
- 각 서버 실행
- 어드민 설정 작업 일괄 수행

## 빠른 시작

`server-scripts/scripts` 디렉터리에서 아래 순서로 실행합니다.

```bash
colima start
./install-postgres.sh
./reset-all.sh
```

`./reset-all.sh` 실행 중 기존 서버 중지 및 DB 삭제 여부를 묻는 확인 메시지가 나오면 `y`를 입력하면 됩니다.
서버 구동이 끝나면 어드민 설정을 진행하기 전에 사용할 IP를 입력하라는 안내가 나오며, 그때 환경에 맞는 IP를 입력하면 됩니다.

## `reset-all.sh`의 의미

`reset-all.sh`는 OpenDID PoC 환경을 한 번에 다시 구성하는 통합 스크립트입니다.

- 기존 서버 중지
- 기존 DB 삭제 여부 확인
- DB 삭제 및 재생성
- 서버 일괄 실행
- 어드민 설정 전 IP 입력
- 어드민 설정 작업 수행

단, `reset-all.sh` 자체가 PostgreSQL을 새로 설치하지는 않습니다. PostgreSQL 컨테이너 설치와 실행은 먼저 `./install-postgres.sh`로 끝내둔 상태여야 합니다.

즉, PostgreSQL 설치를 마친 뒤 서버 재구성, 실행, 환경설정을 한 번에 처리하고 싶을 때 사용하는 스크립트입니다.

## 나누어서 실행하는 방법

필요하면 전체를 `reset-all.sh`로 처리하지 않고, 단계별로 나누어서 실행할 수도 있습니다.

### 로컬 DB를 사용하는 경우

```bash
colima start
./install-postgres.sh
./setup-local-db.sh
./start-all.sh
./setup/setup-all.sh
```

위 방식은 다음처럼 사용할 수 있습니다.

- DB만 먼저 설치하고 싶은 경우
- 서버 기동과 어드민 설정을 분리하고 싶은 경우
- 서버 시작과 환경설정을 분리해서 진행하고 싶은 경우

이 순서에서 각 스크립트의 역할은 다음과 같습니다.

- `./setup-local-db.sh`: 로컬 PostgreSQL DB 생성, `.db-config` 저장, `configs/*/application.yml`의 DB 설정 반영
- `./start-all.sh`: 지갑/키/DID 생성, 설정 파일 갱신, 서버 일괄 실행
- `./setup/setup-all.sh`: 서버 실행 후 TA, Wallet, CA, Issuer, Verifier, Admin Policy 설정 수행

`./start-all.sh` 실행 중에는 서버 base URL 입력을 묻습니다.
`./setup/setup-all.sh` 실행 전에는 서버들이 정상적으로 떠 있어야 하며, 실행 시 어드민 설정에 사용할 IP를 묻습니다.

`./setup-server-urls.sh`는 여러 장비에 서버를 분산 배포하는 경우에만 별도로 사용합니다. 단일 장비에서 전체 서버를 같이 올리는 일반적인 흐름에서는 기본 단계가 아닙니다.

### 외부 DB를 사용하는 경우

외부 PostgreSQL을 사용할 때는 `./setup-local-db.sh` 대신 `./setup-external-db.sh`를 사용하면 됩니다.

```bash
./setup-external-db.sh
./start-all.sh
./setup/setup-all.sh
```

외부 DB를 쓰는 경우에는 PostgreSQL 설치를 위해 `colima start`를 할 필요는 없습니다.
다만 로컬 Docker 기반 구성 요소를 함께 사용할 계획이라면 Colima를 따로 올릴 수 있습니다.

## DB를 완전히 초기화하고 다시 시작해야 하는 경우

Colima 볼륨까지 포함해서 PostgreSQL 데이터를 완전히 비우고 다시 시작하려면 아래 순서로 진행합니다.

```bash
colima stop
colima delete
colima start
./install-postgres.sh
./reset-all.sh
```

이 경우 기존 Colima VM 안에 있던 DB 데이터도 함께 제거되므로, 완전히 깨끗한 상태에서 다시 시작할 수 있습니다.
이후 `./reset-all.sh` 실행 중 기존 서버 중지 및 DB 삭제 여부를 묻는 확인 메시지가 나오면 `y`를 입력하면 됩니다.
서버 구동이 끝나면 어드민 설정 전에 사용할 IP를 입력하라는 안내가 나오며, 그때 환경에 맞는 IP를 입력하면 됩니다.

## 참고

- `reset-all.sh`는 서버 재기동뿐 아니라 필요한 초기 설정 작업까지 한 번에 수행하는 용도로 사용합니다.
- Colima가 올라오지 않은 상태에서는 PostgreSQL 설치 및 서버 실행이 정상적으로 진행되지 않을 수 있습니다.
- 서버를 개별적으로 중지하거나 상태를 확인할 때는 `./stop-all.sh`, `./status.sh`, `./servers.sh`를 사용할 수 있습니다.
