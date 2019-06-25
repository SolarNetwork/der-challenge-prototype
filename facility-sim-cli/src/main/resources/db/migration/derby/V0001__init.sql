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
