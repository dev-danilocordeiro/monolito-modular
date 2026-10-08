from dataclasses import dataclass
from typing import Protocol
from uuid import UUID


@dataclass(frozen=True)
class Matricula:
    estudante_id: UUID
    turma_id: UUID
    ano_letivo: int


class RepositorioDeMatriculas(Protocol):
    def salvar(self, matricula: Matricula) -> None: ...
