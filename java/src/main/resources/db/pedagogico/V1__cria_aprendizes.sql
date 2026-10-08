-- Cópia local do que o pedagógico precisa, alimentada pelo evento MatriculaConfirmada.
-- O id é o mesmo estudanteId da matrícula.
CREATE TABLE pedagogico.aprendizes (
    id          UUID PRIMARY KEY,
    nome_social TEXT NOT NULL
);
