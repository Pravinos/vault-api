-- V19__expand_categories.sql

-- Add additional expense categories
INSERT INTO categories (name, icon) VALUES
    ('Bills',        '💡'),
    ('Hobbies',      '🎨'),
    ('Work',         '🛠️'),
    ('Subscriptions','📺'),
    ('Education',    '📚');

-- Add additional income category
INSERT INTO income_categories (name, icon) VALUES
    ('Cashback',     '💳');
