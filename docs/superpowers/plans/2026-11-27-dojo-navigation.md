# Navegação "Só o Dojo" (proposta B): plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Objetivo:** tirar a barra de abas, deixar o Dojo como tela fixa, levar o resto para um menu e organizar as telas (Treino, Dieta, Evolução, Perfil, Ajustes e modo treino) com um botão principal por cartão e as ações secundárias atrás de um ⋯, sem perder nenhuma função.

**Arquitetura:** o `NavHost` continua com `HOME` como início, e toda navegação a partir do menu volta a pilha até `HOME`. Regras que dá para testar fora da tela (kanji dos músculos, faixa da semana, contagem de séries, estados do cartão, conclusão automática, resumo do treino) viram funções puras em `domain/` com testes na JVM. As telas só montam peças existentes (`ZenCard`, `ZenButton`, `ZenChip`, `KanjiBadge`, `SectionTitle`, `ZenFixedHeader`, `ZenPage`) e algumas peças novas compartilhadas (`HeaderBar`, `ZenSheet`, `ZenTabs`, `AccordionCard`). Cada fase deixa o app funcionando e pode sair como versão.

**Stack:** Kotlin 1.9.25, Jetpack Compose (BOM 2024.09.03, Material 3 1.3), Navigation Compose 2.8.4, Room 2.6.1, JUnit 4, Compose UI Test.

**Desenho:** [docs/superpowers/specs/2026-11-27-dojo-navigation-design.md](../specs/2026-11-27-dojo-navigation-design.md). Leia junto: as seções citadas em cada tarefa têm o texto exato das telas.

## Restrições globais

- Nenhuma dependência nova; `minSdk 24`; sem migração do Room nem mudança de esquema.
- Não mudam: widget, notificações (`RestTimer`, `RestService`, lembretes), backup, planejadores, nutrição, progressão e valores de XP.
- Todo texto novo passa por `tr("…")` e entra em `domain/Translations.kt` com inglês e japonês, preservando `{0}`, `{1}`. O `TranslationsCoverageTest` (Tarefa 1) falha se faltar alguma.
- Animações novas respeitam `LocalMotionEnabled` (sem animação quando desligado).
- O estilo muda só a "roupa": usar `Zen.look` (`shape`, `smallShape`, `border`, `thinBorder`, `shadow`), `Zen.p` e os acentos. Nenhuma tela ganha layout próprio por estilo.
- **Nunca chamar `ZenkaigymTheme` dentro de outra tela:** ele grava o global `accentSet` em `ui/theme/Color.kt` e troca os acentos do app inteiro.
- Ações diárias continuam a um toque no Dojo: "Feito", "Treinar agora", "+250 ml".
- Cor e kanji por destino: Dojo `Aka` 道 · Menu 門 · Treino `Ki` 体 · Dieta `Midori` 食 · Evolução `Daidai` 進 · Perfil `Sakura` 己 · Ajustes `Ao` 設.
- Comentários em português e nomes de teste em português (`camelCase`), como no resto do projeto.
- Trabalhar numa branch (`feat/dojo-navigation`), nunca direto na `main`. Um commit por tarefa.
- Gradle no shell do Claude (Git Bash), a partir da raiz do repositório: `export JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:/Users/gabriel.ramos/.gradle/uds'` antes de `./gradlew`.
- **Testes instrumentados:** nunca no AVD `Medium_Phone_API_36`. Ele tem um build de release com os dados de teste do usuário, e o debug não instala por cima (desinstalar apaga os dados). Crie um segundo AVD com a mesma imagem de sistema (por exemplo `ZenTest_API_36`), deixe só ele ligado ou use `ANDROID_SERIAL` (o `connectedDebugAndroidTest` roda em todos os aparelhos conectados). `adb` e `emulator` ficam em `$LOCALAPPDATA/Android/Sdk/`; no Git Bash use `export MSYS_NO_PATHCONV=1`.

Comandos usados nas tarefas:
- Unitários: `./gradlew :app:testDebugUnitTest --tests "com.zenkaigym.domain.<Classe>"`
- Compilar: `./gradlew :app:compileDebugKotlin`
- Instrumentados: `ANDROID_SERIAL=<serial do ZenTest> ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.zenkaigym.ui.<Classe>`

## Foco da revisão

Situações que o desenho implica e que mais podem morder quem usa o app. Cada uma tem um teste na tarefa dona do código:

1. **A conclusão automática dispara de novo** depois de "Marcar como feito" ou "Concluir assim", ao abrir um treino já concluído ou ao refazer uma série com o treino concluído. Esperado: só na transição "não concluído → última série pendente registrada". Teste `conclusaoAutomaticaSoNaTransicao` (Tarefa 16).
2. **O voltar do sistema pula um nível**: sai de Ajustes com uma subpágina aberta, sai da Evolução com o detalhe do histórico aberto, ou volta para o menu em vez do Dojo. Esperado: fecha primeiro o que está por cima; de qualquer destino do menu, volta ao Dojo. Testes `voltarDeQualquerTelaLevaAoDojo` (Tarefa 2), `voltarFechaDetalheDoHistorico` (Tarefa 9) e `voltarFechaASubpagina` (Tarefa 15).
3. **Tela larga ou celular deitado.** Esperado: coluna à esquerda com 6 entradas (inclui Dojo), selo 道 escondido e voltar levando ao Dojo. Teste `telaLargaMostraColunaSemSelo` (Tarefa 2).
4. **App novo ou depois de apagar os dados** (sem perfil, ficha, cardápio ou estatísticas). Esperado: Dojo, quadros do menu e faixa da semana mostram marcadores, sem quebrar. Testes `menuSemDadosMostraMarcadores` (Tarefa 3) e `semanaSemDadosNaoMarcaNada` (Tarefa 5).
5. **"Ver no cardápio" cai na aba errada** porque a Dieta lembrou a última aba aberta. Esperado: a Dieta sempre abre em Cardápio, no dia de hoje. Teste `dietaReabreNoCardapio` (Tarefa 13).

## Mapa de arquivos

| Arquivo | Responsabilidade | Tarefa |
|---|---|---|
| `test/.../domain/TranslationsCoverageTest.kt` (novo) | Todo `tr("…")` literal tem tradução | 1 |
| `ui/components/HeaderBar.kt` (novo) | Botão "← Dojo" (ou outro rótulo) + ações à direita; `MoreButton` (⋯) | 2 |
| `ui/components/ZenComponents.kt` | `ZenHeader`/`ZenFixedHeader` ganham `trailing`; `StyleBackdrop` sai de `ZenPage`; `ZenChip` ganha `border` | 2, 3, 7 |
| `ui/components/ZenIcons.kt` | Ícones `More` e `Cart` | 2, 13 |
| `ui/nav/AppNav.kt` | Sem barra; rota `MENU`; `go()`; coluna larga com 6 entradas | 2, 9, 13 |
| `ui/screens/MenuScreen.kt` (novo) | Menu 門 com quadros e dados ao vivo | 2, 3 |
| `domain/MuscleKanji.kt` (novo) | `MuscleGroup.kanji()`, `distinctByKanji()` | 4 |
| `domain/TrainingFlow.kt` (novo) | `SetCount`, `setCount()`, estados do cartão e do modo treino | 4, 16 |
| `domain/WeekStrip.kt` (novo) | Os 7 dias da faixa da semana | 5 |
| `ui/components/ZenSheet.kt` (novo) | Folha do ⋯ vestida com `Zen.look` | 6 |
| `ui/components/MuscleColors.kt` (novo) | `MuscleGroup.color()` (sai do `TrainSession.kt`) | 6 |
| `ui/screens/HomeScreen.kt`, `ui/screens/DojoSheets.kt` (novo), `ui/screens/DojoWeekStrip.kt` (novo) | Cartões do Dojo, folhas do ⋯ e faixa da semana | 6, 7, 8 |
| `ui/screens/HistoryContent.kt` (substitui `HistoryScreen.kt`) | Lista e detalhe do histórico, sem rolagem própria | 9 |
| `ui/screens/WeightLog.kt` (novo) | Registrar, listar, editar e remover peso | 10 |
| `ui/components/ZenTabs.kt`, `ui/components/Accordion.kt` (novos) | Abas internas e sanfona | 11 |
| `ui/screens/WorkoutScreen.kt`, `ui/screens/MyExercises.kt` (novo) | Treino com Ficha · Agenda · Exercícios | 12 |
| `ui/screens/DietScreen.kt`, `ui/screens/FoodsScreen.kt` | Dieta com Cardápio · Meta · Alimentos | 13 |
| `ui/theme/Theme.kt`, `ui/theme/Color.kt`, `ui/screens/StylePreview.kt` (novo) | Paleta e forma por estilo para as miniaturas | 14 |
| `ui/screens/SettingsScreen.kt`, `ui/screens/SettingsPages.kt` (novo), `domain/SettingsSummary.kt` (novo), `ui/screens/UpdateCard.kt` | Sete linhas, subpáginas e linha de atualização | 15 |
| `domain/WorkoutSummary.kt` (novo), `ui/ZenViewModel.kt` | Resumo do treino do dia; `completeWorkout()`, `todaySummary` | 16 |
| `ui/screens/TrainSession.kt`, `ui/screens/TrainFooter.kt` (novo), `ui/screens/TrainConclusion.kt` (novo) | Modo treino novo | 17–19 |
| `androidTest/.../ui/TestApp.kt` | `withWorkoutEveryDay()`, `withDiet()`, `withLoggedSet()` | 6, 7, 9 |

Caminhos relativos a `app/src/main/java/com/zenkaigym/` (código) e `app/src/` (testes).

---

## Fase 1 · Casca da navegação

### Task 1: Teste de cobertura das traduções

Hoje uma frase sem tradução aparece em português, sem nenhum aviso. Este teste protege todas as tarefas seguintes. Ele já passa no código atual (os 605 textos literais têm tradução).

**Arquivos:**
- Criar: `app/src/test/java/com/zenkaigym/domain/TranslationsCoverageTest.kt`

- [ ] **Passo 1: Escrever o teste**

```kotlin
class TranslationsCoverageTest {
    private val literal = Regex("""\btr\("([^"\\]*)"""")

    @Test fun todoTextoComTrTemTraducao() {
        val root = File("src/main/java") // os testes unitários rodam a partir do módulo app
        assertTrue("pasta de código não encontrada: ${root.absolutePath}", root.isDirectory)
        val missing = root.walk().filter { it.extension == "kt" && it.name != "Translations.kt" }
            .flatMap { f -> literal.findAll(f.readText()).map { f.name to it.groupValues[1] } }
            .filter { (_, pt) -> pt !in Translations.en }
            .map { (file, pt) -> "$file: \"$pt\"" }.distinct().toList()
        assertTrue("Textos sem tradução:\n" + missing.joinToString("\n"), missing.isEmpty())
    }
}
```

- [ ] **Passo 2: Rodar e ver passar**: `./gradlew :app:testDebugUnitTest --tests "com.zenkaigym.domain.TranslationsCoverageTest"`. Esperado: PASS.
- [ ] **Passo 3: Provar que pega a falta:** troque temporariamente um texto em `HomeScreen.kt` por `tr("Texto de prova")`, rode e veja FAIL citando `HomeScreen.kt: "Texto de prova"`. Desfaça a troca.
- [ ] **Passo 4: Commit** `test: garante tradução para todo texto com tr()`

### Task 2: Sem barra, menu e "voltar leva ao Dojo"

Desenho: seções 5.1 e 5.4 (só a estrutura; os dados dos quadros entram na Tarefa 3).

**Arquivos:**
- Criar: `ui/components/HeaderBar.kt`, `ui/screens/MenuScreen.kt`
- Modificar: `ui/nav/AppNav.kt`, `ui/components/ZenComponents.kt` (`ZenHeader`, `ZenFixedHeader`), `ui/components/ZenIcons.kt`, `ui/screens/HomeScreen.kt`, `WorkoutScreen.kt`, `DietScreen.kt`, `ProgressScreen.kt`, `ProfileScreen.kt`, `SettingsScreen.kt`, `domain/Translations.kt`
- Reescrever: `app/src/androidTest/java/com/zenkaigym/ui/NavigationUiTest.kt`

**Interfaces:**
- Produz:
  - `Routes.MENU = "menu"`.
  - `@Composable fun HeaderBar(onBack: () -> Unit, backLabel: String = tr("Dojo"), actions: @Composable RowScope.() -> Unit = {})`: `ZenButton(backLabel, leading = ZenIcons.ArrowLeft, accent = Zen.p.card, small = true)`, espaço e `actions` à direita. Entra no parâmetro `back` de `ZenFixedHeader`.
  - `@Composable fun MoreButton(onClick: () -> Unit, description: String = tr("Mais opções"))`: botão só com ícone `ZenIcons.More` e `contentDescription`.
  - `ZenHeader`/`ZenFixedHeader(…, trailing: (@Composable () -> Unit)? = null)`: quando informado, substitui o `KanjiBadge` da direita (`{}` esconde).
  - `ZenIcons.More`: `fill("More", …)` com três círculos de raio 1,8 em (5,12), (12,12) e (19,12).
  - `HomeScreen(viewModel, onNavigate: (String) -> Unit, showMenuSeal: Boolean)`
  - `MenuScreen(viewModel: ZenViewModel, onOpen: (String) -> Unit, onClose: () -> Unit)`
  - `WorkoutScreen(viewModel, onBack)`, `DietScreen(viewModel, onBack, onOpenFoods)`, `ProfileScreen(viewModel, onBack, onOpenHistory, onOpenProgress)`, `SettingsScreen(viewModel, onBack: () -> Unit = {})` (o padrão mantém o `SettingsUiTest` compilando). `ProgressScreen(viewModel, onBack)` não muda de assinatura.

- [ ] **Passo 1: Reescrever o `NavigationUiTest`.** Troque o `@Before` com `setContent` por um `start(wide: Boolean = false, setup: TestApp.() -> TestApp = { withProfile() })` chamado em cada teste (o `TestApp` também passa a ser criado ali). Com `wide`, envolva o `AppNav` em `CompositionLocalProvider(LocalConfiguration provides Configuration(<config atual>).apply { screenWidthDp = 900; screenHeightDp = 700 })`. Use `Espresso.pressBack()` para o voltar do sistema. Testes:

```kotlin
@Test fun abreNoDojo() { start(); rule.onNodeWithContentDescription("Abrir menu").assertIsDisplayed() }

@Test fun menuAbreCadaTela() {
    start()
    listOf(
        "TREINO" to "Dias, horários e ficha", "DIETA" to "Meta, horários e refeições",
        "EVOLUÇÃO" to "Treino, dieta, peso e conquistas", "PERFIL" to "Seus dados",
        "AJUSTES" to "Aparência, estilo, treino, semana, idioma e informações"
    ).forEach { (tile, subtitle) ->
        openMenu()                                  // toca em "Abrir menu" e espera "Para onde agora?"
        rule.onNodeWithText(tile).performClick()
        waitForText(subtitle)
        rule.onNodeWithText("DOJO").performClick()  // botão "← Dojo" do cabeçalho
        rule.onNodeWithContentDescription("Abrir menu").assertIsDisplayed()
    }
}

@Test fun voltarDeQualquerTelaLevaAoDojo() {
    start(); openMenu(); rule.onNodeWithText("TREINO").performClick(); waitForText("Dias, horários e ficha")
    Espresso.pressBack()
    rule.onNodeWithContentDescription("Abrir menu").assertIsDisplayed()
    assertEquals(0, rule.onAllNodesWithTextCount("Para onde agora?"))
}

@Test fun fecharMenuVoltaAoDojo()  // abre o menu, toca em "Fechar" e vê "Abrir menu"

@Test fun dietaAbreMeusAlimentosEVolta() {
    // Fase 1: Alimentos ainda é subtela da Dieta. Voltar → Dieta; voltar de novo → Dojo.
}

@Test fun telaLargaMostraColunaSemSelo() {
    start(wide = true)
    assertEquals(0, rule.onAllNodes(hasContentDescription("Abrir menu")).fetchSemanticsNodes().size)
    rule.onNodeWithText("TREINO").performClick(); waitForText("Dias, horários e ficha")
    Espresso.pressBack()
    assertEquals(0, rule.onAllNodesWithTextCount("Dias, horários e ficha"))
}
```

- [ ] **Passo 2: Rodar e ver falhar** (`NavigationUiTest`): não existe "Abrir menu".
- [ ] **Passo 3: Implementar.**
  - `AppNav`: tire `BottomBar` e o `TabOrder`. Navegue sempre por `go(route)`: para `HOME`, `popBackStack(Routes.HOME, inclusive = false)`; para o resto, `navigate(route) { popUpTo(Routes.HOME); launchSingleTop = true }`. **Sem `saveState`/`restoreState`:** cada tela abre do começo (a Tarefa 13 depende disso).
  - Transições: entrar desliza da direita (direção 1); voltar ao Dojo e os `pop` deslizam da esquerda (−1). Mantenha as curvas de hoje e o `motion`.
  - `FOODS` e `HISTORY` continuam como subtelas empilhadas (`navigate` simples) até as Tarefas 9 e 13.
  - Coluna larga (`layout.useRail`): reaproveite `SideRail`/`TabItem` com 6 entradas em `kanji + rótulo` (Dojo 道, Treino 体, Dieta 食, Evolução 進, Perfil 己, Ajustes 設, nas cores das Restrições), sem os dados ao vivo. Destaque a tela atual com `parentTab` (`FOODS → DIET`, `HISTORY → PROFILE`, `MENU → HOME`). Se a tela virar larga com o menu aberto, volte ao Dojo (`LaunchedEffect(layout.useRail)`).
  - Dojo: `trailing` do cabeçalho com o selo 道 (`KanjiBadge`, `Aka`, 56 dp) e o rótulo "Menu" embaixo, num nó só (`semantics(mergeDescendants = true) { contentDescription = tr("Abrir menu") }`, `Role.Button`). Com `showMenuSeal = false`, passe `trailing = {}`.
  - `MenuScreen`: `ZenPage` + `ZenFixedHeader(tr("Menu"), "門", subtitle = tr("Para onde agora?"), back = { HeaderBar(onClose, backLabel = tr("Fechar")) })`. Por enquanto, cinco quadros simples em três `SectionTitle` ("Plano", "Progresso", "Você"), cada um `ZenCard(accent)` clicável com kanji e título em maiúsculas.
  - Telas do menu: `back = { HeaderBar(onBack) }` em Treino, Dieta, Evolução (substitui "Voltar ao perfil"), Perfil e Ajustes. Alimentos e Histórico mantêm os rótulos atuais nesta fase.
  - Perfil: `onOpenProgress` passa a chamar `go(Routes.PROGRESS)` (voltar da Evolução leva ao Dojo).
  - Traduza "Menu", "Abrir menu", "Para onde agora?", "Plano", "Progresso", "Você", "Mais opções".
- [ ] **Passo 4: Rodar e ver passar**: `NavigationUiTest`, `TranslationsCoverageTest` e `./gradlew :app:testDebugUnitTest` (suíte inteira). Esperado: PASS.
- [ ] **Passo 5: Commit** `feat: navegação sem barra, com menu e volta ao Dojo`

### Task 3: Quadros do menu com dados ao vivo

Desenho: seção 5.4.

**Arquivos:**
- Modificar: `ui/screens/MenuScreen.kt`, `ui/components/ZenComponents.kt`, `domain/Translations.kt`, `NavigationUiTest.kt`

**Interfaces:**
- Produz: `@Composable fun StyleBackdrop(modifier: Modifier)` (linhas de velocidade, brilhos ou grade, conforme `Zen.look`), usado por `ZenPage` (com as mesmas alturas de hoje: 260/320 dp) e pelos quadros grandes.

- [ ] **Passo 1: Escrever o teste** `menuSemDadosMostraMarcadores` no `NavigationUiTest`: com `TestApp.create()` sem `withProfile()`, abre o menu e confere que "Sem ficha ainda" e "Sem perfil ainda" aparecem e que tocar em "PERFIL" abre "Seus dados".
- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar.**
  - Layout: "Plano" com Treino e Dieta lado a lado, em quadros altos; "Progresso" com Evolução num quadro largo, com `ZenProgressBar(level.fraction, Daidai)`; "Você" com Perfil e Ajustes lado a lado, em quadros baixos. Nos quadros grandes (Treino, Dieta, Evolução), `StyleBackdrop` fica atrás do conteúdo, recortado pelo `Zen.look.shape`.
  - Dados (linha embaixo do título):
    - Treino: `tr(activeWorkout.plan.splitName) + " · " + tr("{0} dias de treino", availability.windowsByDay().size)`; sem ficha, `tr("Sem ficha ainda")`.
    - Dieta: `tr("{0} kcal · {1} refeições", profile.targetKcal ?: suggestedTarget.kcal, mealSlots.size)`; sem meta, `tr("Sem meta ainda")`.
    - Evolução: `tr("Nv {0} · {1} / {2} XP", level.level, level.xpIntoLevel, level.xpForNext)`; sem estatísticas ainda, "—".
    - Perfil: `"%.1f kg".format(weightKg) + " · " + goal.label`; sem perfil, `tr("Sem perfil ainda")`.
    - Ajustes: `style.label + " · " + themeMode.label`.
  - Traduza os textos novos.
- [ ] **Passo 4: Rodar e ver passar** (`NavigationUiTest`, `TranslationsCoverageTest`).
- [ ] **Passo 5: Conferir à mão** no ZenTest: o menu nos três estilos, claro e escuro, mais tela pequena com letra grande (rola sem cortar). Anote o que viu no commit.
- [ ] **Passo 6: Commit** `feat: menu com dados atuais e fundo do estilo nos quadros`

---

## Fase 2 · Dojo

### Task 4: Regras do cartão do treino (domínio)

Desenho: seção 5.2 (treino de hoje).

**Arquivos:**
- Criar: `domain/MuscleKanji.kt`, `domain/TrainingFlow.kt`, `test/.../domain/MuscleKanjiTest.kt`, `test/.../domain/TrainingFlowTest.kt`
- Modificar: `ui/screens/TrainSession.kt` (remove o `kanji()` privado e importa o do domínio)

**Interfaces:**
- Produz:
  - `fun MuscleGroup.kanji(): String` (mesma tabela do `TrainSession.kt` de hoje) e `fun List<MuscleGroup>.distinctByKanji(): List<MuscleGroup>` (primeiro grupo de cada kanji, na ordem recebida).
  - `data class SetCount(val done: Int, val total: Int) { val complete: Boolean get() = done >= total }`
  - `fun setCount(plan: List<Pair<Long, Int>>, logged: Collection<SetLog>): SetCount`. `plan` = (exerciseId, nº de séries). Só contam séries de trabalho (`setIndex` de 1 até o nº de séries), igual ao `doneSets` do modo treino.
  - `enum class DojoWorkoutState { NO_PLAN, REST, PENDING, IN_PROGRESS, DONE, SKIPPED }`
  - `object TrainingFlow { fun dojoState(hasPlan: Boolean, hasSession: Boolean, done: Boolean, skipped: Boolean, count: SetCount): DojoWorkoutState }`. Ordem: sem ficha → `NO_PLAN`; sem sessão → `REST`; `done` → `DONE`; `skipped` → `SKIPPED`; `count.done > 0` → `IN_PROGRESS`; senão `PENDING`.

- [ ] **Passo 1: Escrever os testes**

```kotlin
// MuscleKanjiTest
@Test fun gruposDoMesmoKanjiAparecemUmaVezNaOrdem() {
    val row = listOf(QUADS, CHEST, GLUTES, LATS, CHEST, CARDIO).distinctByKanji()
    assertEquals(listOf("脚", "胸", "背", "走"), row.map { it.kanji() })
}
@Test fun todoGrupoTemUmDosSeteKanji() {
    MuscleGroup.entries.forEach { assertTrue(it.name, it.kanji() in setOf("胸", "背", "脚", "肩", "腕", "腹", "走")) }
}

// TrainingFlowTest
private fun log(ex: Long, i: Int) = SetLog(epochDay = 0, exerciseId = ex, setIndex = i, reps = 10, weightKg = 20.0)
@Test fun contaSoSeriesDeTrabalhoDaFicha() {
    val logged = listOf(log(1, 1), log(1, 2), log(1, -1), log(2, 1), log(9, 1), log(1, 4))
    assertEquals(SetCount(done = 3, total = 5), setCount(listOf(1L to 3, 2L to 2), logged))
}
@Test fun estadoDoCartaoDoTreino() {
    val none = SetCount(0, 5); val some = SetCount(2, 5)
    assertEquals(NO_PLAN, TrainingFlow.dojoState(false, false, false, false, none))
    assertEquals(REST, TrainingFlow.dojoState(true, false, false, false, none))
    assertEquals(PENDING, TrainingFlow.dojoState(true, true, false, false, none))
    assertEquals(IN_PROGRESS, TrainingFlow.dojoState(true, true, false, false, some))
    assertEquals(DONE, TrainingFlow.dojoState(true, true, true, false, some))
    assertEquals(SKIPPED, TrainingFlow.dojoState(true, true, false, true, some))
}
```

- [ ] **Passo 2: Rodar e ver falhar** (não compila).
- [ ] **Passo 3: Implementar** as funções acima e trocar o uso em `TrainSession.kt`.
- [ ] **Passo 4: Rodar e ver passar** (`MuscleKanjiTest`, `TrainingFlowTest`) e compilar.
- [ ] **Passo 5: Commit** `feat: kanji dos músculos e estados do treino do dia no domínio`

### Task 5: Faixa da semana (domínio)

Desenho: seção 5.2 (faixa da semana).

**Arquivos:**
- Criar: `domain/WeekStrip.kt`, `test/.../domain/WeekStripTest.kt`

**Interfaces:**
- Produz: `data class WeekStripDay(val dayOfWeek: Int, val done: Boolean, val today: Boolean, val missed: Boolean)` e `object WeekStrip { fun days(start: WeekStart, today: Long, weekDots: List<Boolean>, plannedDays: Set<Int>): List<WeekStripDay> }`.
  - `weekDots` é o `ProgressStats.weekDots` (índice i = `start.weekStartOf(today) + i`); vazio ou nulo conta como "sem treino".
  - `plannedDays` são os dias com horário em `availability.windowsByDay().keys`.
  - `missed` = dia planejado, antes de hoje e sem treino.

- [ ] **Passo 1: Escrever os testes** (2026-11-23 é segunda-feira):

```kotlin
private val monday = LocalDate.of(2026, 11, 23).toEpochDay()
@Test fun ordemSegueOInicioDaSemana() {
    val days = WeekStrip.days(WeekStart.SUNDAY, monday + 2, List(7) { false }, emptySet())
    assertEquals(listOf(7, 1, 2, 3, 4, 5, 6), days.map { it.dayOfWeek })
    assertEquals(3, days.indexOfFirst { it.today })
}
@Test fun diaPlanejadoQuePassouSemTreinoFicaPerdido() {
    val days = WeekStrip.days(WeekStart.MONDAY, monday + 2, List(7) { false }, setOf(1, 3, 5))
    assertEquals(listOf(true, false, false, false, false, false, false), days.map { it.missed })
}
@Test fun diaComTreinoNaoContaComoPerdido() {
    val days = WeekStrip.days(WeekStart.MONDAY, monday + 2, listOf(true, false, true, false, false, false, false), setOf(1, 3))
    assertEquals(listOf(true, false, true, false, false, false, false), days.map { it.done })
    assertTrue(days.none { it.missed })
}
@Test fun semanaSemDadosNaoMarcaNada() {
    val days = WeekStrip.days(WeekStart.MONDAY, monday, emptyList(), emptySet())
    assertEquals(7, days.size); assertTrue(days.none { it.done || it.missed })
}
```

- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar `WeekStrip.days`.**
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Commit** `feat: dias da faixa da semana no domínio`

### Task 6: Folha do ⋯ e cartão do treino no Dojo

Desenho: seções 5.2 (treino de hoje) e 5.3 (folha do ⋯ do treino).

**Arquivos:**
- Criar: `ui/components/ZenSheet.kt`, `ui/components/MuscleColors.kt`, `ui/screens/DojoSheets.kt`, `app/src/androidTest/java/com/zenkaigym/ui/DojoUiTest.kt`
- Modificar: `ui/screens/HomeScreen.kt`, `ui/screens/TrainSession.kt` (`color()` sai de lá), `androidTest/.../TestApp.kt`, `domain/Translations.kt`

**Interfaces:**
- Consome: `MuscleGroup.kanji()`, `distinctByKanji()`, `setCount()`, `TrainingFlow.dojoState()` (Tarefa 4); `MoreButton` (Tarefa 2).
- Produz:
  - `@Composable fun ZenSheet(onDismiss: () -> Unit, accent: Color, title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit)`. Decisão: `ModalBottomSheet` do Material 3 com `skipPartiallyExpanded = true`, `containerColor = Zen.p.card`, faixa de acento no topo, alça própria e só os cantos de cima no formato do estilo (retos no Shōnen, `RoundedCornerShape(topStart, topEnd = look.corner)` no Kawaii, `CutCornerShape` igual no Isekai).
  - `@Composable fun SheetOption(title: String, description: String, onClick: () -> Unit, enabled: Boolean = true)`: linha de no mínimo 48 dp, título e frase; tocar fecha a folha e executa a ação.
  - `fun MuscleGroup.color(): Color` público, em `MuscleColors.kt`.
  - `TestApp.withWorkoutEveryDay()`: `withProfile()`, depois a disponibilidade salva de novo com os 7 dias, `repo.generateWorkoutProposal()` e `repo.acceptWorkout(repo.proposedWorkout.first()!!.plan.id)`. Assim há sessão hoje em qualquer dia da semana.

- [ ] **Passo 1: Escrever o `DojoUiTest`** (`setContent { ZenkaigymTheme(darkTheme = false) { HomeScreen(app.viewModel, onNavigate = {}, showMenuSeal = true) } }`, com `withWorkoutEveryDay()`):

```kotlin
@Test fun marcarComoFeitoPeloMenuDoCartao() {
    rule.onNodeWithContentDescription("Mais opções do treino").performClick()
    rule.onNodeWithText("Marcar como feito").performClick()
    rule.waitUntil(5_000) { app.viewModel.workoutDone.value }
    rule.onNodeWithText("TREINO FEITO").assertIsDisplayed()
}
@Test fun desfazerConclusaoPeloMenuDoCartao()  // depois de feito, o ⋯ só oferece "Desfazer conclusão", que volta a "TREINAR AGORA"
@Test fun naoRealizadoPeloMenuDoCartao()       // "Não realizado" mostra "NÃO REALIZADO" e "DESFAZER"
@Test fun seriesRegistradasMostramEmAndamento() {
    // app.repo.logSet(SetLog(...)) com a série 1 do 1º exercício de app.viewModel.todayWorkout.value.session
    rule.onNodeWithText("Em andamento", substring = true).assertIsDisplayed()
    rule.onNodeWithText("CONTINUAR").assertIsDisplayed()
}
```

- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar `ZenSheet`, `SheetOption` e o novo `TodayWorkout`.**
  - Estado com `TrainingFlow.dojoState(workout != null, today != null, done, skipped, setCount(today.items.map { it.exercise.id to it.item.sets }, setsToday))`, onde `today` já vem com as trocas do dia (`withSwaps`, como hoje).
  - Fileira de kanji: no lugar dos três nomes, `items.map { it.exercise.group }.distinctByKanji()` como `KanjiBadge(group.kanji(), color = group.color(), size = 30.dp)` numa `Row` com espaço de 6 dp. Some em `compactHeight`, como os nomes hoje.
  - Botões por estado:
    - `PENDING`: "Treinar agora" (`Aka`, preenchido, peso 1).
    - `IN_PROGRESS`: acima dos botões, `tr("Em andamento · {0}/{1}", done, total)` e `ZenProgressBar(done/total, Ki)`; o principal vira "Continuar".
    - `DONE`: "Treino feito" (`Midori`, `trailing = Check`), que chama `startTraining` como hoje.
    - `SKIPPED`: "NÃO REALIZADO" + "Desfazer", como hoje.
    - Ao lado do principal, sempre `MoreButton(description = tr("Mais opções do treino"))`.
  - Os estados `NO_PLAN` e `REST` não mudam.
  - Opções da folha (título = nome do treino; subtítulo = horário da agenda):
    - `PENDING`/`IN_PROGRESS`:
      - "Marcar como feito" · "Conta o treino de hoje sem registrar séries." → `setWorkoutDone(true)`
      - "Não realizado" · "A sessão passa para o próximo dia de treino." → `setWorkoutSkipped(true)`
      - "Trocar o treino de hoje" · "Escolhe outra sessão da semana só para hoje." → abre o `ChangeWorkoutDialog`
    - `DONE`: "Desfazer conclusão" · "O treino de hoje volta a ficar pendente." → `setWorkoutDone(false)`
    - `SKIPPED`: só "Trocar o treino de hoje" (decisão minha: mantém a troca que hoje existe nesse estado).
  - Traduza os textos novos.
- [ ] **Passo 4: Rodar e ver passar** (`DojoUiTest`, `NavigationUiTest`, `TranslationsCoverageTest`, unitários).
- [ ] **Passo 5: Commit** `feat: cartão do treino com um botão principal, ⋯ e kanji dos músculos`

### Task 7: Cartão da refeição com ⋯ e chips de horário

Desenho: seções 5.2 (refeição) e 5.3 (⋯ da refeição).

**Arquivos:**
- Modificar: `ui/screens/HomeScreen.kt` (`NextMeal`), `ui/screens/DojoSheets.kt`, `ui/components/ZenComponents.kt` (`ZenChip`), `DojoUiTest.kt`, `TestApp.kt`, `domain/Translations.kt`

**Interfaces:**
- Consome: `ZenSheet`, `SheetOption` (Tarefa 6).
- Produz:
  - `ZenChip(…, border: Dp? = null)`: `null` mantém `look.border`.
  - `TestApp.withDiet()`: `withProfile()` + `repo.ensureDietSuggestion()` + `repo.acceptDiet(repo.proposedDiet.first()!!.plan.id)`.

- [ ] **Passo 1: Escrever os testes** (com `withDiet()`):

```kotlin
@Test fun naoRealizeiPeloMenuDaRefeicao() {
    rule.onNodeWithContentDescription("Mais opções da refeição").performClick()
    rule.onNodeWithText("Não realizei").performClick()
    rule.waitUntil(5_000) { app.viewModel.mealsSkipped.value.isNotEmpty() }
    rule.onNodeWithText("NÃO REALIZADA", substring = true).assertIsDisplayed()
}
@Test fun feitoContinuaAUmToque() {
    rule.onAllNodesWithText("FEITO")[0].performClick()
    rule.waitUntil(5_000) { app.viewModel.mealsDone.value.isNotEmpty() }
}
// No NavigationUiTest (precisa do AppNav), com start(setup = { withDiet() }):
@Test fun verNoCardapioAbreADieta()  // ⋯ da refeição → "Ver no cardápio" → espera "Meta, horários e refeições"
```

- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar.**
  - Botões da página: pendente → "Feito" (`Midori`, `trailing = Check`, peso 1) + `MoreButton(description = tr("Mais opções da refeição"))`; feita ou não realizada → "Desfazer" + `MoreButton`. O botão "+" e o "Não realizei" saem do cartão. Continua valendo: "Feito" grava as kcal ajustadas.
  - Folha: título = nome da refeição; subtítulo = `"$horário · ${tr("{0} kcal", ajustadas)}"`.
    - "Comi outra coisa" · "Registra um prato da sua lista ou um prato livre, em kcal." → `addingExtra = meal` (fluxo atual do `AddExtraDialog`)
    - "Não realizei" · "As próximas refeições do dia se ajustam." → `setMealSkipped(type, true)` (só aparece se a refeição não estiver feita nem marcada como não realizada)
    - "Ver no cardápio" · "Abre a Dieta no dia de hoje." → `onNavigate(Routes.DIET)`
  - Chips "HOJE x/y": feita = `selected` com `accent = Midori`; a da vez = `border = look.border`; as outras = `border = look.thinBorder`; não realizada = marca `Close` (com até 4 refeições, como hoje). Tocar continua rolando o pager.
  - Traduza os textos novos.
- [ ] **Passo 4: Rodar e ver passar** (`DojoUiTest`, `NavigationUiTest`, `TranslationsCoverageTest`).
- [ ] **Passo 5: Commit** `feat: cartão da refeição com Feito e ⋯`

### Task 8: Faixa da semana no Dojo

Desenho: seção 5.2 (faixa da semana).

**Arquivos:**
- Criar: `ui/screens/DojoWeekStrip.kt`
- Modificar: `ui/screens/HomeScreen.kt`, `NavigationUiTest.kt`, `domain/Translations.kt`

**Interfaces:**
- Consome: `WeekStrip.days()` (Tarefa 5); `progressStats` (`weekDots`, `weekSessions`, `plannedPerWeek`, `level`), `availability`, `weekStart` e `clock` do ViewModel.
- Produz: `@Composable fun DojoWeekStrip(days: List<WeekStripDay>, sessions: Int, planned: Int, level: LevelInfo?, onClick: () -> Unit)`

- [ ] **Passo 1: Escrever o teste** `faixaDaSemanaAbreAEvolucao` no `NavigationUiTest`: toca no nó com `contentDescription` "Abrir evolução" e espera "Treino, dieta, peso e conquistas".
- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar.**
  - Sem cartão, depois dos cartões (largura total também em `twoColumns`).
  - Linha de cima: `tr("Semana · {0}/{1} treinos", sessions, planned)` à esquerda e `tr("Nv {0} · {1} / {2} XP", …)` à direita (some se `level == null`).
  - Sete quadradinhos com `look.smallShape` e a inicial do dia (`WEEK_DAYS_SHORT[dow - 1].take(1)`) embaixo:
    - feito = fundo `Ki` e texto `Sumi`;
    - hoje = borda `look.border`;
    - perdido = borda tracejada (`drawBehind` com `PathEffect.dashPathEffect` no contorno de `smallShape`);
    - os outros = borda `look.thinBorder`.
  - A faixa inteira é um botão (`Role.Button`, `contentDescription = tr("Abrir evolução")`) que chama `onNavigate(Routes.PROGRESS)`.
- [ ] **Passo 4: Rodar e ver passar.**
- [ ] **Passo 5: Conferir à mão** no ZenTest: o Dojo nos três estilos, claro e escuro, em retrato e deitado (dois cartões lado a lado). Anote no commit.
- [ ] **Passo 6: Commit** `feat: faixa da semana no Dojo`

---

## Fase 3 · Evolução e Perfil

As abas da Evolução **já têm rótulo escrito** no código atual (commit 4d17862: `ZenChip(tr(t.pt), …, mark = t.icon)` com rolagem). O item da seção 5.7 sobre rótulos não precisa de mudança. A Tarefa 9 só confirma isso com o teste.

### Task 9: Histórico dentro da aba Treino

Desenho: seção 5.7.

**Arquivos:**
- Criar: `ui/screens/HistoryContent.kt`, `app/src/androidTest/java/com/zenkaigym/ui/ProgressUiTest.kt`
- Apagar: `ui/screens/HistoryScreen.kt`
- Modificar: `ui/screens/ProgressScreen.kt`, `ui/screens/ProfileScreen.kt` (o botão "Histórico" sai), `ui/nav/AppNav.kt` (a rota `HISTORY` sai), `TestApp.kt`

**Interfaces:**
- Produz:
  - `@Composable fun HistoryList(exercises: List<Exercise>, onSelect: (Exercise) -> Unit)` e `@Composable fun HistoryDetail(exercise: Exercise, viewModel: ZenViewModel, onBack: () -> Unit)`. O conteúdo é o mesmo de hoje, mas em `Column` comum (sem `LazyColumn`, `ZenPage` nem cabeçalho próprio), porque fica dentro da rolagem da Evolução. O detalhe começa com `HeaderBar(onBack, backLabel = tr("Treino"))`, o nome e o grupo.
  - `TestApp.withLoggedSet()`: registra uma série de trabalho hoje no primeiro exercício de `repo.exercises.first()`.

- [ ] **Passo 1: Escrever o `ProgressUiTest`** (`ProgressScreen` direto, com `withLoggedSet()`):

```kotlin
@Test fun abasTemRotuloEscrito() { listOf("Resumo", "Treino", "Dieta", "Peso", "Conquistas").forEach { rule.onNodeWithText(it).assertExists() } }
@Test fun historicoAbreDentroDaAbaTreino() {
    rule.onNodeWithText("Treino").performClick()
    rule.onNodeWithText(<nome do exercício>).performScrollTo().performClick()
    rule.onNodeWithText("Últimas sessões", ignoreCase = true).performScrollTo().assertIsDisplayed()
}
@Test fun voltarFechaDetalheDoHistorico() {
    // abre o detalhe como acima; Espresso.pressBack(); a lista de exercícios volta e a tela continua na Evolução
}
```

- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar.**
  - `ProgressScreen` guarda `var historyId by rememberSaveable { mutableStateOf<Long?>(null) }`, zera ao trocar de aba e usa `BackHandler(enabled = historyId != null) { historyId = null }`.
  - Na aba Treino: com `historyId`, mostre só o `HistoryDetail`; sem ele, o conteúdo atual e, no fim, `SectionTitle(tr("Histórico"))` + `HistoryList(viewModel.loggedExercises)`.
  - Tire a rota `HISTORY`, o `onOpenHistory` do Perfil e o botão "Histórico".
- [ ] **Passo 4: Rodar e ver passar** (`ProgressUiTest`, `NavigationUiTest`, `TranslationsCoverageTest`).
- [ ] **Passo 5: Commit** `feat: histórico por exercício dentro da aba Treino da Evolução`

### Task 10: Peso na Evolução e Perfil enxuto

Desenho: seções 5.7 (aba Peso) e 5.8.

**Arquivos:**
- Criar: `ui/screens/WeightLog.kt`
- Modificar: `ui/screens/ProgressScreen.kt` (`WeightTab`, `LevelCard`), `ui/screens/ProfileScreen.kt`, `ui/nav/AppNav.kt`, `ProgressUiTest.kt`, `domain/Translations.kt`

**Interfaces:**
- Produz: `@Composable fun WeightLogCard(weights: List<WeightEntry>, onRegister: (Double) -> Unit, onEdit: (Long, Double) -> Unit, onRemove: (Long) -> Unit)`. Vem do `WeightCard` atual: campo "Novo peso (kg)", "Registrar" (aceita de 30 a 300 kg), os últimos 8 registros e as janelas de editar e remover. Sem o número grande e sem o gráfico pequeno, porque a aba já tem o `WeightTrendChart`. Acento `Daidai`.
- `ProfileScreen(viewModel, onBack)` (perde `onOpenProgress`; o `onOpenHistory` saiu na Tarefa 9).

- [ ] **Passo 1: Escrever o teste** `registrarPesoNaAbaPeso` no `ProgressUiTest`: aba "Peso", digita "80,5" no campo (`onNode(hasSetTextAction()).performTextInput(…)`; é o único campo da aba), toca "REGISTRAR" e espera `app.viewModel.weights.value.any { it.weightKg == 80.5 }`. E `textoVazioApontaParaAPropriaAba`: sem registros, aparece "Registre seu peso aqui ao menos duas vezes para ver a evolução."
- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar.**
  - `WeightTab` recebe os três callbacks (`viewModel::addWeight`, `editWeight`, `removeWeight`) e termina com `WeightLogCard` **nos dois ramos** (com e sem dados suficientes).
  - Troque o texto vazio pela frase do teste (com tradução).
  - Perfil: tire `LevelCard`, os botões "Evolução"/"Histórico" e o `WeightCard`. O acento passa de `Daidai` para `Sakura` (cabeçalho, cartões, chips, botões). O campo "Peso" do cartão Corpo continua igual (salvar com outro peso ainda cria um registro).
  - `LevelCard` vira `private` e perde `onClick`.
- [ ] **Passo 4: Rodar e ver passar** (`ProgressUiTest`, `NavigationUiTest`, `TranslationsCoverageTest`, unitários).
- [ ] **Passo 5: Commit** `feat: registro de peso na Evolução; Perfil só com os dados da pessoa`

---

## Fase 4 · Treino e Dieta

### Task 11: Abas internas e sanfona (componentes)

**Arquivos:**
- Criar: `ui/components/ZenTabs.kt`, `ui/components/Accordion.kt`
- Modificar: `ui/screens/ProgressScreen.kt` (usa `ZenTabs` no lugar de `ProgressTabs`)

**Interfaces:**
- Produz:
  - `data class ZenTab(val label: String, val accent: Color, val icon: ImageVector? = null)` e `@Composable fun ZenTabs(tabs: List<ZenTab>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier)`. É o `ProgressTabs` de hoje, generalizado: rolagem horizontal, aba escolhida sempre à vista e esmaecido nas bordas.
  - `@Composable fun AccordionCard(title: String, meta: String?, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, accent: Color? = null, content: @Composable ColumnScope.() -> Unit)`:
    - Cabeçalho clicável (`Role.Button`, `stateDescription` aberto/fechado), com `ChevronDown`/`ChevronUp`.
    - O conteúdo abre com `AnimatedVisibility`, ou sem animação quando `LocalMotionEnabled` está desligado.

- [ ] **Passo 1: Trocar `ProgressTabs` por `ZenTabs`** em `ProgressScreen`, sem mudar o visual.
- [ ] **Passo 2: Rodar** `ProgressUiTest` (as abas continuam com rótulo e o histórico continua funcionando). Esperado: PASS.
- [ ] **Passo 3: Implementar `AccordionCard`** (o uso e os testes vêm nas Tarefas 12 e 13).
- [ ] **Passo 4: Compilar.**
- [ ] **Passo 5: Commit** `refactor: abas internas e sanfona compartilhadas`

### Task 12: Treino com Ficha · Agenda · Exercícios

Desenho: seção 5.5.

**Arquivos:**
- Criar: `ui/screens/MyExercises.kt`, `app/src/androidTest/java/com/zenkaigym/ui/WorkoutUiTest.kt`
- Modificar: `ui/screens/WorkoutScreen.kt`, `domain/Translations.kt`

**Interfaces:**
- Consome: `ZenTabs`, `AccordionCard` (Tarefa 11); `HeaderBar`, `MoreButton` (Tarefa 2); `ZenSheet`, `SheetOption` (Tarefa 6).
- Produz: `@Composable fun MyExercisesList(exercises: List<Exercise>, viewModel: ZenViewModel)`. É o conteúdo do `MyExercisesDialog` como tela: lista, renomear e excluir, com as janelas de confirmação de hoje. O `MyExercisesDialog` deixa de existir.

- [ ] **Passo 1: Escrever o `WorkoutUiTest`** (`WorkoutScreen` direto, com `withWorkoutEveryDay()`):

```kotlin
@Test fun abasDoTreino() { listOf("Ficha", "Agenda", "Exercícios").forEach { rule.onNodeWithText(it).assertExists() } }
@Test fun soUmDiaAbertoPorVez() {
    // o dia de hoje abre primeiro: "+ ADICIONAR" aparece uma vez; tocar em outro dia fecha o de hoje (continua uma vez)
}
@Test fun novaSugestaoFicaNoMenuDoCabecalho() {
    rule.onNodeWithContentDescription("Mais opções").performClick()
    rule.onNodeWithText("Nova sugestão").assertIsDisplayed()
}
@Test fun agendaMostraOsDias()  // aba Agenda mostra "Dias e horários"
```

- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar.**
  - Cabeçalho: `back = { HeaderBar(onBack) { MoreButton(…) } }`. A folha tem uma opção: "Nova sugestão" · "Monta outra ficha para você revisar e aceitar." → `generateWorkout()`.
  - `ZenTabs` com Ficha, Agenda e Exercícios, todas em `Ki`. Aba com `rememberSaveable`.
  - Ficha:
    - Continua mostrando `shown = proposed ?: active`, e as edições valem para o plano mostrado.
    - `PlanCard` com sugestão: "SUGESTÃO · REVISE E ACEITE", nome da divisão, "Aceitar", "Outra" e "Descartar e manter a ativa" (este só com ficha ativa).
    - `PlanCard` sem sugestão: "FICHA ATIVA", nome da divisão e `tr("{0} dias · {1} exercícios", …)`, sem botão (a "Nova sugestão" foi para o ⋯).
    - Sem ficha: o cartão atual.
    - Depois, os dias em `AccordionCard`, ordenados como hoje:
      - Título `"SEG · ${tr(day.name)}"`; meta `"${window.format()} · ${tr("{0} exercícios", n)}"`.
      - `var openDayId by rememberSaveable` começa no dia de hoje (ou no primeiro). Tocar no dia aberto fecha; tocar em outro troca.
      - Aberto: as linhas atuais do `DayCard` (toque abre o `ExerciseItemDialog`) e "+ Adicionar".
  - Agenda: `AvailabilitySection(editable, viewModel::saveAvailability)`.
  - Exercícios: `MyExercisesList`.
  - Traduza os textos novos.
- [ ] **Passo 4: Rodar e ver passar** (`WorkoutUiTest`, `NavigationUiTest`, `TranslationsCoverageTest`).
- [ ] **Passo 5: Commit** `feat: Treino com abas Ficha, Agenda e Exercícios`

### Task 13: Dieta com Cardápio · Meta · Alimentos

Desenho: seção 5.6.

**Arquivos:**
- Modificar: `ui/screens/DietScreen.kt`, `ui/screens/FoodsScreen.kt` (vira `FoodsTab`), `ui/nav/AppNav.kt` (a rota `FOODS` sai), `ui/components/ZenIcons.kt` (`Cart`), `NavigationUiTest.kt`, `domain/Translations.kt`
- Criar: `app/src/androidTest/java/com/zenkaigym/ui/DietUiTest.kt`

**Interfaces:**
- Produz:
  - `@Composable fun FoodsTab(viewModel: ZenViewModel)`: o `FoodsScreen` sem `ZenPage`, sem cabeçalho e sem "Voltar à dieta".
  - `DietScreen(viewModel, onBack)` (perde `onOpenFoods`).
  - `ZenIcons.Cart`: traço no grid 24×24 como os outros (alça, cesto e duas rodas).

- [ ] **Passo 1: Escrever os testes.**
  - `DietUiTest` (`DietScreen` direto, com `withDiet()`):
    - `abasDaDieta`: Cardápio, Meta e Alimentos existem.
    - `carrinhoAbreListaDeCompras`: `contentDescription` "Lista de compras" abre a janela "Lista de compras".
    - `novoCardapioFicaNoMenuDoCabecalho`: o ⋯ do cabeçalho mostra "Novo cardápio…".
    - `alimentosVirouAba`: a aba Alimentos mostra "+ NOVO ALIMENTO".
  - No `NavigationUiTest`: troque `dietaAbreMeusAlimentosEVolta` por `dietaReabreNoCardapio`: menu → DIETA → aba Alimentos → voltar (Dojo) → menu → DIETA → a aba Cardápio está selecionada (`assertIsSelected`).
- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar.**
  - Cabeçalho: `HeaderBar(onBack) { carrinho; MoreButton }`.
    - O carrinho é um botão só com ícone (`contentDescription = tr("Lista de compras")`) que abre o `ShoppingListDialog` com `active ?: shown`; some quando não há plano.
    - O ⋯ tem uma opção: "Novo cardápio…" · "Monta um cardápio novo para a semana." → abre o `WeekSetupDialog`.
  - `ZenTabs` com Cardápio, Meta e Alimentos, todas em `Midori`. A aba começa sempre em Cardápio, e o dia em `todayDayOfWeek()`; a Tarefa 2 já tirou o `restoreState`.
  - Cardápio:
    - Continua mostrando `shown = proposed ?: active`.
    - `PlanCard` com "CARDÁPIO DA SEMANA · REVISE E ACEITE" + `WeekTotals` + "Aceitar"/"Outra" + "Manter a ativa" (este só com cardápio ativo). Sem sugestão: "CARDÁPIO DA SEMANA ATIVO" + `WeekTotals`.
    - "Novo cardápio…" e "Lista de compras" saem do cartão.
    - O aviso de cardápio desatualizado continua.
    - Depois: `DayPicker`, `DaySummary` e as refeições do dia em `AccordionCard`:
      - Título = nome da refeição; meta = `"$horário · ${tr("{0} kcal", kcal)}"`.
      - Abre primeiro a próxima refeição de hoje (`MealSchedule.next` com `mealsDone + mealsSkipped` e `clock.minutes`), ou a primeira refeição nos outros dias. Só uma aberta por vez.
      - Aberta: as linhas do `MealCard` (toque abre o `FoodItemDialog`) e "+ Adicionar".
    - Sem plano: o cartão "Sem cardápio ainda" atual.
  - Meta: `TargetCard` (se há perfil) e `ScheduleCard`.
  - Alimentos: `FoodsTab(viewModel)`.
  - `AppNav`: tire `Routes.FOODS` e o `parentTab` de `FOODS`.
- [ ] **Passo 4: Rodar e ver passar** (`DietUiTest`, `NavigationUiTest`, `DojoUiTest`, `TranslationsCoverageTest`).
- [ ] **Passo 5: Conferir à mão** no ZenTest: Treino e Dieta nos três estilos, e uma sugestão pendente editada antes de aceitar. Anote no commit.
- [ ] **Passo 6: Commit** `feat: Dieta com abas Cardápio, Meta e Alimentos; carrinho no cabeçalho`

---

## Fase 5 · Ajustes

### Task 14: Cartão de estilo com prévia

Desenho: seção 5.9 (cartão "Estilo").

**Arquivos:**
- Criar: `ui/screens/StylePreview.kt`
- Modificar: `ui/theme/Theme.kt`, `ui/theme/Color.kt`, `ui/screens/SettingsScreen.kt`, `domain/Translations.kt`

**Interfaces:**
- Produz:
  - `fun paletteFor(style: AppStyle, dark: Boolean): ZenPalette` e `fun lookFor(style: AppStyle): ZenLook` em `Theme.kt`. O `ZenkaigymTheme` passa a usá-las (o `when` atual sai de dentro dele).
  - `fun previewAccent(style: AppStyle, dark: Boolean): Color` em `Color.kt`: o valor de `Aka` no `AccentSet` daquele estilo e modo. Os seis hex de `Aka` viram uma lista privada que `Aka` e `previewAccent` leem.
  - `@Composable fun StyleCard(style: AppStyle, theme: ThemeMode, dark: Boolean, onStyle: (AppStyle) -> Unit, onTheme: (ThemeMode) -> Unit)`

- [ ] **Passo 1: Conferir que o `SettingsUiTest` atual passa** antes de mexer (`escolherTemaEscuro…`, `escolherEstiloKawaii…`).
- [ ] **Passo 2: Implementar.**
  - Três miniaturas lado a lado (Shōnen, Kawaii, Isekai). Cada uma fica dentro de `CompositionLocalProvider(LocalZenPalette provides paletteFor(it, dark), LocalZenLook provides lookFor(it))`, **sem `ZenkaigymTheme`**. Por dentro: `StyleBackdrop`, uma faixa com `previewAccent(it, dark)`, um mini cartão e o nome do estilo.
  - A miniatura escolhida ganha borda `look.border` e um `Check`. Tocar chama `setStyle`.
  - Embaixo, chips na ordem Claro, Escuro, Sistema (`ThemeMode.LIGHT, DARK, SYSTEM`) e as frases de hoje ("Segue o modo claro/escuro do seu celular." / "Fixo em modo {0}.") e a descrição do estilo.
  - `dark` = `theme` resolvido (`SYSTEM` → `isSystemInDarkTheme()`).
  - O cartão substitui "Aparência" e "Estilo" no topo de Ajustes. O resto da tela fica igual até a Tarefa 15.
- [ ] **Passo 3: Rodar e ver passar** (`SettingsUiTest` inteiro, `TranslationsCoverageTest`).
- [ ] **Passo 4: Conferir à mão** que trocar de miniatura não muda as cores das outras telas até tocar nela: abra o menu e veja os acentos. Anote no commit.
- [ ] **Passo 5: Commit** `feat: cartão de estilo com prévia dos três estilos`

### Task 15: Sete linhas e subpáginas de Ajustes

Desenho: seção 5.9 (tabela das sete linhas).

**Arquivos:**
- Criar: `ui/screens/SettingsPages.kt`, `domain/SettingsSummary.kt`, `test/.../domain/SettingsSummaryTest.kt`
- Modificar: `ui/screens/SettingsScreen.kt`, `ui/screens/UpdateCard.kt` (vira `UpdateRow`), `domain/Translations.kt`
- Reescrever: `app/src/androidTest/java/com/zenkaigym/ui/SettingsUiTest.kt`

**Interfaces:**
- Produz:
  - `object SettingsSummary { fun reminders(meals: Boolean, water: Boolean): String; fun languageCode(choice: AppLanguage): String }`
  - `enum class SettingsPage { TRAINING, REMINDERS, WEEK_LANGUAGE, BACKUP, ABOUT, DANGER }`
  - `@Composable fun SettingsRow(title: String, value: String?, onClick: (() -> Unit)?, titleColor: Color = Zen.p.ink, trailing: (@Composable () -> Unit)? = null)`
  - `@Composable fun UpdateRow()`: mesma máquina de estados e mesmos textos do `UpdateCard`. O botão "Verificar" fica na linha e vira "Baixar" quando há versão nova; a frase de estado fica embaixo do título.

- [ ] **Passo 1: Escrever os testes**

```kotlin
// SettingsSummaryTest
@Test fun resumoDosLembretes() {
    assertEquals("Refeições e água", SettingsSummary.reminders(meals = true, water = true))
    assertEquals("Refeições", SettingsSummary.reminders(true, false))
    assertEquals("Só água", SettingsSummary.reminders(false, true))
    assertEquals("Desligados", SettingsSummary.reminders(false, false))
}
@Test fun codigoDoIdioma() {
    assertEquals("PT", SettingsSummary.languageCode(AppLanguage.PT)); assertEquals("EN", SettingsSummary.languageCode(AppLanguage.EN))
    assertEquals("JA", SettingsSummary.languageCode(AppLanguage.JA)); assertEquals("Sistema", SettingsSummary.languageCode(AppLanguage.SYSTEM))
}
```

  No `SettingsUiTest`, mantenha os testes atuais, agora abrindo a linha certa antes:
  - `escolherSemanaComecandoNoDomingo` e os dois de idioma abrem "Semana e idioma".
  - `reverApresentacaoReabreAIntro` abre "Sobre".
  - Tema e estilo continuam no cartão do topo.

  E acrescente:
  - `mostraAsSeteLinhas`: as sete linhas e os grupos "Treino e lembretes" e "Dados e app".
  - `voltarFechaASubpagina`: abre "Backup", `Espresso.pressBack()` e vê "Treino e lembretes" de novo.
  - `atualizacaoTemVerificarNaLinha`: o botão "Verificar" existe sem abrir nenhuma subpágina.
- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar.**
  - Depois do `StyleCard`, dois `ZenCard` com `SectionTitle`:
    - "Treino e lembretes":
      - Modo treino: `listOf(if (keepOn) tr("Tela acesa") else tr("Normal"), if (rest == 0) tr("Auto") else tr("{0} s", rest)).joinToString(" · ")`.
      - Lembretes: `SettingsSummary.reminders`.
      - Semana e idioma: `"${weekStart.label} · ${languageCode}"`.
    - "Dados e app":
      - Backup: "Exportar · Importar".
      - Atualização: `UpdateRow`.
      - Sobre: sem valor.
      - Apagar todos os dados: título em `Aka`.
  - `var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }`, com `BackHandler(enabled = page != null) { page = null }`. Na subpágina: `ZenFixedHeader(<título da linha>, "設", accent = Ao, back = { HeaderBar({ page = null }, backLabel = tr("Ajustes")) })` e o conteúdo atual daquela seção, sem mudar os textos.
  - **Os `rememberLauncherForActivityResult` (exportar, importar, permissão) ficam no topo do `SettingsScreen`**, fora das subpáginas, e descem como funções. Uma subpágina que sai da composição com o seletor de arquivo aberto perderia o resultado.
  - O `UpdateCard` sai; `UpdateRow` entra no lugar.
  - Traduza os textos novos.
- [ ] **Passo 4: Rodar e ver passar** (`SettingsSummaryTest`, `SettingsUiTest`, `NavigationUiTest`, `TranslationsCoverageTest`).
- [ ] **Passo 5: Commit** `feat: Ajustes em sete linhas com subpáginas e verificação de atualização na linha`

---

## Fase 6 · Modo treino

### Task 16: Regras da conclusão e resumo do treino (domínio e ViewModel)

Desenho: seção 5.10 (conclusão, sair no meio, estados depois da conclusão).

**Arquivos:**
- Criar: `domain/WorkoutSummary.kt`, `test/.../domain/WorkoutSummaryTest.kt`
- Modificar: `domain/TrainingFlow.kt`, `test/.../domain/TrainingFlowTest.kt`, `ui/ZenViewModel.kt`

**Interfaces:**
- Consome: `SetCount` (Tarefa 4), `Progress.personalRecords`, `XpRules`.
- Produz:
  - `enum class TrainPrimary { LOG_NEXT, FINISH, SUMMARY }`
  - `TrainingFlow.primary(done: Boolean, count: SetCount): TrainPrimary`. `done` → `SUMMARY`; `count.complete` → `FINISH`; senão `LOG_NEXT`.
  - `TrainingFlow.shouldAutoComplete(loggedLastPending: Boolean, done: Boolean): Boolean` = `loggedLastPending && !done`. Aquecimentos nunca são "a última pendente".
  - `TrainingFlow.askOnExit(count: SetCount, done: Boolean): Boolean` = `count.done > 0 && !done`. Decisão minha: só séries de trabalho contam, então quem só aqueceu sai direto.
  - `data class DaySummary(val sets: Int, val volumeKg: Double, val records: List<RecordEntry>, val xp: Int)`
  - `object WorkoutSummary { fun of(sets: List<SetLog>, today: Long, names: Map<Long, String>): DaySummary }`:
    - Considera só as séries de trabalho de `today`.
    - `volumeKg` = Σ carga × reps.
    - `records` = `Progress.personalRecords(sets sem aquecimento, names)` com `epochDay == today`.
    - `xp` = `SESSION + sets × SET + records × RECORD`.
  - ViewModel:
    - `val todaySummary: StateFlow<DaySummary?>`: `repo.allSets` + `exercises` + `today`, em `Dispatchers.Default`, `WhileSubscribed(5_000)`.
    - `fun completeWorkout()`: `repo.setWorkoutDone(hoje, true)` e `stopRest()`, **sem** `stopTraining()` e sem aviso. O `setWorkoutDone(true)` de hoje continua para o "Marcar como feito" do Dojo.

- [ ] **Passo 1: Escrever os testes**

```kotlin
// TrainingFlowTest
@Test fun conclusaoAutomaticaSoNaTransicao() {
    assertTrue(TrainingFlow.shouldAutoComplete(loggedLastPending = true, done = false))
    assertFalse(TrainingFlow.shouldAutoComplete(loggedLastPending = true, done = true))   // depois de "Marcar como feito" ou "Concluir assim"
    assertFalse(TrainingFlow.shouldAutoComplete(loggedLastPending = false, done = false)) // ainda faltam séries, ou foi um aquecimento
}
@Test fun botaoPrincipalDoRodape() {
    assertEquals(TrainPrimary.LOG_NEXT, TrainingFlow.primary(done = false, SetCount(3, 5)))
    assertEquals(TrainPrimary.FINISH, TrainingFlow.primary(false, SetCount(5, 5)))   // desfez a conclusão com tudo registrado
    assertEquals(TrainPrimary.FINISH, TrainingFlow.primary(false, SetCount(0, 0)))
    assertEquals(TrainPrimary.SUMMARY, TrainingFlow.primary(true, SetCount(2, 5)))   // aberto pelo "Treino feito"
}
@Test fun perguntaAoSairSoComSeriesENaoConcluido() {
    assertFalse(TrainingFlow.askOnExit(SetCount(0, 5), done = false))
    assertTrue(TrainingFlow.askOnExit(SetCount(1, 5), done = false))
    assertFalse(TrainingFlow.askOnExit(SetCount(3, 5), done = true))
}

// WorkoutSummaryTest
private fun set(day: Long, ex: Long, i: Int, reps: Int, kg: Double, warmup: Boolean = false) =
    SetLog(epochDay = day, exerciseId = ex, setIndex = i, reps = reps, weightKg = kg, warmup = warmup)
@Test fun resumoDoDiaComRecorde() {
    val sets = listOf(set(10, 1, 1, 10, 40.0), set(12, 1, 1, 10, 40.0), set(12, 1, 2, 8, 42.5),
        set(12, 1, -1, 10, 60.0, warmup = true), set(12, 2, 1, 12, 30.0))
    val s = WorkoutSummary.of(sets, today = 12, names = mapOf(1L to "Supino", 2L to "Remada"))
    assertEquals(3, s.sets)
    assertEquals(400.0 + 340.0 + 360.0, s.volumeKg, 0.001)
    assertEquals(listOf(RecordEntry(1, "Supino", 42.5, 40.0, 12)), s.records) // aquecimento de 60 kg não conta; Remada é a 1ª vez
    assertEquals(XpRules.SESSION + 3 * XpRules.SET + XpRules.RECORD, s.xp)
}
@Test fun diaSemSeriesValeSoASessao() {
    val s = WorkoutSummary.of(emptyList(), 12, emptyMap())
    assertEquals(0, s.sets); assertEquals(XpRules.SESSION, s.xp)
}
```

- [ ] **Passo 2: Rodar e ver falhar.**
- [ ] **Passo 3: Implementar** as funções e os dois itens do ViewModel.
- [ ] **Passo 4: Rodar e ver passar** (`TrainingFlowTest`, `WorkoutSummaryTest`, unitários).
- [ ] **Passo 5: Commit** `feat: regras da conclusão do treino e resumo do dia`

### Task 17: Cabeçalho em faixa e quadro do exercício com ⋯

Desenho: seção 5.10 (cabeçalho e quadro do exercício).

**Arquivos:**
- Modificar: `ui/screens/TrainSession.kt`, `domain/Translations.kt`

**Interfaces:**
- Consome: `setCount()` (Tarefa 4); `MoreButton` (Tarefa 2); `ZenSheet`, `SheetOption` (Tarefa 6).

- [ ] **Passo 1: Implementar o cabeçalho.**
  - `TrainHeader(dayName, count: SetCount, done: Boolean, onClose)` numa faixa só, com fundo `p.card`, faixa `Aka` em cima e linha `p.border` embaixo.
  - Da esquerda para a direita:
    - o X (o botão de 48 dp de hoje, agora à esquerda);
    - "MODO TREINO" pequeno e o nome do dia;
    - `KanjiBadge(Progression.mood(done, total).first, size = 28.dp)`.
  - Embaixo: `ZenProgressBar(done/total)` e `tr("{0} / {1} séries", …)`, ou `tr("Concluído")` quando `done`.
  - O `PowerGauge` sai (sem frase de ânimo nem porcentagem grande).
  - `doneSets`/`totalSets` passam a vir de `setCount()`.
- [ ] **Passo 2: Implementar o quadro.**
  - Linha do topo:
    - miniatura de 56 dp do `ExerciseImage`; o toque abre um `ZenDialog` com a imagem grande;
    - o nome e `tr("{0} séries × {1} · {2}", sets, reps, group.label)` (cardio: só `reps`, como hoje);
    - o `HankoStamp` quando completo;
    - `MoreButton`.
  - A imagem grande e a fileira "Trocar / + Aquecimento" saem do corpo do quadro.
  - Folha do ⋯:
    - `"Trocar (${group.label})"`: com série registrada, fica desativado e a descrição é a frase de hoje, "Para trocar, desfaça as séries deste exercício."; senão, "Vale só para o treino de hoje; a ficha continua igual.". Ação: `onSwap`.
    - "+ Aquecimento" · "Adiciona uma série leve antes das de trabalho (até 3)." → `onAddWarmup`. Só aparece se `!timed && warmups.size < 3`.
  - Dica de carga: aparece só se `doneCount == 0` para aquele exercício. Vira um balão de fala: o `SpeechTag` atual com uma pontinha (triângulo pequeno) apontando para o nome, nas cores e bordas de `Zen.look`.
- [ ] **Passo 3: Compilar e rodar os unitários.**
- [ ] **Passo 4: Conferir à mão** no ZenTest (dia de treino nos dados): trocar com e sem série registrada, aquecimento até 3, a dica sumindo depois da 1ª série, e a imagem ampliando. Anote no commit.
- [ ] **Passo 5: Commit** `feat: modo treino com cabeçalho em faixa e ⋯ no exercício`

### Task 18: Rodapé único, descanso e pergunta ao sair

Desenho: seção 5.10 (rodapé e sair no meio pelo X).

**Arquivos:**
- Criar: `ui/screens/TrainFooter.kt` (o `TrainFooter` e o `ToolButton` saem do `TrainSession.kt`)
- Modificar: `ui/screens/TrainSession.kt`, `domain/Translations.kt`

**Interfaces:**
- Consome: `TrainingFlow.primary()`, `askOnExit()` (Tarefa 16); `RestTimer` via ViewModel (`rest`, `startRest`, `addRest`, `stopRest`).
- Produz: `@Composable fun TrainFooter(viewModel: ZenViewModel, primary: TrainPrimary, next: NextSet?, restBlocked: Boolean, onLogNext: () -> Unit, onFinish: () -> Unit, onSummary: () -> Unit)`, com `data class NextSet(val setIndex: Int, val exerciseName: String, val detail: String)`.
  - `detail`: "40 kg × 12"; sem carga, "× 12"; prancha, `tr("{0} s", seg)`; cardio, `tr("{0} min", min)`.

- [ ] **Passo 1: Implementar o rodapé.**
  - Sem descanso: à esquerda, o botão pequeno de cronômetro (`ZenIcons.Timer`, `contentDescription = tr("Descanso manual")`), desativado com `restBlocked` (treino concluído ou todas as séries feitas).
  - O principal, por `primary`:
    - `LOG_NEXT`: `ToolButton` vermelho com `"▶ " + tr("Registrar série {0}", next.setIndex)` e, embaixo, `"${next.exerciseName} · ${next.detail}"`. Faz o mesmo que o `onNext` de hoje.
    - `FINISH`: "Concluir treino" (`Midori`).
    - `SUMMARY`: "Ver resumo" (`Midori`).
  - Com descanso: o rodapé inteiro vira o cronômetro:
    - `tr("Descanso · depois: série {0}", next.setIndex)` (sem `next`, só `tr("Descanso")`);
    - o tempo grande (`m:ss`);
    - `ZenProgressBar`;
    - `tr("+{0} s", RestTimer.ADD_SECONDS)` → `addRest()` e "Pular" → `stopRest()`.
  - "Sair" e "Concluir treino" saem do rodapé.
- [ ] **Passo 2: Implementar a saída.**
  - O X e o `BackHandler` chamam `requestExit()`. Se `TrainingFlow.askOnExit(count, done)`, abre um `ZenDialog`:
    - título "Sair do treino?";
    - texto `tr("Você registrou {0} de {1} séries.", …)`;
    - "Concluir assim" → `completeWorkout()` e mostra a conclusão (Tarefa 19);
    - "Continuar depois" → `onClose()`.
  - Senão, `onClose()` direto.
- [ ] **Passo 3: Compilar, rodar unitários e `TranslationsCoverageTest`.**
- [ ] **Passo 4: Conferir à mão** no ZenTest: registrar série, descanso com +15 s e Pular, notificação do descanso igual a hoje, e sair com e sem séries. Anote no commit.
- [ ] **Passo 5: Commit** `feat: rodapé do modo treino com botão único e descanso; pergunta ao sair`

### Task 19: Conclusão automática e tela de conclusão

Desenho: seção 5.10 (conclusão e estados depois da conclusão).

**Arquivos:**
- Criar: `ui/screens/TrainConclusion.kt`
- Modificar: `ui/screens/TrainSession.kt` (`HankoStamp` ganha `size: Dp = 40.dp`), `domain/Translations.kt`

**Interfaces:**
- Consome: `shouldAutoComplete()`, `primary()` (Tarefa 16); `todaySummary`, `completeWorkout()`, `setWorkoutDone(false)`, `progressStats` (`level`).
- Produz: `@Composable fun TrainConclusion(dayName: String, summary: DaySummary?, level: LevelInfo?, onBackToDojo: () -> Unit, onUndo: () -> Unit)`

- [ ] **Passo 1: Ligar a conclusão.**
  - `var showConclusion by rememberSaveable { mutableStateOf(false) }`.
  - Em `logDraft`, depois de registrar: `if (TrainingFlow.shouldAutoComplete(isLast, done)) { viewModel.completeWorkout(); showConclusion = true }`. O `isLast` de hoje já ignora aquecimentos.
  - `FINISH` e "Concluir assim" fazem o mesmo. `SUMMARY` só faz `showConclusion = true`.
- [ ] **Passo 2: Implementar a tela** (por cima do conteúdo do modo treino, com `ZenPage`):
  - `HankoStamp(size = 96.dp)` com 完.
  - "Treino concluído" e o nome do dia.
  - Três `StatTile`: "Séries" (`summary.sets`), "Volume" (kg, mesmo formato da Evolução) e "Recordes" (`summary.records.size`).
  - `tr("+{0} XP", summary.xp)`, `ZenProgressBar(level.fraction, Daidai)` e `tr("{0} / {1} XP para o nível {2}", …)`.
  - Com recordes: `SectionTitle(tr("Recordes de hoje"))` e uma linha por recorde: `"${tr(name)} · ${antes} → ${agora} kg"`.
  - "Voltar ao Dojo" → `onClose()`, que fecha o modo treino; a `LevelUpScene` aparece depois, como já acontece fora do modo treino.
  - "Desfazer conclusão" → `setWorkoutDone(false)` e `showConclusion = false`.
  - Voltar do sistema na conclusão = "Voltar ao Dojo" (decisão minha).
  - Sem duração do treino (seção 11 do desenho).
- [ ] **Passo 3: Compilar, rodar unitários e `TranslationsCoverageTest`.**
- [ ] **Passo 4: Conferir à mão** no ZenTest:
  - (a) registrar a última série abre a conclusão uma vez;
  - (b) "Desfazer conclusão" com tudo registrado mostra "Concluir treino" e registrar/desfazer não abre a conclusão sozinho;
  - (c) "Treino feito" no Dojo abre com "Concluído" e "Ver resumo";
  - (d) "Marcar como feito" no Dojo e depois registrar as séries que faltavam não abre a conclusão;
  - (e) subir de nível mostra a comemoração depois de "Voltar ao Dojo";
  - (f) girar a tela na conclusão mantém a conclusão.
  Anote no commit.
- [ ] **Passo 5: Commit** `feat: conclusão automática e tela de conclusão do treino`

### Task 20: Verificação final

- [ ] **Passo 1:** `./gradlew :app:testDebugUnitTest`. Esperado: todos PASS.
- [ ] **Passo 2:** `ANDROID_SERIAL=<ZenTest> ./gradlew :app:connectedDebugAndroidTest`. Esperado: todos PASS.
- [ ] **Passo 3:** `./gradlew :app:assembleRelease -PversionCode=10003 -PversionName=1.1.0-dev` e instalar com `adb install -r` no `Medium_Phone_API_36` (é o build de release, por cima dos dados de teste do usuário). Conferir que os dados continuam lá e que o R8 não quebrou nada: menu, uma folha do ⋯, a conclusão do treino e a linha "Atualização" → Verificar.
- [ ] **Passo 4:** Percorrer a tabela da seção 6 do desenho, linha por linha, e confirmar que cada função está no lugar novo.
- [ ] **Passo 5:** Commit de qualquer ajuste, e relatório com o que foi conferido à mão e o que não deu para conferir.
