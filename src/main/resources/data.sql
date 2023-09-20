-- data.sql

-- Vložení hardcodované sezony Ostatní, pokud neexistuje
INSERT INTO season (id, from_date, name, to_date, editable)
SELECT -1, '1970-01-01', 'Ostatní', '1970-12-31', false
WHERE NOT EXISTS (SELECT 1 FROM season WHERE id = -1);

-- Vložení hardcodované pokuty Gól, pokud neexistuje
INSERT INTO fine (id, amount, name, editable, inactive)
SELECT -1, 10, 'Gol', false, false
WHERE NOT EXISTS (SELECT 1 FROM fine WHERE id = -1);

-- Vložení hardcodované pokuty Hattrick, pokud neexistuje
INSERT INTO fine (id, amount, name, editable, inactive)
SELECT -2, 50, 'Hattrick', false, false
WHERE NOT EXISTS (SELECT 1 FROM fine WHERE id = -2);