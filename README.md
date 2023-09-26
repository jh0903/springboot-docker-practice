# Dockerizing a Spring Boot
## Docker

컨테이너 기반의 오픈소스 가상화 플랫폼으로, 일관성있고 동일한 개발환경을 제공해준다.

💡컨테이너: 라이브러리, 시스템 도구, 코드, 런타임 등 서버를 실행하는 데 필요한 모든 것이 포함되어 있음

![Untitled](https://velog.velcdn.com/images%2Fmarkany%2Fpost%2Ffcc402fe-8238-46c1-a93f-ebbd9f7e3e9b%2Fdifference.png)

dockerfile을 작성하여 도커 이미지를 생성할 수 있다.

```bash
FROM openjdk:11
ARG JAR_FILE=*.jar
COPY practice-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

`FROM` 베이스 이미지

`ARG` 이미지 생성시에만 사용하는 변수

`COPY` 호스트 컴퓨터에 있는 디렉토리나 파일을 Docker 이미지의 파일시스템으로 복사할 때 사용

`ENTRYPOINT` 컨테이너를 실행 할 때 실행할 명령어

## docker compose

여러 개의 컨테이너를 띄우는 도커 애플리케이션을 정의하고 실행하는 도구이다.

docker-compose.yml 파일에 컨테이너 실행에 필요한 옵션을 적고,

docker-compose up 명령어를 통해 컨테이너들을 실행시킬 수 있다.

`docker-compose up`: Docker Compose에 정의되어 있는 모든 서비스 컨테이너를 한 번에 생성하고 실행함

`docker-compose down`: Docker Compose에 정의되어 있는 모든 서비스 컨테이너를 한 번에 정지시키고 삭제함

`docker-compose stop`: 돌아가고 있는 특정 서비스 컨테이너를 정지시킴

❗`docker-compose up` 명령어는 캐싱된 이미지를 사용하는데,

소스 수정이 이루어졌을 때는 다시 이미지를 빌드해야 하므로 가급적 `docker-compose up --build`를  하는 게 좋다.



다음은 springboot application 컨테이너와 mysql 컨테이너를 한번에 띄워주는 docker-compose.yml 파일이다.

```bash
version: "3" # yaml 파일 포맷의 버전을 나타냄
services: # 생성될 컨테이너들을 묶어놓은 단위
  database:
    image: mysql
    container_name: test_db
    environment: # 컨테이너 내부에서 사용할 환경변수 지정
            - MYSQL_DATABASE=testdb
            - MYSQL_ROOT_HOST=%
            - MYSQL_ROOT_PASSWORD=1234
    command: ['--character-set-server=utf8mb4', '--collation-server=utf8mb4_unicode_ci']
    ports:
      - 3308:3306
  application:
    build: . # build 항목에 정의된 도커파일에서 이미지를 빌드해 서비스의 컨테이너를 생성하도록 설정
    restart: always
    ports:
      - 80:8080
    depends_on: # 이 항목에 명시된 컨테이너가 먼저 생성되고 실행됨
      - database
    container_name: app_test
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://database:3306/testdb?useSSL=false&serverTimezone=UTC&useLegacyDatetimeCode=false&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: 1234
```

- 도커의 Host 포트와 Container 포트
    
    <img src="https://postfiles.pstatic.net/20150221_131/alice_k106_14244891403328IKAO_PNG/%C4%B8%C3%B3.PNG?type=w2" width="450" height="300"/>
    
    docker run -p 80:5000은 호스트의 80포트를 컨테이너의 5000포트에 연결한다는 뜻이다.
    
    따라서 외부에서 80번 포트에 접근하면, 컨테이너의 5000번 포트에 접속된다.
    
- 데이터베이스 컨테이너와 웹 서버 컨테이너가 정해진 순서대로 실행됐더라도, 데이터베이스가 초기화 중이라면 웹 서버 컨테이너가 정상적으로 동작하지 않을 수도 있다. 따라서 restart: always로 설정한다.
- 기본적으로 Docker Compose는 하나의 디폴트 네트워크에 모든 컨테이너를 연결한다. 디폴트 네트워크 안에서 컨테이너간 통신은 서비스 이름이 호스트명으로 사용되므로, jdbc:mysql://database:3306으로 mysql 컨테이너에 접근할 수 있다.

---

### Dockerfile 최적화

도커 이미지는 레이어로 빌드된다. 레이어별로 캐시해서, 변경이 없는 레이어에 대해서는 pull/push하지 않는다.

→ 의존 패키지를 캐싱해두면, 다음 빌드 시 캐싱된 것을 사용하여 도커 빌드 타임을 줄일 수 있다. 따라서 의존 패키지만 다운받는 단계를 만든다.

최종 이미지의 크기를 줄이기 위해 multi-stage build를 사용한다.

💡multi-stage build: 컨테이너 이미지를 만들면서 빌드 등에는 필요하지만, 최종 컨테이너 이미지에는 필요 없는 환경을 제거할 수 있도록 단계를 나누어 기반 이미지를 만드는 방법

builder 이미지에서 만들어진 jar파일만 복사해와서 최종 이미지의 크기를 줄일 수 있다.

```bash
FROM gradle:7.4-jdk11-alpine as builder
WORKDIR /build

# gradle 파일이 변경되었을 때만 새롭게 의존패키지 다운로드 받게함
COPY build.gradle settings.gradle /build/
RUN gradle build -x test --parallel --continue > /dev/null 2>&1 || true

# 빌더 이미지에서 애플리케이션 빌드
COPY . /build
RUN gradle build -x test --parallel

# APP
FROM openjdk:11.0-slim
WORKDIR /app

# 빌더 이미지에서 jar 파일만 복사
COPY --from=builder /build/build/libs/practice-0.0.1-SNAPSHOT.jar .

EXPOSE 8080

# root 대신 nobody 권한으로 실행
USER nobody
ENTRYPOINT [                                                \
    "java",                                                 \
    "-jar",                                                 \
    "practice-0.0.1-SNAPSHOT.jar"              \
]
```

<details>
<summary>출처</summary>
<div markdown="1">

    https://findstar.pe.kr/2022/05/13/gradle-docker-cache/
  
    https://blog.naver.com/alice_k106/220278762795

</div>
</details>
