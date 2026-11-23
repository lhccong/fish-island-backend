CREATE TABLE user_points (userId BIGINT PRIMARY KEY, usedPoints INT NOT NULL) ENGINE=InnoDB;
CREATE TABLE farm_user (userId BIGINT PRIMARY KEY, level INT NOT NULL) ENGINE=InnoDB;
CREATE TABLE fish_pet (petId BIGINT PRIMARY KEY, userId BIGINT NOT NULL, level INT NOT NULL, isDelete INT NOT NULL) ENGINE=InnoDB;
CREATE TABLE pet_equip_forge (
    petId BIGINT NOT NULL, equipSlot INT NOT NULL, equipLevel INT,
    PRIMARY KEY (petId, equipSlot)
) ENGINE=InnoDB;
CREATE TABLE achievement_verify_ledger (
    user_id BIGINT NOT NULL, source_id VARCHAR(64) NOT NULL, points INT NOT NULL,
    PRIMARY KEY (user_id, source_id)
) ENGINE=InnoDB;

INSERT INTO user_points VALUES (1, 0), (2, 0), (3, 0);
INSERT INTO farm_user VALUES (1, 21), (2, 9), (3, 10);
INSERT INTO pet_card_player (user_id, opened) VALUES (1, 7), (2, 2);
INSERT INTO fish_pet VALUES (10, 1, 20, 0), (11, 1, 60, 1), (20, 2, 9, 0), (30, 3, 10, 0);
INSERT INTO pet_equip_forge VALUES (10, 1, 3), (10, 2, 1), (10, 3, NULL), (10, 7, 2), (10, 8, 1), (11, 1, 20);
