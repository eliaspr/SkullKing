FROM amazoncorretto:21-alpine3.19

WORKDIR /app

COPY build/libs/SkullKingServer.jar .

EXPOSE 8080

CMD ["java", "-jar", "SkullKingServer.jar"]
