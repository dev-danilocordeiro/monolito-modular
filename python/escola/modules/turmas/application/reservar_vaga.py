from uuid import UUID

from escola.modules.turmas.domain.turma import RepositorioDeTurmas


class ReservarVaga:
    def __init__(self, turmas: RepositorioDeTurmas) -> None:
        self._turmas = turmas

    def __call__(self, turma_id: UUID) -> None:
        turma = self._turmas.buscar(turma_id)
        turma.reservar_vaga()
        self._turmas.salvar(turma)
