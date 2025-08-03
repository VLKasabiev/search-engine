
@echo off
SETLOCAL

echo Установка библиотек Morphology...

REM Установка morph
call mvn install:install-file ^
    -Dfile=lib\morphology\morph\1.5\morph-1.5.jar ^
    -DgroupId=org.apache.lucene.morphology ^
    -DartifactId=morph ^
    -Dversion=1.5 ^
    -Dpackaging=jar

REM Установка morphology
call mvn install:install-file ^
    -Dfile=lib\analysis\morphology\1.5\morphology-1.5.jar ^
    -DgroupId=org.apache.lucene.analysis ^
    -DartifactId=morphology ^
    -Dversion=1.5 ^
    -Dpackaging=jar

REM Установка dictionary-reader
call mvn install:install-file ^
    -Dfile=lib\morphology\dictionary-reader\1.5\dictionary-reader-1.5.jar ^
    -DgroupId=org.apache.lucene.morphology ^
    -DartifactId=dictionary-reader ^
    -Dversion=1.5 ^
    -Dpackaging=jar

REM Установка english
call mvn install:install-file ^
    -Dfile=lib\morphology\english\1.5\english-1.5.jar ^
    -DgroupId=org.apache.lucene.morphology ^
    -DartifactId=english ^
    -Dversion=1.5 ^
    -Dpackaging=jar

REM Установка russian
call mvn install:install-file ^
    -Dfile=lib\morphology\russian\1.5\russian-1.5.jar ^
    -DgroupId=org.apache.lucene.morphology ^
    -DartifactId=russian ^
    -Dversion=1.5 ^
    -Dpackaging=jar

echo Все библиотеки успешно установлены!
ENDLOCAL