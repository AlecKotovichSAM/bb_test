-- Добавляем поле avatar для хранения base64 изображений
-- Используем CLOB для хранения больших base64 строк (data:image/jpeg;base64,...)
ALTER TABLE users ADD COLUMN avatar CLOB;
