# Используем официальный образ Maven
FROM maven:3.8.6 AS builder

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем только POM файл сначала (для кэширования зависимостей)
COPY pom.xml .

# Копируем файлы с библиотеками
COPY lib ./lib

# Устанавливаем кастомные библиотеки
RUN mvn install:install-file -Dfile=lib/morphology/morph/1.5/morph-1.5.jar \
    -DgroupId=org.apache.lucene.morphology \
    -DartifactId=morph \
    -Dversion=1.5 \
    -Dpackaging=jar

RUN mvn install:install-file -Dfile=lib/analysis/morphology/1.5/morphology-1.5.jar \
    -DgroupId=org.apache.lucene.analysis \
    -DartifactId=morphology \
    -Dversion=1.5 \
    -Dpackaging=jar

RUN mvn install:install-file -Dfile=lib/morphology/dictionary-reader/1.5/dictionary-reader-1.5.jar \
    -DgroupId=org.apache.lucene.morphology \
    -DartifactId=dictionary-reader \
    -Dversion=1.5 \
    -Dpackaging=jar

RUN mvn install:install-file -Dfile=lib/morphology/english/1.5/english-1.5.jar \
    -DgroupId=org.apache.lucene.morphology \
    -DartifactId=english \
    -Dversion=1.5 \
    -Dpackaging=jar

RUN mvn install:install-file -Dfile=lib/morphology/russian/1.5/russian-1.5.jar \
    -DgroupId=org.apache.lucene.morphology \
    -DartifactId=russian \
    -Dversion=1.5 \
    -Dpackaging=jar

# Копируем исходный код
COPY src ./src

# Собираем проект
RUN mvn clean package

# Финальный образ
FROM eclipse-temurin:17
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
CMD ["java", "-jar", "app.jar"]

