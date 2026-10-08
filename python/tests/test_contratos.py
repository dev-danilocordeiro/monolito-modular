"""Prova que os contratos do .importlinter rejeitam imports ruins.

Cada teste copia o pacote para um diretório temporário, injeta uma violação e
roda o lint-imports na cópia. Passar no código de hoje não basta: se alguém
afrouxar um contrato, o teste correspondente começa a falhar.
"""

import os
import shutil
import subprocess
import sys
from pathlib import Path

import pytest

RAIZ = Path(__file__).resolve().parent.parent
# O executável instalado pelo Poetry no mesmo ambiente do pytest
LINT_IMPORTS = str(Path(sys.executable).parent / "lint-imports")


def lint_imports(diretorio: Path) -> subprocess.CompletedProcess[str]:
    env = {**os.environ, "PYTHONPATH": str(diretorio)}
    return subprocess.run(
        [LINT_IMPORTS, "--no-cache"],
        cwd=diretorio,
        env=env,
        capture_output=True,
        text=True,
    )


@pytest.fixture
def copia(tmp_path: Path) -> Path:
    shutil.copytree(RAIZ / "escola", tmp_path / "escola", ignore=shutil.ignore_patterns("__pycache__"))
    shutil.copy(RAIZ / ".importlinter", tmp_path / ".importlinter")
    return tmp_path


def injeta(copia: Path, arquivo: str, linha: str) -> None:
    alvo = copia / arquivo
    alvo.write_text(alvo.read_text() + f"\n{linha}  # violação injetada pelo teste\n")


def contratos_quebrados(saida: str) -> set[str]:
    return {linha.removesuffix(" BROKEN") for linha in saida.splitlines() if linha.endswith(" BROKEN")}


def test_o_codigo_atual_respeita_todos_os_contratos(copia: Path) -> None:
    resultado = lint_imports(copia)

    assert resultado.returncode == 0, resultado.stdout + resultado.stderr
    assert "4 kept, 0 broken" in resultado.stdout


@pytest.mark.parametrize(
    ("arquivo", "linha", "contrato"),
    [
        pytest.param(
            "escola/modules/matricula/application/matricular.py",
            "from escola.modules.turmas.domain import turma",
            "matricula só alcança turmas pelo public.py",
            id="matricula-importa-dominio-de-turmas",
        ),
        pytest.param(
            "escola/modules/turmas/domain/turma.py",
            "import pydantic",
            "Domínio não importa framework",
            id="dominio-importa-framework",
        ),
        pytest.param(
            "escola/modules/matricula/domain/matricula.py",
            "import fastapi",
            "Domínio não importa framework",
            id="dominio-de-outro-modulo-importa-framework",
        ),
        pytest.param(
            "escola/modules/turmas/application/reservar_vaga.py",
            "from escola.modules.matricula import public",
            "turmas não depende de matricula",
            id="turmas-importa-matricula",
        ),
        pytest.param(
            "escola/modules/turmas/domain/turma.py",
            "from escola.modules.turmas.application import reservar_vaga",
            "Cada módulo é api -> infrastructure -> application -> domain",
            id="dominio-importa-camada-de-cima",
        ),
    ],
)
def test_cada_violacao_quebra_o_contrato_correspondente(copia: Path, arquivo: str, linha: str, contrato: str) -> None:
    injeta(copia, arquivo, linha)

    resultado = lint_imports(copia)

    assert resultado.returncode != 0, "o lint deveria ter reprovado:\n" + resultado.stdout
    assert contrato in contratos_quebrados(resultado.stdout), resultado.stdout
