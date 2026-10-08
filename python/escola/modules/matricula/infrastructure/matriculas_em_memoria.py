from escola.modules.matricula.domain.matricula import Matricula


class MatriculasEmMemoria:
    def __init__(self) -> None:
        self.salvas: list[Matricula] = []

    def salvar(self, matricula: Matricula) -> None:
        self.salvas.append(matricula)
