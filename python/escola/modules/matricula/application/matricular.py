from collections.abc import Callable
from uuid import UUID

from escola.modules.matricula.domain.matricula import Matricula, RepositorioDeMatriculas


class Matricular:
    def __init__(self, matriculas: RepositorioDeMatriculas, reservar_vaga: Callable[[UUID], None]) -> None:
        self._matriculas = matriculas
        self._reservar_vaga = reservar_vaga

    def __call__(self, estudante_id: UUID, turma_id: UUID, ano_letivo: int) -> Matricula:
        self._reservar_vaga(turma_id)
        matricula = Matricula(estudante_id, turma_id, ano_letivo)
        self._matriculas.salvar(matricula)
        return matricula
