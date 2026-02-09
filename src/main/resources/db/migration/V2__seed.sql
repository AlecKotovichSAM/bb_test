-- Seed users (hosts & registered users)
INSERT INTO users (first_name, last_name, email, address) VALUES
 ('Anna','Muster','anna@example.com','Berlin, Prenzlauer Allee 1'),
 ('Ben','Schmidt','ben@example.com','Hamburg, Reeperbahn 7'),
 ('Clara','Fischer','clara@example.com','München, Leopoldstr. 12');

-- Seed children (belong to users)
INSERT INTO children (user_id, first_name, birthday, gender, avatar) VALUES
 (1,'Levi','2018-06-10','male',NULL),
 (1,'Matilda','2020-03-22','female',NULL),
 (2,'Tom','2017-12-01','male',NULL);

-- Providers (locations)
INSERT INTO providers (company_name, website, email, phone, address, description) VALUES
 ('Klettermax Indoor','https://klettermax.de','info@klettermax.de','030-123456','Berlin, Ring 5','Indoor-Kletterpark für Kinder'),
 ('Bälleparadies','https://baelleparadies.de','hello@baelleparadies.de','040-998877','Hamburg, Hafenstr. 3','Indoor-Spielplatz mit Bällebad'),
 ('FunPark München','https://funpark-m.de','hi@funpark-m.de','089-556677','München, Parkweg 9','Großer Indoorspielplatz mit Rutschen');

-- Events (hosted by user 1 for child 1 & 2)
INSERT INTO events (host_id, child_id, datetime, location_type, location, status, comment) VALUES
 (1,1, TIMESTAMP '2026-03-15 15:00:00','provider','1','Planned','Levi wird 8!'),
 (1,2, TIMESTAMP '2026-04-20 14:30:00','manual','Berlin, Familiencafé Sonnenschein','Draft','Matilda mag Einhörner');

-- Guests: unregistered and registered (Ben is registered user id=2)
INSERT INTO event_guests (event_id, guest_name, user_id, rsvp_status) VALUES
 (1,'Sophie', NULL,'open'),
 (1,'Ben', 2,'accepted'),
 (1,'Clara', 3,'open'),
 (2,'Laura', NULL,'open');

-- Guest tokens (for unregistered guests in event 1 and 2)
-- For demo the tokens are static; rotate in prod
INSERT INTO guest_tokens (guest_id, token, valid_until) VALUES
 (1,'11111111-1111-1111-1111-111111111111', TIMESTAMP '2026-12-31 23:59:59'),
 (4,'22222222-2222-2222-2222-222222222222', TIMESTAMP '2026-12-31 23:59:59');

-- Gifts for event 1
INSERT INTO gifts (event_id, title, description, url, image, price, status, reserved_by_guest) VALUES
 (1,'LEGO Technic Set','Auto-Modell','https://example.com/lego', NULL, 59.99,'open', NULL),
 (1,'Fußball','Größe 4','https://example.com/ball', NULL, 19.95,'reserved', 2),
 (1,'Bücherpaket','3x Abenteuer','https://example.com/books', NULL, 24.50,'open', NULL);

-- Chat messages (registered users only)
INSERT INTO chat_messages (event_id, user_id, message, created_at) VALUES
 (1,2,'Wir kommen gern! Brauchen wir etwas mitzubringen?', CURRENT_TIMESTAMP()),
 (1,1,'Danke Ben, evtl. Obstteller wäre super.', CURRENT_TIMESTAMP());
