CREATE TABLE media_files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    filename VARCHAR(255),
    content_type VARCHAR(255) NOT NULL,
    file_id VARCHAR(255),
    source_url VARCHAR(2048),
    uploaded_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token_id VARCHAR(64) NOT NULL,
    family_id VARCHAR(64) NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6),
    replaced_by_token_id VARCHAR(64),
    user_agent VARCHAR(255),
    ip_address VARCHAR(45),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_token_id UNIQUE (token_id)
);

CREATE TABLE organizer_keys (
    id BIGINT NOT NULL AUTO_INCREMENT,
    key_value VARCHAR(32) NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_by BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_organizer_keys_key_value UNIQUE (key_value)
);

CREATE TABLE secrets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    secret_type VARCHAR(255) NOT NULL,
    vault_path VARCHAR(255) NOT NULL,
    vault_key VARCHAR(255),
    status VARCHAR(255),
    last_synced_at DATETIME(6),
    expires_at DATETIME(6),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_secrets_name UNIQUE (name)
);

CREATE TABLE places (
    id VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    street VARCHAR(255),
    city VARCHAR(255),
    zip VARCHAR(255),
    country VARCHAR(255),
    latitude DOUBLE,
    longitude DOUBLE,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_place_name_city_country UNIQUE (name, city, country)
);

CREATE INDEX idx_place_city ON places (city);
CREATE INDEX idx_place_country ON places (country);

CREATE TABLE pages (
    id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    picture_id BIGINT,
    token_refreshed_at DATETIME(6),
    token_stored_at DATETIME(6),
    token_expires_at DATETIME(6),
    token_expires_in_days INTEGER,
    token_status VARCHAR(255),
    last_refresh_success BIT,
    last_refresh_error VARCHAR(255),
    last_refresh_attempt DATETIME(6),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    connected_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_pages_picture
        FOREIGN KEY (picture_id) REFERENCES media_files (id)
);

CREATE INDEX idx_page_token_status ON pages (token_status);
CREATE INDEX idx_page_token_expires_at ON pages (token_expires_at);

CREATE TABLE events (
    id VARCHAR(255) NOT NULL,
    page_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description LONGTEXT,
    start_time DATETIME(6) NOT NULL,
    end_time DATETIME(6),
    place_id VARCHAR(255),
    cover_image_id BIGINT,
    event_url VARCHAR(255),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_events_page
        FOREIGN KEY (page_id) REFERENCES pages (id),
    CONSTRAINT fk_events_place
        FOREIGN KEY (place_id) REFERENCES places (id),
    CONSTRAINT fk_events_cover_image
        FOREIGN KEY (cover_image_id) REFERENCES media_files (id)
);

CREATE INDEX idx_event_start_time ON events (start_time);
