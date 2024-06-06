FROM amazoncorretto:21-alpine3.19

WORKDIR /app

COPY build/libs/SkullKingServer.jar .

EXPOSE 80

CMD ["java", "-jar", "SkullKingServer.jar"]
