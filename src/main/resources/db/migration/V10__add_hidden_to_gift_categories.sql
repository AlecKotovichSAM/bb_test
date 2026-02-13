-- Добавляем поле hidden в таблицу gift_categories
ALTER TABLE gift_categories ADD COLUMN hidden BOOLEAN DEFAULT FALSE NOT NULL;

-- Устанавливаем hidden = true для всех существующих (предустановленных) категорий
UPDATE gift_categories SET hidden = TRUE;
