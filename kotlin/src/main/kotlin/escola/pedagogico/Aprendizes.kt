package escola.pedagogico

// Porta de persistência do contexto pedagógico. O post só usa o salvar.
interface Aprendizes {
    fun salvar(aprendiz: Aprendiz)
}
