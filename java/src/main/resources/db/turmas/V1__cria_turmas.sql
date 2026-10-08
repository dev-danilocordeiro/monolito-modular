CREATE TABLE turmas.turmas (
    id         UUID PRIMARY KEY,
    ano_letivo INT  NOT NULL,
    capacidade INT  NOT NULL CHECK (capacidade > 0),
    ocupadas   INT  NOT NULL DEFAULT 0,
    CONSTRAINT ocupadas_dentro_da_capacidade CHECK (ocupadas BETWEEN 0 AND capacidade)
);
