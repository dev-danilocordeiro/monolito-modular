from uuid import UUID

from escola.modules.turmas.domain.turma import Turma


class TurmasEmMemoria:
    def __init__(self) -> None:
        self._turmas: dict[UUID, Turma] = {}

    def buscar(self, turma_id: UUID) -> Turma:
        return self._turmas[turma_id]

    def salvar(self, turma: Turma) -> None:
        self._turmas[turma.id] = turma
