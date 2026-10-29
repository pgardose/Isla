-- Isla database schema (MySQL 8 / MariaDB compatible).
-- Create the database first:  CREATE DATABASE isla CHARACTER SET utf8mb4;
-- The game also runs this script automatically at startup (all statements are idempotent).

CREATE TABLE IF NOT EXISTS players (
    player_id  INT          NOT NULL PRIMARY KEY,
    name       VARCHAR(60)  NOT NULL,
    money      INT          NOT NULL,
    stamina    INT          NOT NULL,
    pos_x      INT          NOT NULL,
    pos_y      INT          NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS items (
    item_id       INT          NOT NULL PRIMARY KEY,
    name          VARCHAR(80)  NOT NULL,
    type          VARCHAR(20)  NOT NULL,          -- CONSUMABLE | TOOL | SOUVENIR
    price         INT          NOT NULL,
    effect_value  INT          NOT NULL DEFAULT 0, -- stamina restored (consumables)
    unlocks_area  VARCHAR(80)  NULL                -- area unlocked (tools)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS inventory (
    inventory_id  INT  NOT NULL AUTO_INCREMENT PRIMARY KEY,
    player_id     INT  NOT NULL,
    item_id       INT  NOT NULL,
    quantity      INT  NOT NULL,
    UNIQUE KEY uq_inventory (player_id, item_id),
    CONSTRAINT fk_inventory_player FOREIGN KEY (player_id) REFERENCES players (player_id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_item   FOREIGN KEY (item_id)   REFERENCES items (item_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS unlocked_areas (
    player_id  INT          NOT NULL,
    area_name  VARCHAR(80)  NOT NULL,
    PRIMARY KEY (player_id, area_name),
    CONSTRAINT fk_areas_player FOREIGN KEY (player_id) REFERENCES players (player_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS vendors (
    vendor_id   INT           NOT NULL PRIMARY KEY,
    name        VARCHAR(60)   NOT NULL,
    location_x  INT           NOT NULL,
    location_y  INT           NOT NULL,
    sprite      VARCHAR(40)   NOT NULL,
    dialogue    VARCHAR(255)  NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS shop_stock (
    stock_id            INT  NOT NULL AUTO_INCREMENT PRIMARY KEY,
    vendor_id           INT  NOT NULL,
    item_id             INT  NOT NULL,
    quantity_available  INT  NOT NULL,
    UNIQUE KEY uq_stock (vendor_id, item_id),
    CONSTRAINT fk_stock_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (vendor_id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_item   FOREIGN KEY (item_id)   REFERENCES items (item_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quests (
    quest_id       INT           NOT NULL PRIMARY KEY,
    giver_npc_id   INT           NOT NULL,
    description    VARCHAR(255)  NOT NULL,
    reward_amount  INT           NOT NULL,
    is_completed   BOOLEAN       NOT NULL DEFAULT FALSE,
    required_item  VARCHAR(80)   NULL
) ENGINE=InnoDB;

-- Milestone 4: extra data for debug puzzles. Separate tables (not new columns on
-- quests) so databases created before this milestone pick them up automatically.
CREATE TABLE IF NOT EXISTS debug_quests (
    quest_id       INT           NOT NULL PRIMARY KEY,
    snippet        VARCHAR(500)  NOT NULL,
    correct_index  INT           NOT NULL,
    FOREIGN KEY (quest_id) REFERENCES quests (quest_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quest_options (
    quest_id       INT           NOT NULL,
    option_index   INT           NOT NULL,
    option_text    VARCHAR(255)  NOT NULL,
    PRIMARY KEY (quest_id, option_index),
    FOREIGN KEY (quest_id) REFERENCES debug_quests (quest_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Score: wrong answers and deaths for the current playthrough (the ending turns them into a rank).
-- A separate table, so databases created before the score existed gain it automatically.
CREATE TABLE IF NOT EXISTS player_stats (
    player_id      INT  NOT NULL PRIMARY KEY,
    wrong_answers  INT  NOT NULL DEFAULT 0,
    deaths         INT  NOT NULL DEFAULT 0,
    CONSTRAINT fk_stats_player FOREIGN KEY (player_id) REFERENCES players (player_id) ON DELETE CASCADE
) ENGINE=InnoDB;
