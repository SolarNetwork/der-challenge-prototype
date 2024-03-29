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

CREATE TABLE PRICE_MAP_OFFER_EVENTS (
	ID CHAR(16) FOR BIT DATA NOT NULL,
	CREATED_AT TIMESTAMP NOT NULL,
	MODIFIED_AT TIMESTAMP NOT NULL,
	PRICE_MAP_ID BIGINT NOT NULL,
	COUNTER_OFFER_ID BIGINT,
	START_AT TIMESTAMP NOT NULL,
	IS_ACCEPTED BOOLEAN NOT NULL,
	IS_SUCCESS BOOLEAN NOT NULL,
	EXEC_STATE VARCHAR(12) NOT NULL,
	MSG VARCHAR(255),
	FAC_PRICE_MAP_ID VARCHAR(64),
	CONSTRAINT PRICE_MAP_OFFER_EVENTS_PK PRIMARY KEY (ID),
	CONSTRAINT PRICE_MAP_OFFER_EVENTS_PRICE_MAP_FK FOREIGN KEY (PRICE_MAP_ID) REFERENCES PRICE_MAPS,
	CONSTRAINT PRICE_MAP_OFFER_EVENTS_COUNTER_OFFER_FK FOREIGN KEY (COUNTER_OFFER_ID) REFERENCES PRICE_MAPS
);
