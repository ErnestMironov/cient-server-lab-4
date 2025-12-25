### Лабораторная работа №4

### Тема: Обмен сообщениями между микросервисами через RabbitMQ

Для работы рекомендуется использовать следующие инструменты: Intellij IDEA, DBeaver, Docker Desktop, Postman/Swagger UI.

Цель лабораторной:

В этой работе ты реализуешь две микросервисные Java-приложения, которые взаимодействуют через брокер сообщений RabbitMQ:

- PRODUCER-SERVICE - принимает HTTP-запрос от клиента и отправляет сообщение в очередь RabbitMQ.
- CONSUMER-SERVICE - читает сообщения из очереди и записывает их в PostgreSQL.

Связка выглядит так:

Клиент → PRODUCER (REST) → RabbitMQ (queue) → CONSUMER (listener) → PostgreSQL

По сути, это мини-пример event-driven архитектуры, где сервисы обмениваются событиями через брокер сообщений, а не напрямую по HTTP.

Теория (очень кратко)  
RabbitMQ - это брокер сообщений. Он:

- принимает сообщения от одних приложений (producers),
- кладёт их в очереди,
- и позволяет другим приложениям (consumers) забирать их из очередей.

Таким образом, сервисы становятся слабо связаны: отправитель не обязан знать, кто и когда обработает его сообщение.

Основные сущности:

- Exchange - «почтовое отделение», которое принимает сообщения и решает, в какие очереди их направить.
- Queue - очередь сообщений.
- Binding - правило маршрутизации между exchange и queue.

Ты создаешь 3 инфраструктурных компонента и 2 сервиса:

- Docker-контейнеры:

- PostgreSQL - для хранения данных.
- RabbitMQ - брокер сообщений.

- Микросервисы:

- producer-service - REST API + отправка сообщений в RabbitMQ.
- consumer-service - слушает очередь RabbitMQ и сохраняет данные в PostgreSQL.

Для конфигурации сервисов используем Java 17, Maven, Spring Boot 3.x, как в предыдущих лабораторных.

Допускается не создавать новые сервисы, а использовать из прошлой лабораторной, добавив зависимости и конфиги для RabbitMQ.

Этап 1. Docker-контейнеры PostgreSQL и RabbitMQ

- Создай docker-compose.yaml в корне проекта.
- Добавь в файл сервисы для Postgres и RabbitMQ:

version: "3.8"

services:

db:

image: postgres:16

container_name: postgres

restart: always

environment:

POSTGRES_DB: postgres

POSTGRES_USER: postgres

POSTGRES_PASSWORD: postgres

ports:

\- "5432:5432"

rabbitmq:

image: rabbitmq:3-management

container_name: rabbitmq

restart: always

ports:

\- "5672:5672" # порт для приложений (AMQP)

\- "15672:15672" # web-интерфейс управления

environment:

RABBITMQ_DEFAULT_USER: guest

RABBITMQ_DEFAULT_PASS: guest

- Запусти контейнеры:

docker-compose up -d

- Проверь:

- Postgres доступен на localhost:5432 (можно через DBeaver).
- Интерфейс RabbitMQ доступен в браузере по адресу

<http://localhost:15672> (логин/пароль: guest/guest).

Сделай коммит в GitHub.

Этап 2. PRODUCER-SERVICE - сервис-отправитель.

- Перейди на [start.spring.io](https://start.spring.io/) (по аналогии с ЛР1/2/3)
  - Настройки:

- Project: Maven
- Language: Java
- Java: 17
- Spring Boot: 3.5.x (актуальная)
- Group: ru.rksp
- Artifact/Name: твоя фамилия латиницей + -producer (например, ivanov-producer)
- Description: Лабораторная работа №4 Имя Фамилия
  - Зависимости:
- Spring Web
- Spring for RabbitMQ (Spring AMQP)
- Lombok
- Springdoc OpenAPI для Swagger:

&lt;dependency&gt;

&lt;groupId&gt;org.springdoc&lt;/groupId&gt;

&lt;artifactId&gt;springdoc-openapi-starter-webmvc-ui&lt;/artifactId&gt;

&lt;version&gt;2.0.3&lt;/version&gt;

&lt;/dependency&gt;

- Скачай проект, залей в отдельный репозиторий на github.
- Конфигурация RabbitMQ в application.properties
  - Добавь настройки для подключения к RabbitMQ:

server.port=8081

spring.application.name=producer-service

spring.rabbitmq.host=localhost

spring.rabbitmq.port=5672

spring.rabbitmq.username=guest

spring.rabbitmq.password=guest

app.rabbitmq.exchange=student.exchange

app.rabbitmq.routing-key=student.created

app.rabbitmq.queue=student.queue

springdoc.swagger-ui.path=/swagger-ui.html

Пояснение:

Мы заранее задаем имена exchange, routing key и queue, чтобы использовать их и в producer и в consumer.

- 1. Добавление конфигурационного класса для RabbitMQ. например RabbitConfig:

@Configuration

public class RabbitConfig {

@Value("\${app.rabbitmq.exchange}")

private String exchangeName;

@Value("\${app.rabbitmq.queue}")

private String queueName;

@Value("\${app.rabbitmq.routing-key}")

private String routingKey;

@Bean

public TopicExchange exchange() {

return new TopicExchange(exchangeName);

}

@Bean

public Queue queue() {

return new Queue(queueName, true);

}

@Bean

public Binding binding(Queue queue, TopicExchange exchange) {

return BindingBuilder

.bind(queue)

.to(exchange)

.with(routingKey);

}

@Bean

public Jackson2JsonMessageConverter messageConverter() {

return new Jackson2JsonMessageConverter();

}

@Bean

public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,

Jackson2JsonMessageConverter messageConverter) {

RabbitTemplate template = new RabbitTemplate(connectionFactory);

template.setMessageConverter(messageConverter);

return template;

}

}

Это конфиг Spring AMQP: создает очередь, exchange, binding и настраивает RabbitTemplate для отправки JSON-сообщений.

- DTO для сообщения. Создай класс StudentMessage:

@Data

public class StudentMessage {

private String fullName;

private String passport;

private Instant createdAt;

}

- REST-контроллер, отправляющий сообщение в очередь. Создай контроллер, например StudentProducerController:

@RestController

@RequestMapping("/api/producer/students")

@RequiredArgsConstructor

public class StudentProducerController {

private final RabbitTemplate rabbitTemplate;

@Value("\${app.rabbitmq.exchange}")

private String exchangeName;

@Value("\${app.rabbitmq.routing-key}")

private String routingKey;

@PostMapping

public ResponseEntity&lt;String&gt; sendStudent(@RequestBody StudentMessage request) {

// Добавим timestamp на стороне продьюсера

request.setCreatedAt(Instant.now());

rabbitTemplate.convertAndSend(exchangeName, routingKey, request);

return ResponseEntity.status(HttpStatus.ACCEPTED)

.body("Message sent to RabbitMQ");

}

}

- Запусти producer-service, открой Swagger (если подключил) на

[http://localhost:{порт сервиса}/swagger-ui/index.html](http://localhost:%7bпорт%20сервиса%7d/swagger-ui/index.html) и попробуй отправить POST запрос.

Также можешь смотреть очередь в UI RabbitMQ (Queues → student.queue).

- Сделай коммит.

Этап 3. CONSUMER-SERVICE - сервис-получатель и запись в PostgreSQL

- Аналогично создаёшь второй проект - consumer-service:

Artifact/Name: твоя фамилия латиницей + -consumer (например, ivanov-consumer)

- 1. Зависимости:
- Spring Web
- Spring Data JPA
- Spring for RabbitMQ
- PostgreSQL Driver
- Liquibase Migration
- Lombok
- (опционально) Springdoc OpenAPI - для демонстрации работы сервиса.

- Залей на github.
- Настройки БД (можно взять из предыдущих лабораторных) и RabbitMQ в application.properties

server.port=8082

spring.application.name=consumer-service

\# PostgreSQL

spring.datasource.url=jdbc:postgresql://localhost:5432/postgres

spring.datasource.driver-class-name=org.postgresql.Driver

spring.datasource.username=postgres

spring.datasource.password=postgres

spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

\# RabbitMQ

spring.rabbitmq.host=localhost

spring.rabbitmq.port=5672

spring.rabbitmq.username=guest

spring.rabbitmq.password=guest

app.rabbitmq.queue=student.queue

springdoc.swagger-ui.path=/swagger-ui.html

- Создай схему и таблицу для хранения информации, которую мы передаем в producer-service, примерный скрипт:

CREATE SCHEMA IF NOT EXISTS utmn;

CREATE TABLE IF NOT EXISTS utmn.student_message

(

id bigserial PRIMARY KEY,

full_name varchar(128) NOT NULL,

passport varchar(32) NOT NULL,

created_at timestamptz NOT NULL

);

COMMENT ON TABLE utmn.student_message IS 'Сообщения о студентах из RabbitMQ';

COMMENT ON COLUMN utmn.student_message.full_name IS 'ФИО студента';

COMMENT ON COLUMN utmn.student_message.passport IS 'Паспорт студента';

COMMENT ON COLUMN utmn.student_message.created_at IS 'Время создания сообщения';

- Создай Entity + Repository для сохранения сущностей в БД по аналогии с предыдущими лабораторными.
- DTO для десериализации сообщения. Сделай такой же StudentMessage (как в producer), либо в общем виде:

@Data

public class StudentMessage {

private String fullName;

private String passport;

private Instant createdAt;

}

- Конфигурация RabbitMQ и Listener.
  - Добавь конфиг (приём сообщений):

@Configuration

public class RabbitConfig {

@Bean

public Jackson2JsonMessageConverter messageConverter() {

return new Jackson2JsonMessageConverter();

}

@Bean

public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(

ConnectionFactory connectionFactory,

Jackson2JsonMessageConverter messageConverter

) {

SimpleRabbitListenerContainerFactory factory =

new SimpleRabbitListenerContainerFactory();

factory.setConnectionFactory(connectionFactory);

factory.setMessageConverter(messageConverter);

return factory;

}

}

- 1. И сам listener-класс:

@Service

@RequiredArgsConstructor

public class StudentMessageListener {

private final StudentMessageRepository repository;

@RabbitListener(queues = "\${app.rabbitmq.queue}")

public void handleStudentMessage(StudentMessage message) {

StudentMessageEntity entity = new StudentMessageEntity();

entity.setFullName(message.getFullName());

entity.setPassport(message.getPassport());

entity.setCreatedAt(message.getCreatedAt());

repository.save(entity);

}

}

Пояснение:

Аннотация @RabbitListener говорит Spring: «вызывать этот метод каждый раз, когда появляется сообщение в очереди student.queue».

- Запусти consumer-service. Убедись, что он не падает и подключается к RabbitMQ/DB.
- Сделай коммит.

Этап 4. Сквозная проверка

- Убедись, что запущены:

- Docker-контейнеры: Postgres, RabbitMQ.
- producer-service
- consumer-service

- Отправь POST запрос в producer-service, это может быть url <http://localhost:8081/api/producer/students>
- Проверь в UI RabbitMQ, что сообщение попало в очередь и обработалось (сообщений в очереди не должно быть, но счетчик delivered/in/out изменится).
- Сделай коммит.

Не все может заработать по инструкции, нужно самостоятельно привести систему в работоспособное состояние.

По итогам лабораторной твоя система должна:

- Принимать запрос в producer-service через REST (POST запрос в Swagger).
- Отправлять сообщение в RabbitMQ.
- consumer-service считывает сообщение из RabbitMQ.
- consumer-service записывает данные в PostgreSQL.
