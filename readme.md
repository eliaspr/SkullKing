# SkullKing Online

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=hoerner-it_skullking&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=hoerner-it_skullking) [![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=hoerner-it_skullking&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=hoerner-it_skullking) [![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=hoerner-it_skullking&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=hoerner-it_skullking) [![Bugs](https://sonarcloud.io/api/project_badges/measure?project=hoerner-it_skullking&metric=bugs)](https://sonarcloud.io/summary/new_code?id=hoerner-it_skullking)

A digital recreation of the german card game [SkullKing](https://www.schmidtspiele.de/details/produkt/skull-king.html). The server is written in Java using the Spring framework and is accessed by the client using a standard web browser. Client-side logic is implemented in plain JavaScript and communication with the server is done via web socket.

## Getting started

Use the following command to clone a copy of the repository:

```shell
git clone https://github.com/eliaspr/SkullKing
```

Inside the `src` directory, run the following command to start a local instance of the SkullKing server:

```shell
gradlew bootRun
```

The server uses port `8080` by default. The images shown in the client application are stored in an Azure storage account so make sure the client browser can access and load images from `https://hoerneritpfs.blob.core.windows.net/skullking/...`.

## Data storage

The server stores all game session related data in memory only. Currently, there is no option to persist that data.

## Build & Deployment

The project contains a `Dockerfile` for simple deployment using Docker. The docker image can be built by running the following commands in the `src` directory:

```shell
gradlew bootJar
docker build -t skull-king .
```

For automated deployment, the project contains a `publish.ps1` script which detects version information, builds the docker image and pushes the image to the `hoernerit.azurecr.io` container registry. Run the script by executing the `publish.ps1` or `publish.bat` file.  

You can start an instance of the Docker image using the following command:

```shell
docker run -d --rm --name SkullKing -p 8080:8080 hoernerit.azurecr.io/skull-king:latest
```

> **Note**: Any action involving the `hoernerit.azurecr.io` container registry requires proper authentication. Run `docker login hoernerit.azurecr.io` and provide your credentials in order to pull or push the `skull-king` image.

Alternatively, use the following `docker-compose` template:

```yaml
version: '3.8'
services:
  SkullKing:
    image: 'hoernerit.azurecr.io/skull-king:latest'
    container_name: SkullKing
    ports:
      - '8080:8080'
```
