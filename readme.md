# SkullKing Online

A digital recreation of the german card game [SkullKing](https://www.schmidtspiele.de/details/produkt/skull-king.html). The server is written in Java using the Spring framework and is accessed by the client using a standard web browser. Client-side logic is implemented in plain JavaScript and communication with the server is done via web socket.

See a list of recent changes in [changelog.md](./changelog.md).

## Getting started

Use the following command to clone a copy of the repository:

```shell
git clone git@github.com:eliaspr/SkullKing.git
```

Next, run the following command to start a local instance of the SkullKing server:

```shell
gradlew bootRun
```

The server uses port `8080` by default. Access the application using multiple browsers and start your first game!

## Data storage

The server stores all game session related data in memory only. Currently, there is no option to persist that data.

## Card images

Due to copyright concerns, there is no longer a public CDN containing the card images. The repository contains replacement images which convey the meaning of the cards without using pictures of the actual card designs. However, there exists the `skullking.cards.url` application property which you can change to point to any location where you can then host your own card images.

> [!CAUTION]
> The current server implementation is anything but optimized for serving static content. This is another reason for hosting the card images on some other server.

## Build & Deployment

The project contains a `Dockerfile` for simple deployment using Docker. The docker image can be built by running the following commands:

```shell
gradlew bootJar
docker build -t skull-king .
```

For automated build and release, the project contains a `publish.ps1` script which detects version information, builds the docker image and pushes the image to the specified container registry. Run the script by executing the `publish.ps1` file (make sure to specify the correct docker registry):

```shell
powershell -ExecutionPolicy Unrestricted ./publish.ps1 -DockerRegistry example.docker.io
```

You can start an instance of the Docker image using the following command (make sure to use the correct image name when pulling from a repository):

```shell
docker run --rm --name SkullKing -p 8080:8080 skull-king:latest
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
      - '8080:8080'
```

Using either option, don't forget you can set the `skullking.cards.url` environment variable as described above.
