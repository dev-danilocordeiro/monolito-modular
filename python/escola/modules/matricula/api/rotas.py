from uuid import UUID

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from escola.modules.matricula.public import matricular
from escola.modules.turmas.public import TurmaSemVaga

router = APIRouter(prefix="/matriculas")


class NovaMatricula(BaseModel):
    estudante_id: UUID
    turma_id: UUID
    ano_letivo: int


@router.post("", status_code=201)
def criar(pedido: NovaMatricula) -> NovaMatricula:
    try:
        matricular(pedido.estudante_id, pedido.turma_id, pedido.ano_letivo)
    except TurmaSemVaga as erro:
        raise HTTPException(status_code=409, detail=str(erro)) from erro
    return pedido
