CREATE SEQUENCE HIBERNATE_SEQUENCE START WITH 1 INCREMENT BY 1;

CREATE TABLE EXCHANGES (
	IDENT VARCHAR(255) NOT NULL,
	CREATED_AT TIMESTAMP NOT NULL,
	MODIFIED_AT TIMESTAMP NOT NULL,
	EXCH_URI VARCHAR(255) NOT NULL,
	EXCH_KEY VARCHAR(255) FOR BIT DATA NOT NULL,
	CONSTRAINT EXCHANGES_PK PRIMARY KEY (IDENT)
);

CREATE TABLE EXCHANGE_REGS (
	IDENT VARCHAR(255) NOT NULL,
	CREATED_AT TIMESTAMP NOT NULL,
	MODIFIED_AT TIMESTAMP NOT NULL,
	EXCH_URI VARCHAR(255) NOT NULL,
	EXCH_KEY VARCHAR(255) FOR BIT DATA NOT NULL,
	EXCH_NONCE VARCHAR(24) FOR BIT DATA NOT NULL,
	FAC_NONCE VARCHAR(24) FOR BIT DATA NOT NULL,
	CONSTRAINT EXCHANGE_REGS_PK PRIMARY KEY (IDENT)
);

CREATE TABLE PRICE_MAPS (
	ID BIGINT NOT NULL, 
	CREATED_AT TIMESTAMP NOT NULL, 
	MODIFIED_AT TIMESTAMP NOT NULL, 
	DUR BIGINT NOT NULL, 
	POWER_REACTIVE BIGINT, 
	POWER_REAL BIGINT, 
	PRICE_CURRENCY VARCHAR(3) NOT NULL, 
	PRICE_ENERGY_APPARENT DECIMAL(18,9), 
	RESP_TIME_MAX BIGINT NOT NULL, 
	RESP_TIME_MIN BIGINT NOT NULL, 
	CONSTRAINT PRICE_MAPS_PK PRIMARY KEY (ID)
);

CREATE TABLE RESOURCE_CHARS (
	ID BIGINT NOT NULL,
	CREATED_AT TIMESTAMP NOT NULL,
	MODIFIED_AT TIMESTAMP NOT NULL,
	LOAD_POWER_FACTOR FLOAT NOT NULL,
	LOAD_POWER_MAX BIGINT NOT NULL,
	RESP_TIME_MAX BIGINT NOT NULL,
	RESP_TIME_MIN BIGINT NOT NULL,
	STORAGE_ENERGY_CAP BIGINT NOT NULL,
	SUPPLY_POWER_FACTOR FLOAT NOT NULL,
	SUPPLY_POWER_MAX BIGINT NOT NULL,
	CONSTRAINT RESOURCE_CHARS_PK PRIMARY KEY (ID)
);

CREATE TABLE FAC_SETTINGS (
	ID BIGINT NOT NULL,
	CREATED_AT TIMESTAMP NOT NULL,
	MODIFIED_AT TIMESTAMP NOT NULL,
	PRICE_MAP_ID BIGINT,
	CONSTRAINT FAC_SETTINGS_PK PRIMARY KEY (ID),
	CONSTRAINT FAC_SETTINGS_PRICE_MAP_FK FOREIGN KEY (PRICE_MAP_ID) REFERENCES PRICE_MAPS
);

CREATE TABLE PROGRAM_TYPES (
	FAC_SETTING_ID BIGINT NOT NULL,
	PROGRAM VARCHAR(64) NOT NULL,
	CONSTRAINT PROGRAM_TYPES_PK PRIMARY KEY (FAC_SETTING_ID, PROGRAM),
	CONSTRAINT PROGRAM_TYPES_FAC_SETTING_FK FOREIGN KEY (FAC_SETTING_ID) REFERENCES FAC_SETTINGS
);

CREATE TABLE FACILITY_PRICE_MAPS (
	FAC_SETTING_ID BIGINT NOT NULL,
	PRICE_MAP_ID BIGINT,
	CONSTRAINT FACILITY_PRICE_MAPS_PK PRIMARY KEY (FAC_SETTING_ID, PRICE_MAP_ID),
	CONSTRAINT FACILITY_PRICE_MAPS_FAC_SETTING_FK FOREIGN KEY (FAC_SETTING_ID) REFERENCES FAC_SETTINGS,
	CONSTRAINT FACILITY_PRICE_MAPS_PRICE_MAP_FK FOREIGN KEY (PRICE_MAP_ID) REFERENCES PRICE_MAPS
);

CREATE UNIQUE INDEX FACILITY_PRICE_MAPS_PRICE_MAP_UNQ ON FACILITY_PRICE_MAPS (PRICE_MAP_ID);

CREATE TABLE PRICE_MAP_OFFER_EVENTS (
	ID CHAR(16) FOR BIT DATA NOT NULL,
	CREATED_AT TIMESTAMP NOT NULL,
	MODIFIED_AT TIMESTAMP NOT NULL,
	PRICE_MAP_ID BIGINT NOT NULL,
	START_AT TIMESTAMP NOT NULL,
	IS_ACCEPTED BOOLEAN NOT NULL,
	IS_CONFIRMED BOOLEAN NOT NULL,
	IS_SUCCESS BOOLEAN NOT NULL,
	EXEC_STATE VARCHAR(12) NOT NULL,
	MSG VARCHAR(255),
	CONSTRAINT PRICE_MAP_OFFER_EVENTS_PK PRIMARY KEY (ID),
	CONSTRAINT PRICE_MAP_OFFER_EVENTS_PRICE_MAP_FK FOREIGN KEY (PRICE_MAP_ID) REFERENCES PRICE_MAPS
);
