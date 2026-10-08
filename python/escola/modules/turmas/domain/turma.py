from dataclasses import dataclass
from typing import Protocol
from uuid import UUID


class TurmaSemVaga(Exception):
    def __init__(self, turma_id: UUID) -> None:
        super().__init__(f"turma {turma_id} sem vaga")
        self.turma_id = turma_id


@dataclass
class Turma:
    id: UUID
    capacidade: int
    ocupadas: int = 0

    def reservar_vaga(self) -> None:
        if self.ocupadas >= self.capacidade:
            raise TurmaSemVaga(self.id)
        self.ocupadas += 1


class RepositorioDeTurmas(Protocol):
    def buscar(self, turma_id: UUID) -> Turma: ...
    def salvar(self, turma: Turma) -> None: ...
