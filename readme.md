# SkullKing Online

A digital recreation of the german card game [SkullKing](https://www.schmidtspiele.de/details/produkt/skull-king.html). The server is written in Java using the Spring framework and is accessed by the client using a standard web browser. Client-side logic is implemented in plain JavaScript and communication with the server is done via web socket.

See a list of recent changes in [changelog.md](./changelog.md).

## Getting started

Use the following command to clone a copy of the repository:

```shell
git clone https://github.com/eliaspr/SkullKing
```

Next, run the following command to start a local instance of the SkullKing server:

```shell
gradlew bootRun
```

The server uses port `80` by default. The images shown in the client application are stored in an Azure storage account so make sure the client browser can access and load images from `https://skull-king-assets.fra1.digitaloceanspaces.com/...`.

## Data storage

The server stores all game session related data in memory only. Currently, there is no option to persist that data.

## Build & Deployment

The project contains a `Dockerfile` for simple deployment using Docker. The docker image can be built by running the following commands:

```shell
gradlew bootJar
docker build -t skull-king .
```

For automated build and release, the project contains a `publish.ps1` script which detects version information, builds the docker image and pushes the image to the specified container registry. Run the script by executing the `publish.ps1` file (make sure to specify the correct docker registry):

```shell
powershell.exe -ExecutionPolicy Unrestricted ./publish.ps1 -DockerRegistry example.docker.io
```

You can start an instance of the Docker image using the following command (make sure to use the correct image name when pulling from a repository):

```shell
docker run -d --rm --name SkullKing -p 80:80 skull-king:latest
```

> **Note**: Any push/pull actions require authentication with the corresponding docker registry. Run `docker login` and provide your credentials in order to pull or push the `skull-king` image.

Alternatively, use the following `docker-compose` template:

```yaml
version: '3.8'
services:
  SkullKing:
    image: 'skull-king:latest'
    container_name: 'SkullKing'
    ports:
      - '80:80'
```
