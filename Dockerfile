FROM amazoncorretto:17-alpine3.17

WORKDIR /app

COPY build/libs/SkullKingServer.jar .

EXPOSE 80

CMD ["java", "-jar", "SkullKingServer.jar"]
