from fastapi import FastAPI

from escola.modules.matricula.api import rotas as rotas_matricula
from escola.modules.turmas.api import rotas as rotas_turmas

app = FastAPI(title="escola")
app.include_router(rotas_matricula.router)
app.include_router(rotas_turmas.router)
