from uuid import uuid4

from fastapi.testclient import TestClient

from escola.main import app
from escola.modules.turmas.public import abrir_turma

cliente = TestClient(app)


def test_matricula_reserva_vaga_pela_fachada_de_turmas() -> None:
    turma_id = uuid4()
    abrir_turma(turma_id, capacidade=1)

    pedido = {"estudante_id": str(uuid4()), "turma_id": str(turma_id), "ano_letivo": 2027}

    assert cliente.post("/matriculas", json=pedido).status_code == 201
    assert cliente.post("/matriculas", json={**pedido, "estudante_id": str(uuid4())}).status_code == 409
