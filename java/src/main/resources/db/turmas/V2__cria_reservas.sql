-- Cada reserva tem identidade. O id vem de quem chama (a tentativa de matrícula) e é a
-- chave de idempotência: repetir a mesma reserva não ocupa outra vaga.
CREATE TABLE turmas.reservas (
    id       UUID PRIMARY KEY,
    turma_id UUID NOT NULL REFERENCES turmas.turmas (id),
    situacao TEXT NOT NULL CHECK (situacao IN ('RESERVADA', 'LIBERADA'))
);
