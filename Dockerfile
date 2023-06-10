FROM amazoncorretto:17-alpine3.17

WORKDIR /app

COPY build/libs/SkullKingServer.jar .

EXPOSE 8080

CMD ["java", "-jar", "SkullKingServer.jar"]