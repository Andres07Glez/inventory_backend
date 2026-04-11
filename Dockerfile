# Usamos una imagen con Java 17 para compilar el proyecto
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# 1. Copiamos TODOS los archivos del proyecto a la carpeta /app del contenedor
COPY . .

# 2. Le damos permisos de ejecución a gradlew (Vital porque vienes de Windows)
RUN chmod +x ./gradlew

# 3. Construimos el jar omitiendo las pruebas para que sea más rápido
RUN ./gradlew clean build -x test

# Creamos la imagen final mucho más ligera solo con el JRE
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiamos el jar generado en el paso anterior
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar

# Exponemos el puerto de Spring Boot
EXPOSE 8080

# Ejecutamos la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]