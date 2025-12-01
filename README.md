# IPAM ETL Application

A Camel Quarkus application designed to ingest, normalize, and persist IP Address Management (IPAM) data from various sources into a standardized MongoDB format.

## Overview

This application acts as an ETL (Extract, Transform, Load) pipeline using **Camel YAML DSL**. It monitors a specific directory for CSV files from different IPAM providers (BlueCat, Infoblox, and others), parses them according to their specific formats, normalizes the data into a common structure, and stores the results in a MongoDB database. **For unrecognized formats, it leverages a Granite LLM to intelligently parse and extract data.**

### Key Features

*   **Camel YAML DSL:** Routes are defined declaratively in `src/main/resources/routes/ipam.yaml`.
*   **Multi-Source Support:**
    *   **BlueCat:** Parses CSV exports containing address records (`A`, `AAAA`), filtering out deleted entries.
    *   **Infoblox:** Handles complex multi-section CSV files containing both `NETWORK` and `HOSTRECORD` definitions.
    *   **Other:** Parses generic CSV formats mapping IP, Hostname, Status, and MAC address.
    *   **AI Fallback:** Uses the `granite-3.1-8b-instruct` model to parse unrecognized CSV formats.
*   **Standardized Data Model:** All inputs are converted into a unified `IpamRecord` JSON format.
*   **Recursive Polling:** Automatically detects and processes files in nested directories within `src/main/resources/samples`.
*   **MongoDB Persistence:** Efficiently stores normalized records using Camel's MongoDB component.

## Prerequisites

*   Java 21
*   Maven (wrapper provided)
*   MongoDB instance (running locally or accessible via network)

## Configuration

The application is configured via `src/main/resources/application.properties`.

### Database Connection
By default, the application connects to a local MongoDB instance:
```properties
quarkus.mongodb.connection-string=mongodb://localhost:27017
quarkus.mongodb.database=ipam_db
```

### Output Endpoint
The destination for processed records is configurable:
```properties
ipam.output.endpoint=mongodb:camelMongoClient?database=ipam_db&collection=ipam_records&operation=insert
```

### LLM Configuration
The application uses a Granite model for parsing unknown formats. Configure the endpoint in `application.properties`:
```properties
quarkus.rest-client."llm-api".url=https://<your-llm-endpoint>
quarkus.rest-client."llm-api".connect-timeout=240000
quarkus.rest-client."llm-api".read-timeout=240000
```

## Running the Application

### Development Mode
Run the application in dev mode for live coding and testing:
```bash
./mvnw quarkus:dev
```

### Building the Application
Package the application into a runnable JAR:
```bash
./mvnw clean package
```
Run the packaged JAR:
```bash
java -jar target/quarkus-app/quarkus-run.jar
```

## Input Data Formats

The application determines the parser based on the parent directory of the input file:

*   `.../bluecat/*.csv` -> BlueCat Parser
*   `.../infoblox/*.csv` -> Infoblox Parser
*   `.../other/*.csv` -> Generic Parser
*   **Any other directory** -> AI Parser (Granite)

## Testing

Integration tests are provided in `IpamRouteTest.java`. These tests verify the parsing logic by mocking the database output.
```bash
./mvnw test
```

## Development Environment

This application is developed using:
*   **OpenShift Dev Spaces**
*   **Roo Code extension**
*   **Gemini Pro 3** as the LLM backend for Roo
