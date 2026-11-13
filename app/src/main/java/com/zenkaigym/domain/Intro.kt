package com.zenkaigym.domain

data class IntroPage(val kanji: String, val title: String, val body: String)

object IntroPages {
    val all: List<IntroPage> get() = listOf(
        IntroPage(
            "禅", tr("Bem-vindo ao Zenkaigym"),
            tr("Treino e dieta com a disciplina de um mestre e a energia de um shonen. Seus dados ficam salvos só neste aparelho, de forma local: sem login, sem internet e sem enviar nada para fora.")
        ),
        IntroPage(
            "己", tr("Conte quem você é"),
            tr("Altura, peso, idade e objetivo. Ao salvar, o app já pondera sozinho a sua dieta e o seu treino.")
        ),
        IntroPage(
            "体", tr("Treino sob medida"),
            tr("Marque os dias e horários em que pode treinar. Distribuímos treinos e descanso de forma equilibrada.")
        ),
        IntroPage(
            "食", tr("Dieta simples"),
            tr("Escolha quantas refeições faz por dia e os horários. Sugerimos ovos e pão de manhã, frango e arroz no almoço, e assim por diante. No Dojo, é só marcar o que já comeu.")
        ),
        IntroPage(
            "決", tr("A decisão é sua"),
            tr("Tudo que o app gera é só uma sugestão. Você decide: ajuste, aceite ou descarte. Suas informações ficam guardadas localmente neste celular.")
        )
    )
}

/** Posição na apresentação; imutável para facilitar o teste e o estado do Compose. */
data class IntroFlow(val pageCount: Int, val index: Int = 0) {
    init {
        require(pageCount >= 1) { "A apresentação precisa de ao menos uma página." }
        require(index in 0 until pageCount) { "Página $index fora do intervalo 0..${pageCount - 1}." }
    }

    val isFirst get() = index == 0
    val isLast get() = index == pageCount - 1

    fun next() = if (isLast) this else copy(index = index + 1)
    fun back() = if (isFirst) this else copy(index = index - 1)
}

interface IntroStore {
    fun isSeen(): Boolean
    fun setSeen()
}

/** A apresentação aparece só até o usuário concluí-la (ou pulá-la) uma vez. */
class IntroGate(private val store: IntroStore) {
    fun shouldShow() = !store.isSeen()
    fun markSeen() = store.setSeen()
}
