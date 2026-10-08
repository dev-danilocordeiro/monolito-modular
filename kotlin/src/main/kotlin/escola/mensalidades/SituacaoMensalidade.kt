package escola.mensalidades

sealed interface SituacaoMensalidade {
    data object EmAberto : SituacaoMensalidade
    data object Paga : SituacaoMensalidade
    data object Atrasada : SituacaoMensalidade
    data object Estornada : SituacaoMensalidade
    data class Desconhecida(val statusOriginal: String) : SituacaoMensalidade
}
