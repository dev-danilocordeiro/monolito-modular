from uuid import UUID

from fastapi import APIRouter, HTTPException

from escola.modules.turmas.public import TurmaSemVaga, reservar_vaga

router = APIRouter(prefix="/turmas")


@router.post("/{turma_id}/reservas", status_code=204)
def reservar(turma_id: UUID) -> None:
    try:
        reservar_vaga(turma_id)
    except TurmaSemVaga as erro:
        raise HTTPException(status_code=409, detail=str(erro)) from erro
