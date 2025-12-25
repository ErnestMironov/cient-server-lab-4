CREATE SCHEMA IF NOT EXISTS utmn;

CREATE TABLE IF NOT EXISTS utmn.student_message
(
    id          bigserial PRIMARY KEY,
    full_name   varchar(128)    NOT NULL,
    passport    varchar(32)     NOT NULL,
    created_at  timestamptz     NOT NULL
);

COMMENT ON TABLE utmn.student_message IS 'Сообщения о студентах из RabbitMQ';
COMMENT ON COLUMN utmn.student_message.full_name IS 'ФИО студента';
COMMENT ON COLUMN utmn.student_message.passport IS 'Паспорт студента';
COMMENT ON COLUMN utmn.student_message.created_at IS 'Время создания сообщения';


