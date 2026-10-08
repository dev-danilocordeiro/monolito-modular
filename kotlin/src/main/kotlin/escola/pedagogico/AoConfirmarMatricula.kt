package escola.pedagogico

import escola.matricula.api.MatriculaConfirmada

class AoConfirmarMatricula(private val aprendizes: Aprendizes) {
    fun handle(evento: MatriculaConfirmada) {
        aprendizes.salvar(
            Aprendiz(
                id = AprendizId(evento.estudanteId),
                nomeSocial = evento.nomeSocial,
                avaliacoes = emptyList(),
                faltas = 0,
            )
        )
    }
}
