CREATE TABLE role(
    id SERIAL PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE STATUS (
     id SERIAL PRIMARY KEY,
     name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE QUALIFICATION (
      id SERIAL PRIMARY KEY,
      name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE LOCATION (
        id SERIAL PRIMARY KEY,
        name VARCHAR(100) NOT NULL,
        city VARCHAR(50) NOT NULL,
        address VARCHAR(200) NOT NULL,
        is_available BOOLEAN DEFAULT TRUE NOT NULL
);

CREATE TABLE COURT (
       id SERIAL PRIMARY KEY,
       name VARCHAR(20) NOT NULL,
       is_available BOOLEAN DEFAULT TRUE NOT NULL,
       id_location INT NOT NULL, FOREIGN KEY (id_location) REFERENCES location(id) ON DELETE CASCADE
);

CREATE TABLE USERS (
        id SERIAL PRIMARY KEY,
        full_name VARCHAR(100) NOT NULL,
        login VARCHAR(50) NOT NULL UNIQUE,
        password VARCHAR(255) NOT NULL,
        id_role INT NOT NULL, FOREIGN KEY (id_role) REFERENCES role(id)
);

CREATE TABLE PLAYER (
        id SERIAL PRIMARY KEY,
        full_name VARCHAR(100) NOT NULL,
        datebirth DATE NOT NULL,
        phone_number VARCHAR(20) NOT NULL UNIQUE,
        rating INT NOT NULL CHECK(rating>=0),
        id_user INT NOT NULL, FOREIGN KEY (id_user) REFERENCES USERS(id)
);

CREATE TABLE REFEREE (
        id SERIAL PRIMARY KEY,
        full_name VARCHAR(100) NOT NULL,
        datebirth DATE NOT NULL,
        phone_number VARCHAR(20) NOT NULL UNIQUE,
        id_qualification INT NOT NULL,
        id_user INT NOT NULL,
        FOREIGN KEY (id_qualification) REFERENCES qualification(id),
        FOREIGN KEY (id_user) REFERENCES users(id)
);

CREATE TABLE TOURNAMENT(
        id SERIAL PRIMARY KEY,
        name VARCHAR(100) NOT NULL UNIQUE,
        date_start DATE NOT NULL,
        date_finish DATE NOT NULL,
        city VARCHAR(50) NOT NULL,
        min_rating INT NOT NULL CHECK(min_rating>=0),
        max_quantity_participant INT NOT NULL CHECK (max_quantity_participant>0),
        prize_fund DECIMAL(10,2) NOT NULL CHECK(prize_fund>=0),
        description VARCHAR(500),
        id_status INT NOT NULL,
        id_location INT NOT NULL,
        id_user INT NOT NULL,
        FOREIGN KEY (id_status) REFERENCES status(id),
        FOREIGN KEY (id_location) REFERENCES location(id),
        FOREIGN KEY (id_user) REFERENCES users(id)
);

CREATE TABLE MATCH (
        id SERIAL PRIMARY KEY,
        date DATE NOT NULL,
        time TIME NOT NULL,
        id_court INT NOT NULL,
        id_tournament INT NOT NULL,
        id_user INT NOT NULL,
        FOREIGN KEY (id_court) REFERENCES court(id),
        FOREIGN KEY (id_tournament) REFERENCES tournament(id) ON DELETE CASCADE,
        FOREIGN KEY (id_user) REFERENCES users(id)
);

CREATE TABLE TOURNAMENT_PLAYER (
        id SERIAL PRIMARY KEY,
        id_tournament INT NOT NULL,
        id_player INT NOT NULL,
        id_match INT,
        FOREIGN KEY (id_tournament) REFERENCES tournament(id) ON DELETE CASCADE,
        FOREIGN KEY (id_player) REFERENCES player(id) ON DELETE CASCADE,
        FOREIGN KEY (id_match) REFERENCES match(id)
);

CREATE TABLE TOURNAMENT_REFEREE (
       id SERIAL PRIMARY KEY,
       id_tournament INT NOT NULL,
       id_referee INT NOT NULL,
       id_match INT,
       FOREIGN KEY (id_tournament) REFERENCES tournament(id) ON DELETE CASCADE,
       FOREIGN KEY (id_referee) REFERENCES referee(id) ON DELETE CASCADE,
       FOREIGN KEY (id_match) REFERENCES match(id)
);











