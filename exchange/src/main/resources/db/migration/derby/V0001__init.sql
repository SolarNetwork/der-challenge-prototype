CREATE SEQUENCE HIBERNATE_SEQUENCE START WITH 1 INCREMENT BY 1;

CREATE TABLE PRICE_MAPS (
	ID CHAR(16) FOR BIT DATA NOT NULL,
	CREATED_AT TIMESTAMP NOT NULL, 
	MODIFIED_AT TIMESTAMP NOT NULL, 
	DUR BIGINT NOT NULL, 
	POWER_REACTIVE BIGINT, 
	POWER_REAL BIGINT, 
	PRICE_CURRENCY VARCHAR(3) NOT NULL, 
	PRICE_ENERGY_APPARENT DECIMAL(18,9), 
	PRICE_ENERGY_REAL DECIMAL(18,9), 
	RESP_TIME_MAX BIGINT NOT NULL, 
	RESP_TIME_MIN BIGINT NOT NULL, 
	CONSTRAINT PRICE_MAPS_PK PRIMARY KEY (ID)
);

CREATE TABLE FACILITIES (
	ID CHAR(16) FOR BIT DATA NOT NULL,
	CREATED_AT TIMESTAMP NOT NULL,
	MODIFIED_AT TIMESTAMP NOT NULL,
	CUST_ID VARCHAR(20) NOT NULL,
	UICI VARCHAR(20) NOT NULL,
	FAC_UID VARCHAR(255) NOT NULL,
	FAC_URI VARCHAR(255) NOT NULL,
	FAC_KEY VARCHAR(255) FOR BIT DATA NOT NULL,
	PRICE_MAP_ID CHAR(16) FOR BIT DATA,
	CONSTRAINT FACILITIES_PK PRIMARY KEY (ID),
	CONSTRAINT FACILITIES_PRICE_MAP_FK FOREIGN KEY (PRICE_MAP_ID) REFERENCES PRICE_MAPS
);

CREATE INDEX FAC_UID_IDX ON FACILITIES (FAC_UID);

CREATE TABLE FACILITY_REGS (
	ID BIGINT NOT NULL,
	CREATED_AT TIMESTAMP NOT NULL,
	MODIFIED_AT TIMESTAMP NOT NULL,
	CUST_ID VARCHAR(20) NOT NULL,
	UICI VARCHAR(20) NOT NULL,
	FAC_UID VARCHAR(255) NOT NULL,
	FAC_URI VARCHAR(255) NOT NULL,
	FAC_KEY VARCHAR(255) FOR BIT DATA NOT NULL,
	FAC_NONCE VARCHAR(24) FOR BIT DATA NOT NULL,
	EXCH_NONCE VARCHAR(24) FOR BIT DATA NOT NULL,
	CONSTRAINT FACILITY_REGS_PK PRIMARY KEY (ID)
);

CREATE TABLE FACILITY_RESOURCE_CHARS (
	FACILITY_ID CHAR(16) FOR BIT DATA NOT NULL,
	CREATED_AT TIMESTAMP NOT NULL,
	MODIFIED_AT TIMESTAMP NOT NULL,
	LOAD_POWER_FACTOR FLOAT NOT NULL,
	LOAD_POWER_MAX BIGINT NOT NULL,
	RESP_TIME_MAX BIGINT NOT NULL,
	RESP_TIME_MIN BIGINT NOT NULL,
	STORAGE_ENERGY_CAP BIGINT NOT NULL,
	SUPPLY_POWER_FACTOR FLOAT NOT NULL, 
	SUPPLY_POWER_MAX BIGINT NOT NULL,
	CONSTRAINT FACILITY_RESOURCE_CHARS_PK PRIMARY KEY (FACILITY_ID),
	CONSTRAINT FACILITY_RESOURCE_CHARS_FACILITY_FK FOREIGN KEY (FACILITY_ID) REFERENCES FACILITIES
);

CREATE TABLE FACILITY_PROGRAM_TYPES (
	FACILITY_ID CHAR(16) FOR BIT DATA NOT NULL,
	PROGRAM VARCHAR(64) NOT NULL,
	CONSTRAINT FACILITY_PROGRAM_TYPES_PK PRIMARY KEY (FACILITY_ID, PROGRAM),
	CONSTRAINT FACILITY_PROGRAM_TYPES_FACILITY_FK FOREIGN KEY (FACILITY_ID) REFERENCES FACILITIES
);

CREATE TABLE PRICE_MAP_OFFERINGS (
	ID CHAR(16) FOR BIT DATA NOT NULL,
	CREATED_AT TIMESTAMP NOT NULL,
	MODIFIED_AT TIMESTAMP NOT NULL,
	PRICE_MAP_ID CHAR(16) FOR BIT DATA NOT NULL,
	START_AT TIMESTAMP NOT NULL,
	CONSTRAINT PRICE_MAP_OFFERINGS_PK PRIMARY KEY (ID),
	CONSTRAINT PRICE_MAP_OFFERINGS_PRICE_MAP_FK FOREIGN KEY (PRICE_MAP_ID) REFERENCES PRICE_MAPS
);

CREATE TABLE FACILITY_PRICE_MAP_OFFERS (
	ID CHAR(16) FOR BIT DATA NOT NULL,
	CREATED_AT TIMESTAMP NOT NULL,
	MODIFIED_AT TIMESTAMP NOT NULL,
	FACILITY_ID CHAR(16) FOR BIT DATA NOT NULL,
	OFFERING_ID CHAR(16) FOR BIT DATA NOT NULL,
	PRICE_MAP_ID CHAR(16) FOR BIT DATA,
	CONSTRAINT FACILITY_PRICE_MAP_OFFERS_PK PRIMARY KEY (ID),
	CONSTRAINT FACILITY_PRICE_MAPS_FACILITY_FK FOREIGN KEY (FACILITY_ID) REFERENCES FACILITIES,
	CONSTRAINT FACILITY_PRICE_MAP_OFFERS_OFFERING_FK FOREIGN KEY (OFFERING_ID) REFERENCES PRICE_MAP_OFFERINGS,
	CONSTRAINT FACILITY_PRICE_MAP_OFFERS_PRICE_MAP_FK FOREIGN KEY (PRICE_MAP_ID) REFERENCES PRICE_MAPS
);