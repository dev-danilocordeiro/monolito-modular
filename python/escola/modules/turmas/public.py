"""Fachada do módulo turmas: a única coisa que outros módulos importam."""

from uuid import UUID

from escola.modules.turmas.application.reservar_vaga import ReservarVaga
from escola.modules.turmas.domain.turma import Turma, TurmaSemVaga
from escola.modules.turmas.infrastructure.turmas_em_memoria import TurmasEmMemoria

__all__ = ["TurmaSemVaga", "abrir_turma", "reservar_vaga"]

_turmas = TurmasEmMemoria()
reservar_vaga = ReservarVaga(_turmas)


def abrir_turma(turma_id: UUID, capacidade: int) -> None:
    _turmas.salvar(Turma(turma_id, capacidade))
