"""Fachada do módulo matricula: a única coisa que outros módulos importam."""

from escola.modules.matricula.application.matricular import Matricular
from escola.modules.matricula.domain.matricula import Matricula
from escola.modules.matricula.infrastructure.matriculas_em_memoria import MatriculasEmMemoria
from escola.modules.turmas.public import reservar_vaga

__all__ = ["Matricula", "matricular"]

matricular = Matricular(MatriculasEmMemoria(), reservar_vaga)
