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

Open DID Ledger Service Server Installation And Operation Guide
==

- Date: 2025-06-12
- Version: v2.0.0

Table of Contents
==

- [1. Introduction](#1-introduction)
  - [1.1. Overview](#11-overview)
  - [1.2. Ledger Service Server Definition](#12-ledger-service-server-definition)
  - [1.3. System Requirements](#13-system-requirements)
- [2. Prerequisites](#2-prerequisites)
  - [2.1. Git Installation](#21-git-installation)
- [3. Cloning Source Code from GitHub](#3-cloning-source-code-from-github)
  - [3.1. Source Code Cloning](#31-source-code-cloning)
  - [3.2. Directory Structure](#32-directory-structure)
- [4. Server Execution Methods](#4-server-execution-methods)
  - [4.1. Running with IntelliJ IDEA (Gradle Support)](#41-running-with-intellij-idea-gradle-support)
    - [4.1.1. IntelliJ IDEA Installation and Setup](#411-intellij-idea-installation-and-setup)
    - [4.1.2. Opening Project in IntelliJ](#412-opening-project-in-intellij)
    - [4.1.3. Gradle Build](#413-gradle-build)
    - [4.1.4. Server Execution](#414-server-execution)
    - [4.1.5. Server Configuration](#415-server-configuration)
  - [4.2. Running with Console Commands](#42-running-with-console-commands)
    - [4.2.1. Gradle Build Commands](#421-gradle-build-commands)
    - [4.2.2. Server Execution Method](#422-server-execution-method)
    - [4.2.3. Server Configuration Method](#423-server-configuration-method)
  - [4.3. Running with Docker](#43-running-with-docker)
- [5. Configuration Guide](#5-configuration-guide)
  - [5.1. application.yml](#51-applicationyml)
    - [5.1.1. Spring Basic Configuration](#511-spring-basic-configuration)
    - [5.1.2. Jackson Basic Configuration](#512-jackson-basic-configuration)
    - [5.1.3. Server Configuration](#513-server-configuration)
  - [5.2. application-logging.yml](#52-application-loggingyml)
    - [5.2.1. Logging Configuration](#521-logging-configuration)
  - [5.3. application-database.yml](#53-application-databaseyml)
    - [5.3.1. Database Integration Configuration](#531-database-integration-configuration)
- [6. Profile Configuration and Usage](#6-profile-configuration-and-usage)
  - [6.1. Profile Overview (`sample`, `dev`)](#61-profile-overview-sample-dev)
    - [6.1.1. `sample` Profile](#611-sample-profile)
    - [6.1.2. `dev` Profile](#612-dev-profile)
  - [6.2. Profile Configuration Method](#62-profile-configuration-method)
    - [6.2.1. When Running Server with IDE](#621-when-running-server-with-ide)
    - [6.2.2. When Running Server with Console Commands](#622-when-running-server-with-console-commands)
    - [6.2.3. When Running Server with Docker](#623-when-running-server-with-docker)
- [7. Building and Running with Docker](#7-building-and-running-with-docker)
  - [7.1. Docker Image Build Method (`Dockerfile` Based)](#71-docker-image-build-method-dockerfile-based)
  - [7.2. Docker Image Execution](#72-docker-image-execution)
  - [7.3. Running with Docker Compose](#73-running-with-docker-compose)
    - [7.3.1. `docker-compose.yml` File Description](#731-docker-composeyml-file-description)
    - [7.3.2. Container Execution and Management](#732-container-execution-and-management)
    - [7.3.3. Server Configuration Method](#733-server-configuration-method)

# 1. Introduction

## 1.1. Overview
This document provides a guide for the installation and operation of the DID Ledger Service server. It explains the installation process, configuration methods, and execution procedures step by step to enable users to efficiently install and operate the service.

For the complete installation guide of OpenDID, please refer to the [Open DID Installation Guide].

<br/>

## 1.2. Ledger Service Server Definition

The DID Ledger Service Server is an alternative service that utilizes PostgreSQL database to provide the same functionality as blockchain.

Key Features:
- DID Document creation, registration, and status changes
- VC Meta creation and VC status changes
- VC Schema registration and management
- ZKP Credential Schema and ZKP Credential Definition registration and management

Through this server, you can build a stable and efficient decentralized identity management system without the need for complex blockchain infrastructure.

<br/>

## 1.3. System Requirements
- **Java 21** or higher
- **Gradle 7.0** or higher
- **Docker** and **Docker Compose** (when using Docker)
- **PostgreSQL 13** or higher (when using dev profile)
- Minimum **2GB RAM** and **10GB disk space**

<br/>

# 2. Prerequisites

This chapter guides you through the prerequisites needed before installing the components of the Open DID project.

## 2.1. Git Installation

`Git` is a distributed version control system that tracks changes in source code and supports collaboration among multiple developers. Git is essential for managing and version controlling the source code of the Open DID project.

If installation is successful, you can check the Git version using the following command:

```bash
git --version
```

> **Reference Links**
> - [Git Installation Guide](https://docs.github.com/en/repositories/creating-and-managing-repositories/cloning-a-repository)

<br/>

# 3. Cloning Source Code from GitHub

## 3.1. Source Code Cloning

The `git clone` command clones source code from a remote repository hosted on GitHub to your local computer. Using this command, you can work with the entire source code and related files of the project locally. After cloning, you can proceed with necessary work within the repository and push changes back to the remote repository.

Open a terminal and execute the following command to copy the Ledger Service server repository to your local computer:
```bash
# Clone repository from Git repository
git clone https://github.com/OmniOneID/did-ledger-service-server.git

# Navigate to the cloned repository
cd did-ledger-service-server
```

> **Reference Links**
> - [Git Clone Guide](https://docs.github.com/en/repositories/creating-and-managing-repositories/cloning-a-repository)

<br/>

## 3.2. Directory Structure
The main directory structure of the cloned project is as follows:

```
did-ledger-service-server
├── CHANGELOG.md
├── CLA.md
├── CODE_OF_CONDUCT.md
├── CONTRIBUTING.md
├── dependencies-license.md
├── MAINTAINERS.md
├── README.md
├── RELEASE-PROCESS.md
├── SECURITY.md
├── docs
│   ├── api
│       └── LedgerService_API.md
│   ├── errorCode
│       └── LedgerService_ErrorCode.md
│   └── installation
│       └── OpenDID_LedgerService_InstallationAndOperation_Guide.md
├── gradle
├── libs
│   └── did-sdk-common-2.0.0.jar
│   └── did-blockchain-sdk-server-2.0.0.jar
│   └── did-core-sdk-server-2.0.0.jar
│   └── did-crypto-sdk-server-2.0.0.jar
│   └── did-datamodel-sdk-server-2.0.0.jar
│   └── did-wallet-sdk-server-2.0.0.jar
│   └── did-zkp-sdk-server-2.0.0.jar
├── sample
│   └── data
│       ├── diddoc
│       └── vcmeta
└── src
    └── main
        ├── java
        └── resources
            ├── config
            └── db
└── build.gradle
└── README.md
```

| Name                    | Description                                    |
| ----------------------- | ---------------------------------------------- |
| CHANGELOG.md            | Version-specific changes of the project       |
| CODE_OF_CONDUCT.md      | Code of conduct for contributors               |
| CONTRIBUTING.md         | Contribution guidelines and procedures         |
| dependencies-license.md | License information for project dependencies   |
| MAINTAINERS.md          | Guidelines for project maintainers            |
| RELEASE-PROCESS.md      | Procedures for releasing new versions          |
| SECURITY.md             | Security policy and vulnerability reporting    |
| docs                    | Documentation                                  |
| ┖ api                   | API guide documentation                        |
| ┖ errorCode             | Error codes and troubleshooting guides        |
| ┖ installation          | Installation and configuration guides         |
| gradle                  | Gradle build settings and scripts             |
| libs                    | External libraries and dependencies            |
| sample                  | Sample files                                   |
| src                     | Main source code directory                     |
| ┖ main/java             | Java source code                               |
| ┖ main/resources        | Configuration files and resources              |
| build.gradle            | Gradle build configuration file               |
| README.md               | Source code overview and guidance              |

<br/>

# 4. Server Execution Methods
This chapter guides you through three methods of running the server.

1. **Using IDE**: You can open the project in an Integrated Development Environment (IDE), set up execution configuration, and run the server directly. This method is useful for quickly testing code changes during development.

2. **Using console commands after build**: After building the project, you can run the server by executing the generated JAR file from the console using commands (`java -jar`). This method is mainly used when deploying the server or running it in production environments.

3. **Building with Docker**: You can build the server as a Docker image and run it as a Docker container. This method has the advantage of maintaining consistency across environments and facilitating deployment and scaling.

## 4.1. Running with IntelliJ IDEA (Gradle Support)

IntelliJ IDEA is an Integrated Development Environment (IDE) widely used for Java development. It supports build tools like Gradle, making project setup and dependency management very easy. Since the Open DID server is built using Gradle, you can easily set up the project and run the server in IntelliJ IDEA.

### 4.1.1. IntelliJ IDEA Installation and Setup
1. Install IntelliJ. (Refer to the link below for installation instructions)

> **Reference Links**
> - [IntelliJ IDEA Download](https://www.jetbrains.com/idea/download/)

### 4.1.2. Opening Project in IntelliJ
- Launch IntelliJ and select `File -> Open`. When the file selection window appears, select the root folder of the repository cloned in [3.1. Source Code Cloning](#31-source-code-cloning).
- When you open the project, the build.gradle file is automatically recognized.
- Gradle automatically downloads the necessary dependency files. Wait for this process to complete.

### 4.1.3. Gradle Build
- In IntelliJ IDEA's `Gradle` tab, execute `Tasks -> build -> build`.
- When the build completes successfully, the project is ready to run.

### 4.1.4. Server Execution
- In IntelliJ IDEA's Gradle tab, select and run Tasks -> application -> bootRun.
- Gradle automatically builds and runs the server.
- Check the console log for the "Started [ApplicationName] in [time] seconds" message to confirm that the server has started normally.

> **Note**
> - The Ledger Service server is initially configured with the sample profile.
> - When configured with the sample profile, the server starts while ignoring essential configurations (e.g., database). For details, please refer to [6. Profile Configuration and Usage](#6-profile-configuration-and-usage).

<br/>

### 4.1.5. Server Configuration
- The server needs to modify necessary configurations according to the deployment environment to ensure stable operation. For example, database connection information, port numbers, blockchain integration information, and other configuration components need to be adjusted for each environment.
- The server configuration files are located in the `src/main/resources/config` path.
- For detailed configuration methods, please refer to [5. Configuration Guide](#5-configuration-guide).

<br/>

## 4.2. Running with Console Commands

This section guides you through running the DID Ledger Service server using console commands. It explains the process of building the project using Gradle and running the server using the generated JAR file.

### 4.2.1. Gradle Build Commands

- Build the source using gradlew:
  ```shell
    # Navigate to the cloned repository
    cd did-ledger-service-server

    # Grant execution permission to Gradle Wrapper
    chmod 755 ./gradlew

    # Clean build the project (delete previous build files and build anew)
    ./gradlew clean build
  ```
  > Note
  > - gradlew is short for Gradle Wrapper, a script used to execute Gradle in a project. Even if Gradle is not installed locally, it automatically downloads and executes the version of Gradle specified by the project. This allows developers to build the project in the same environment regardless of whether Gradle is installed.

- Navigate to the built folder and confirm that the JAR file has been created:
    ```shell
      cd build/libs
      ls
    ```
- This command creates the `did-ledger-service-server-2.0.0.jar` file.

<br/>

### 4.2.2. Server Execution Method
Run the server using the built JAR file:

```bash
java -jar did-ledger-service-server-2.0.0.jar
```

> **Note**
> - The Ledger Service server is initially configured with the sample profile.
> - When configured with the sample profile, the server starts while ignoring essential configurations (e.g., database). For details, please refer to [6. Profile Configuration and Usage](#6-profile-configuration-and-usage).

<br/>

### 4.2.3. Server Configuration Method
- The server needs to modify necessary configurations according to the deployment environment to ensure stable operation. For example, database connection information, port numbers, blockchain integration information, and other configuration components need to be adjusted for each environment.
- The server configuration files are located in the `src/main/resources/config` path.
- For detailed configuration methods, please refer to [5. Configuration Guide](#5-configuration-guide).

<br/>

## 4.3. Running with Docker
- For Docker image building, configuration, execution, and other processes, please refer to [7. Building and Running with Docker](#7-building-and-running-with-docker) below.

<br/>

# 5. Configuration Guide

## 5.1. application.yml

- Role: The application.yml file defines the basic configuration of a Spring Boot application. Through this file, you can specify various environment variables such as application name, database settings, profile settings, etc., which significantly affect the application's behavior.

- Location: `src/main/resources/config`

### 5.1.1. Spring Basic Configuration
* `spring.application.name`:
  - Specifies the name of the application.
  - Purpose: Used by other services to identify this application.
  - Example: `Ledger-Service-Server`

* `spring.profiles.default`:
  - Defines the default profile to activate.
  - Purpose: Sets the default profile to use when no other profile is specified.
  - Supported profiles: sample, dev
  - Example: `sample`

* `spring.profiles.group.dev`: 🔒
  - Defines individual profiles included in the `dev` profile group.
  - Purpose: Groups and manages configurations used in development environments.
  - Profile file naming convention: Configuration files corresponding to each profile use the names defined in the group as-is. For example, the auth profile is written as application-auth.yml, and the database profile as application-database.yml.

* `spring.profiles.group.sample`: 🔒
  - Defines individual profiles included in the `sample` profile group.
  - Purpose: Groups and manages configurations used in sample environments.
  - Profile file naming convention: Configuration files corresponding to each profile use the names defined in the group as-is.

<br/>

### 5.1.2. Jackson Basic Configuration

Jackson is a JSON serialization/deserialization library used by default in Spring Boot. Through Jackson configuration, you can adjust the serialization method or format of JSON data, improving performance and efficiency during data transmission.

* `spring.jackson.default-property-inclusion`: 🔒
  - Sets not to serialize when property values are null.
  - Example: non_null

<br/>

### 5.1.3. Server Configuration
Server configuration defines the port number where the application will receive requests.

* `server.port`:
  - The port number where the application will run. The default value for the Ledger Service server port configuration is 8098.
  - Value: 8098

<br/>

## 5.2. application-logging.yml
- Role: Sets log groups and log levels. Through this configuration file, you can define log groups for specific packages or modules and specify log levels individually for each group.

- Location: `src/main/resources/config`

### 5.2.1. Logging Configuration
* `logging.level`:
  - Sets the log level.
  - By setting the level to debug, you can see all log messages at DEBUG level and above (INFO, WARN, ERROR, FATAL) for specified packages.

<br/>

## 5.3. application-database.yml

- Role: Configures database connection information to be used by the DID Ledger Service server. It includes configurations for integration with PostgreSQL database and also manages JPA and Liquibase settings.

- Location: `src/main/resources/config`

### 5.3.1. Database Integration Configuration

#### PostgreSQL Database Configuration

- `spring.datasource.url`:
  - Sets the PostgreSQL database connection URL.
  - Example: `jdbc:postgresql://localhost:5432/ledger_service`

- `spring.datasource.username`:
  - Sets the database username.
  - Example: `ledger_user`

- `spring.datasource.password`:
  - Sets the database password.
  - Example: `your_password_here`

- `spring.datasource.driver-class-name`:
  - Sets the PostgreSQL JDBC driver class name.
  - Example: `org.postgresql.Driver`

#### JPA Configuration

- `spring.jpa.hibernate.ddl-auto`:
  - Sets Hibernate's DDL auto-generation option.
  - Options: `none`, `validate`, `update`, `create`, `create-drop`
  - Example: `none` (none is recommended for production environments)

- `spring.jpa.show-sql`:
  - Sets whether to output SQL queries executed by JPA to the console.
  - Example: `false` (false is recommended for production environments)

#### Liquibase Configuration

- `spring.liquibase.change-log`:
  - Sets the path to the Liquibase change log file.
  - Example: `classpath:db/changelog/db.changelog-master.xml`

<br/>

# 6. Profile Configuration and Usage

## 6.1. Profile Overview (`sample`, `dev`)
The DID Ledger Service server supports two profiles, `sample` and `dev`, to run in various environments.

Each profile is designed to apply configurations appropriate for the corresponding environment. By default, the DID Ledger Service server is configured with the `sample` profile, which is designed to run the server independently without integration with external services such as databases or blockchains. The `sample` profile is suitable for API call testing, allowing developers to quickly verify the basic operation of the application. This profile returns fixed response data for all API calls, making it useful in initial development environments.

Sample API calls are written as JUnit tests, so you can refer to them when writing tests.

On the other hand, the `dev` profile is designed to perform actual operations. Using this profile enables testing and verification with real data. When the `dev` profile is activated, it integrates with actual external services such as databases and blockchains, allowing you to test application behavior in real environments.

### 6.1.1. `sample` Profile
The `sample` profile is designed to run the server independently without integration with external services (DB, blockchain, etc.). This profile is suitable for API call testing and allows developers to quickly verify the basic operation of the application. It returns fixed response data for all API calls, making it useful for initial development stages or functional testing. Since no integration with external systems is required, it provides an environment where you can run and test the server standalone.

### 6.1.2. `dev` Profile
The `dev` profile includes configurations suitable for development environments and is used on development servers. To use this profile, configuration for development environment databases and blockchain nodes is required.

## 6.2. Profile Configuration Method
This section explains how to change profiles for each execution method.

### 6.2.1. When Running Server with IDE
- **Configuration File Selection:** Select the `application.yml` file from the `src/main/resources/config` path.
- **Profile Specification:** Add the `--spring.profiles.active={profile}` option in the IDE's run configuration (Run/Debug Configurations) to activate the desired profile.
- **Configuration Application:** The corresponding configuration file is applied according to the activated profile.

### 6.2.2. When Running Server with Console Commands
- **Configuration File Selection:** Prepare profile-specific configuration files in the same directory as the built JAR file or in the path where configuration files are located.
- **Profile Specification:** Add the `--spring.profiles.active={profile}` option to the server execution command to activate the desired profile.

  ```bash
  java -jar build/libs/did-ledger-service-server-2.0.0.jar --spring.profiles.active={profile}
  ```

- **Configuration Application:** The corresponding configuration file is applied according to the activated profile.

### 6.2.3. When Running Server with Docker
- **Configuration File Selection:** When creating a Docker image, specify the configuration file path in the Dockerfile or mount external configuration files to the Docker container.
- **Profile Specification:** Set the `SPRING_PROFILES_ACTIVE` environment variable in the Docker Compose file or Docker execution command to specify the profile.

  ```yaml
  environment:
    - SPRING_PROFILES_ACTIVE={profile}
  ```

- **Configuration Application:** Configuration is applied according to the profile specified when running the Docker container.

You can flexibly change profile-specific configurations according to each method and easily apply configurations suitable for your project environment.

<br/>

# 7. Building and Running with Docker

## 7.1. Docker Image Build Method (`Dockerfile` Based)
Build the Docker image with the following command:

```bash
docker build -t did-ledger-service-server .
```

## 7.2. Docker Image Execution
Run the built image:

```bash
docker run -d -p 8098:8098 did-ledger-service-server
```

## 7.3. Running with Docker Compose

### 7.3.1. `docker-compose.yml` File Description
You can easily manage multiple containers using the `docker-compose.yml` file.

```yaml
version: '3'
services:
  app:
    image: did-ledger-service-server
    ports:
      - "8098:8098"
    volumes:
      - ${your-config-dir}:/app/config
    environment:
      - SPRING_PROFILES_ACTIVE=sample
    depends_on:
      - postgres
  
  postgres:
    image: postgres:13
    environment:
      POSTGRES_DB: ledger_service
      POSTGRES_USER: ledger_user
      POSTGRES_PASSWORD: your_password_here
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

### 7.3.2. Container Execution and Management
Run containers using Docker Compose with the following command:

```bash
docker-compose up -d
```

### 7.3.3. Server Configuration Method
In the above example, mount the `${your-config-dir}` directory to `/app/config` inside the container to share configuration files.
- If additional configuration is needed, you can change settings by adding separate property files to the mounted folder.
  - For example, add an `application.yml` file to `${your-config-dir}` and write the configurations to change in this file.
  - The `application.yml` file located in `${your-config-dir}` takes priority over the default configuration file.
- For detailed configuration, refer to [5. Configuration Guide](#5-configuration-guide).

[Open DID Installation Guide]: https://github.com/OmniOneID/did-release/blob/develop/release-V2.0.0.0/OpenDID_Installation_Guide-V2.0.0.0_ko.md