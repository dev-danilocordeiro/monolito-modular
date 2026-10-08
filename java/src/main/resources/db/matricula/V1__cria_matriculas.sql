-- turma_id referencia outro módulo: só o id, sem foreign key entre schemas.
CREATE TABLE matricula.matriculas (
    id           UUID PRIMARY KEY,
    estudante_id UUID NOT NULL,
    turma_id     UUID NOT NULL,
    ano_letivo   INT  NOT NULL,
    CONSTRAINT matricula_unica_por_turma UNIQUE (estudante_id, turma_id)
);
