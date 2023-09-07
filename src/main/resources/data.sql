-- data.sql

-- Vložení hardcodované sezony Ostatní, pokud neexistuje
INSERT INTO season (id, from_date, name, to_date, editable)
SELECT -2, '1970-01-01', 'Ostatní', '1970-12-31', false
WHERE NOT EXISTS (SELECT 1 FROM season WHERE id = -2);

-- Vložení hardcodované pokuty Gól, pokud neexistuje
INSERT INTO fine (id, amount, name, editable)
SELECT -1, 10, 'Gol', false
WHERE NOT EXISTS (SELECT 1 FROM fine WHERE id = -1);

-- Vložení hardcodované pokuty Hattrick, pokud neexistuje
INSERT INTO fine (id, amount, name, editable)
SELECT -2, 50, 'Hattrick', false
WHERE NOT EXISTS (SELECT 1 FROM fine WHERE id = -2);