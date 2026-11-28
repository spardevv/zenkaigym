# Navegação "Só o Dojo" (proposta B): desenho

- **Data:** 2026-11-27
- **Estado:** aprovado em 2026-11-27 · plano em `docs/superpowers/plans/2026-11-27-dojo-navigation.md`
- **Referência visual:** página "Mapa do Dojo" (https://claude.ai/artifact/F6sMmeRrXrz1oMGkTBxhWD), com todas as telas nos estilos Shōnen, Kawaii e Isekai, claro e escuro. As maquetes usam dados de exemplo.

## 1. Intenção

O app cresceu (água, widget, desafios, evolução com 5 abas, histórico, atualização, CSV) e a navegação continuou a mesma: 5 abas fixas. O resultado é poluído. Ajustes ocupa uma aba que quase não se usa, a Evolução (a tela mais rica) fica escondida dentro do Perfil, e o Dojo tem botões demais com o mesmo peso.

O objetivo é deixar o app mais calmo **sem perder nenhuma função** e **sem perder a estética de anime**. O Dojo continua sendo a tela principal: é onde o app abre e para onde tudo volta.

## 2. Objetivos, não objetivos e antiobjetivos

**Objetivos**
- Tirar a barra de abas. O Dojo é a tela fixa, e o resto fica a um toque, num menu.
- Cada tela com uma hierarquia clara: um botão principal por cartão, e as ações secundárias atrás de um ⋯.
- Toda função existente ganha um lugar definido (seção 6).
- Tudo segue o estilo escolhido (Shōnen, Kawaii, Isekai; claro e escuro) usando as peças que já existem.

**Não objetivos**
- Mudar regras de domínio (planejadores, nutrição, progressão, XP).
- Mudar o banco de dados (sem migração Room).
- Mudar o widget, as notificações ou o backup.
- Criar estilos ou paletas novas.

**Antiobjetivos** (seria um fracasso mesmo funcionando)
- O Dojo virar um painel de métricas cheio de números.
- Uma tela com layout próprio por estilo. O estilo muda a "roupa", nunca a estrutura.
- Alguma ação diária ficar mais longe do que hoje: "Feito", "Treinar agora" e "+250 ml" continuam a um toque no Dojo.
- Alguma função sumir sem lugar novo.

## 3. Restrições

- Kotlin, Jetpack Compose (Material 3), Navigation Compose, Room. `minSdk 24`.
- Funciona offline. A única chamada de rede continua sendo "Verificar atualização", e só ao toque.
- Textos passam por `tr()`. Toda frase nova em português precisa de tradução em inglês e japonês em `Translations.kt`: o `TranslationsTest` exige as mesmas chaves, nenhuma tradução vazia e os marcadores `{0}` preservados.
- Respeitar `LocalMotionEnabled` (animações desligadas quando o sistema pede).
- Telas largas (tablet, celular deitado) usam `WindowLayout` (`useRail`, `twoColumns`, `contentMaxWidth`).
- Estética: `ZenCard`, `ZenButton`, `ZenChip`, `KanjiBadge`, `SectionTitle`, `ZenFixedHeader`, `ZenPage` (fundo com linhas de velocidade, brilhos ou grade holográfica) e `ZenLook` (forma, borda, sombra) já mudam com o estilo. Peças novas são montadas com elas ou com os mesmos tokens (`Zen.p`, `Zen.look`).

## 4. Abordagens consideradas

1. **A · Três abas (Plano · Dojo · Evolução).** Treino e Dieta numa aba com seletor; Perfil e Ajustes numa folha "Você" aberta pelo selo de nível. Mantém uma barra.
2. **B · Só o Dojo (escolhida).** Sem barra; o Dojo abre um menu em grade com o resto. É a mais calma, e as telas que saem da barra (Treino, Dieta, Perfil, Ajustes) são de configuração, visitadas poucas vezes por semana.
3. **C · Hoje e semana (Hoje · Semana · Evolução · Você).** Organiza por tempo; exigia criar do zero uma tela "Semana" que junta treino e dieta.

O usuário escolheu a **B**, com duas condições: manter a estética de anime e se ajustar aos temas.

Dentro da B, houve duas rodadas:
- **Dojo como "linha do dia"** (refeições e treino numa linha do tempo, com um botão flutuante "+ Registrar"): rejeitado. O usuário achou desconexo e pediu algo mais parecido com o Dojo original.
- **Dojo com os cartões originais, melhorados** (seção 5.2): escolhido. A folha "Registrar" saiu junto com o botão flutuante; as ações dela foram para os cartões e para a Evolução.

## 5. O que foi decidido

### 5.1 Navegação

- `AppNav` perde `BottomBar` e `SideRail`. O `NavHost` continua com `HOME` como início.
- Nova rota `MENU`. Do Dojo, o selo 道 do cabeçalho (com o rótulo "Menu" embaixo) abre o menu.
- Do menu, cada quadro abre a sua tela (Treino, Dieta, Evolução, Perfil, Ajustes). Voltar de qualquer uma delas leva ao **Dojo**, não ao menu: ao navegar a partir do menu, a pilha volta até `HOME` antes de empilhar a tela escolhida.
- Atalhos diretos do Dojo: a faixa da semana abre a Evolução; "Treinar agora" abre o modo treino; "Ver no cardápio" (no ⋯ da refeição) abre a Dieta.
- `HISTORY` e `FOODS` deixam de ser rotas próprias (viram abas; seções 5.5 e 5.6). `PROGRESS` deixa de ser subtela do Perfil e vira destino do menu.
- As telas abertas do menu ganham, no cabeçalho, um botão "← Dojo" no lugar dos atuais "Voltar ao perfil" e "Voltar à dieta".
- **Telas largas** (`useRail`, inclusive celular deitado):
  - A coluna à esquerda continua com a largura do `SideRail` de hoje (72–88 dp) e só mostra kanji e rótulo, sem os dados ao vivo dos quadros do menu.
  - Ela tem seis entradas: Dojo, Treino, Dieta, Evolução, Perfil e Ajustes, com a tela atual destacada como hoje.
  - Nesse modo, o selo 道 do Dojo fica escondido.

### 5.2 Dojo

Mesma estrutura de hoje, de cima para baixo: cabeçalho, faixa de água, cartão "Treino de hoje", cartão da refeição e, no fim, a faixa da semana. Em `twoColumns`, os dois cartões continuam lado a lado.

- **Cabeçalho:** "DOJO", "Hoje · Sex" e, à direita, o selo 道 com o rótulo "Menu".
- **Água:** igual a hoje (kanji 水, "ÁGUA · 1,2 / 2,5 L", barra, "−" só quando há água registrada, "+250").
- **Treino de hoje** (`ZenCard`, acento Aka):
  - Título "Treino de hoje" e, à direita, o horário e a duração da agenda.
  - Nome do treino; "14 exercícios · 33 séries"; "Remarcado de …" quando for o caso.
  - No lugar dos três primeiros nomes de exercício: uma fileira de **kanji dos grupos musculares** do treino (胸 背 脚 肩 腕 腹 走), sem repetir e na ordem da ficha. São os mesmos kanji e a mesma função `MuscleGroup.kanji()` do modo treino, que passa a ser compartilhada.
  - Botões: **"Treinar agora"** (principal) e **⋯** (opções na seção 5.3).
  - Estados que já existem e continuam:
    - Sem ficha: texto e "Ver treino".
    - Dia de descanso: 休 e a frase de descanso.
    - Feito: o principal vira **"Treino feito ✓"** (Midori). Tocar reabre o modo treino para revisar ou corrigir séries, como hoje.
    - Não realizado: "Não realizado" com "Desfazer".
    - "Não cabe nesta semana: …".
  - Estado novo **"Em andamento"**: há séries registradas hoje e o treino não foi concluído. Mostra "Em andamento · 9/33" com barra, e o botão principal vira "Continuar".
- **Refeição** (`ZenCard`, acento Aka):
  - O título segue o estado, como hoje ("Próxima refeição", "Refeição realizada", "Futuras refeições"…), e a linha "Hoje ±X kcal · semana ±Y kcal" continua.
  - O pager horizontal continua igual: abre na próxima refeição, e deslizar mostra as outras.
  - Cada página: etiqueta (PRÓXIMA, FEITA, NÃO REALIZADA), nome, horário grande à direita, kcal ajustadas, alimentos, pratos extras.
  - Botões: **"Feito ✓"** (principal, Midori) e **⋯**. Depois de marcada, o principal vira "Desfazer", como hoje.
  - Fileira de horários "HOJE x/y": refeição feita = chip preenchido Midori; refeição da vez = borda grossa (`look.border`); não realizada = traço/×. Tocar leva até a página, como hoje.
- **Faixa da semana** (nova, sem cartão, fica no espaço livre do fim da tela):
  - Rótulo "Semana · 1/3 treinos" e, à direita, "Nv 3 · 130 / 300 XP".
  - Sete quadradinhos na ordem de `WeekStart`: dia com treino feito = Ki; hoje = borda grossa; dia de treino planejado que passou sem treino = borda tracejada.
  - Tocar abre a Evolução.
  - Usa dados que o `ZenViewModel` já expõe (`weekWorkoutLogs`, `availability`, `weekStart`, `progressStats`).
- A rolagem do Dojo continua só como rede de segurança (letra grande, tela baixa).

### 5.3 Folha do ⋯ (nova)

Uma folha que sobe por cima do Dojo, com fundo escurecido, faixa de acento e alça. Os cantos seguem `Zen.look` (retos no Shōnen, redondos no Kawaii, chanfrados no Isekai). Cada opção é uma linha com uma frase dizendo o que acontece.

- **⋯ da refeição:** o cabeçalho mostra o nome, o horário e as kcal da refeição. As opções:
  - "Comi outra coisa", que leva ao fluxo atual do `AddExtraDialog` com "Da minha lista" e "Prato livre (kcal)".
  - "Não realizei".
  - "Ver no cardápio", que abre a Dieta na aba Cardápio, no dia de hoje.
- **⋯ do treino**, conforme o estado:
  - Pendente ou em andamento:
    - "Marcar como feito", que marca o treino sem abrir o modo treino e sem exigir séries registradas. Substitui o "Concluir treino" do rodapé de hoje, que permitia isso.
    - "Não realizado".
    - "Trocar o treino de hoje", que leva ao `ChangeWorkoutDialog` atual.
  - Feito: "Desfazer conclusão".

### 5.4 Menu (門)

Tela cheia com o fundo do estilo (`ZenPage`). Cabeçalho "MENU", "門 · Para onde agora?" e um botão de fechar (volta ao Dojo). O corpo é uma "página de mangá", com quadros de tamanhos diferentes conforme o uso, em três grupos com `SectionTitle`:

- **Plano:** Treino (体, Ki) e Dieta (食, Midori), em quadros altos.
- **Progresso:** Evolução (進, Daidai), em quadro largo com a barra de XP.
- **Você:** Perfil (己, Sakura) e Ajustes (設, Ao), em quadros pequenos.

Cada quadro mostra um dado atual:
- Treino: nome da divisão e dias de treino.
- Dieta: meta de kcal e número de refeições.
- Evolução: nível e XP.
- Perfil: peso e objetivo.
- Ajustes: estilo e modo.

Os quadros são `ZenCard` com faixa de acento. Nos quadros grandes, o fundo decorativo do estilo também aparece dentro do quadro.

### 5.5 Treino (体)

Cabeçalho com "← Dojo", título e kanji. Um ⋯ no cabeçalho guarda "Nova sugestão". Abaixo, abas internas **Ficha · Agenda · Exercícios**:

- **Ficha:**
  - Mostra o mesmo plano de hoje: a sugestão pendente, se houver, ou a ficha ativa (`shown = proposed ?: active`). As edições valem para o plano mostrado, então a pessoa revisa e ajusta a sugestão antes de aceitar, como hoje.
  - No topo, o cartão do plano mostrado:
    - Com sugestão: "Sugestão · revise e aceite", com "Aceitar", "Outra" e "Descartar e manter a ativa" (este só quando há ficha ativa).
    - Sem sugestão: "Ficha ativa", com o nome da divisão e o resumo.
  - Depois, os dias como **sanfona**: uma linha por dia ("SEG · Corpo inteiro A", horário, nº de exercícios). Só um dia aberto por vez, e o de hoje abre primeiro. Aberto, mostra os exercícios (toque abre o `ExerciseItemDialog` atual) e "+ Adicionar".
- **Agenda:** o `AvailabilitySection` atual.
- **Exercícios:** o conteúdo do `MyExercisesDialog` atual, como tela.

### 5.6 Dieta (食)

Cabeçalho com "← Dojo", título e kanji. Um ícone de carrinho abre a lista de compras (`ShoppingListDialog`); um ⋯ guarda "Novo cardápio…". Abas internas **Cardápio · Meta · Alimentos**:

- **Cardápio:**
  - Mostra o mesmo plano de hoje: a sugestão pendente, se houver, ou o cardápio ativo. As edições valem para o plano mostrado.
  - Com sugestão, o cartão do plano diz "Cardápio da semana · revise e aceite", com "Aceitar", "Outra" e "Manter a ativa" (este só quando há cardápio ativo).
  - Aviso de cardápio desatualizado quando o número de refeições mudou.
  - Seletor de dias.
  - Cartão de resumo do dia: kcal do dia contra a meta e macros P/C/G.
  - Refeições em sanfona: a próxima abre primeiro, e aberta mostra os alimentos (toque abre o `FoodItemDialog`) e "+ Adicionar".
- **Meta:** `TargetCard` e `ScheduleCard` atuais (meta, refeições por dia, horários).
- **Alimentos:** o conteúdo da `FoodsScreen` atual, sem o "Voltar à dieta".

### 5.7 Evolução (進)

Destino próprio do menu e da faixa da semana. Cabeçalho com "← Dojo".

- As abas atuais (Resumo, Treino, Dieta, Peso, Conquistas) ganham **rótulo escrito**; hoje são só ícones. A fileira de abas rola na horizontal se não couber.
- **Aba Treino** recebe o **Histórico**: a lista de exercícios registrados, e o detalhe de cada um (1RM estimado, últimas sessões) abre dentro da aba. Voltar fecha o detalhe antes de sair da tela, como o `HistoryScreen` faz hoje.
- **Aba Peso** recebe o **registro de peso** que hoje fica no Perfil: campo "Novo peso (kg)" com "Registrar", e a lista de registros com editar e remover. O texto "Registre seu peso no Perfil…" muda para apontar para a própria aba.
- O `LevelCard` existe só aqui (sai do Perfil). No Dojo fica só o resumo da faixa da semana.

### 5.8 Perfil (己)

Só os dados da pessoa: o cartão "Corpo" (altura, peso, idade, sexo, objetivo), "Preferências de treino" (`TrainingPrefs`), e "Salvar"/"Desfazer" só quando algo muda, como hoje.

- Saem o `LevelCard`, os botões "Evolução" e "Histórico" e o `WeightCard` (gráfico, campo "Novo peso" e lista de registros).
- O campo "Peso" do cartão "Corpo" continua editável. Como hoje, salvar o perfil com outro peso cria um registro de peso (`ZenRepository.saveProfile`).
- O acento passa de Daidai para **Sakura**, para não repetir o laranja da Evolução.

### 5.9 Ajustes (設)

Cabeçalho com "← Dojo". No topo, um cartão "Estilo" com prévia: três miniaturas (Shōnen, Kawaii, Isekai) e chips Claro/Escuro/Sistema. Abaixo, **sete linhas** em dois grupos:

| Linha | Valor à direita | Conteúdo (o mesmo de hoje) |
|---|---|---|
| Modo treino | "Tela acesa · 90 s" | Tela sempre acesa; descanso entre séries (Auto, 60, 90, 120, 180) |
| Lembretes | "Refeições e água" | Lembretes de refeição e treino; lembretes de água |
| Semana e idioma | "Segunda · PT" | Início da semana; idioma |
| Backup | "Exportar · Importar" | Explicação do .zip com CSV, "Exportar dados" e "Importar dados" |
| Atualização | botão **Verificar** na própria linha | Estados do `UpdateCard`: versão atual, "Verificando…", erro, "última versão" e, quando há versão nova, o botão vira **Baixar** |
| Sobre | — | Aviso de saúde, privacidade, créditos das imagens, versão, "Rever apresentação" |
| Apagar todos os dados | — | A zona de perigo atual e a confirmação |

- Grupos: "Treino e lembretes" (as três primeiras linhas) e "Dados e app" (as quatro últimas).
- Cada linha, menos Atualização, **abre uma tela curta** com o conteúdo atual daquela seção. O jeito de montar fica com quem constrói (seção 9).

### 5.10 Modo treino (体)

Continua em tela cheia, por cima de tudo, aberto por "Treinar agora". Um exercício por página, deslizando para os lados, como hoje. Muda o seguinte:

- **Cabeçalho em uma faixa só:**
  - Fechar (X), "Modo treino" e o nome do dia.
  - O kanji do ânimo de `Progression.mood` (準 気 根 完), pequeno.
  - Uma barra com "9 / 33 séries".
  - `TrainHeader` e `PowerGauge` se fundem; a frase de ânimo e a porcentagem grande saem.
- **Fileira de exercícios:** igual a hoje (`ExerciseTabs`).
- **Quadro do exercício:**
  - Miniatura da imagem ao lado do nome (toque amplia num diálogo), "3 séries × 12 · costas" e um ⋯.
  - No ⋯: "Trocar (grupo)", com a mesma trava de hoje quando já há série registrada, e "+ Aquecimento", com o limite de três.
  - A dica de carga (`LoadAdvice`: "Suba!", "Mais reps!", "Deload") vira um **balão de fala** e só aparece até a primeira série de trabalho daquele exercício ser registrada.
  - Tabela de séries como hoje; tocar no número da série continua abrindo RIR e nota.
- **Rodapé:**
  - Sem descanso: um botão pequeno de cronômetro (descanso manual) e o **botão principal "▶ Registrar série N"**, com a carga e as repetições embaixo. Ele faz o mesmo que o "Próxima série" de hoje.
  - Durante o descanso, o rodapé vira o cronômetro: "Descanso · depois: série N", o tempo grande, uma barra, "+15 s" e "Pular". `RestTimer`, `RestService` e a notificação não mudam.
  - "Sair" e "Concluir treino" saem do rodapé.
- **Conclusão:**
  - Ao registrar a última série pendente, o treino é marcado como feito (`setWorkoutDone(true)`) e abre a **tela de conclusão**, ainda dentro do modo treino.
  - A tela mostra o carimbo 完 (mesmo desenho do `HankoStamp`), "Treino concluído" e o nome do dia.
  - Três números: séries, volume em kg e recordes do dia.
  - "+N XP", com a barra até o próximo nível. N = `XpRules.SESSION` + séries × `SET` + recordes × `RECORD`, calculado com as regras atuais.
  - A lista "Recordes de hoje" (antes → agora).
  - Botões "Voltar ao Dojo" e "Desfazer conclusão".
  - Se a pessoa subir de nível, a `LevelUpScene` aparece depois de fechar, como já acontece fora do modo treino.
- **Sair no meio pelo X:**
  - Sem nenhuma série registrada hoje: fecha direto. Para marcar como feito sem séries, existe o "Marcar como feito" no ⋯ do Dojo (seção 5.3).
  - Com séries registradas e o treino não concluído: pergunta "Concluir assim" (marca como feito e mostra a conclusão) ou "Continuar depois" (fecha; o Dojo mostra "Em andamento").
  - O voltar do sistema se comporta igual ao X.
- **Estados depois da conclusão:**
  - A conclusão automática só dispara na transição: o treino **não** estava concluído e a última série pendente acabou de ser registrada.
  - Modo treino aberto com o treino já concluído (pelo "Treino feito ✓" do Dojo):
    - O cabeçalho mostra "Concluído", e as séries continuam editáveis.
    - O botão principal do rodapé vira "Ver resumo", que abre a tela de conclusão.
    - Registrar ou desfazer séries não abre a conclusão de novo.
  - "Desfazer conclusão" (na tela de conclusão ou no ⋯ do Dojo) marca o treino como não feito:
    - Pela tela de conclusão, volta ao modo treino.
    - Se ainda há séries pendentes, o rodapé volta a "Registrar série N".
    - Se todas as séries estão registradas, o principal vira **"Concluir treino"** (manual) e a conclusão automática não dispara de novo.
  - Treino concluído com "Concluir assim" ou "Marcar como feito": registrar depois as séries que faltavam não abre a conclusão de novo.

### 5.11 Estilos

Nenhuma tela tem layout próprio por estilo. O que muda vem de `ZenLook` e `ZenPalette`:

| | Shōnen | Kawaii | Isekai |
|---|---|---|---|
| Quadros | Borda grossa, cantos retos, sombra sólida | Cantos redondos, borda fina, sombra curta | Cantos chanfrados, linha neon fina |
| Fundo | Linhas de velocidade | Brilhos | Grade holográfica |
| Títulos | Itálico pesado | Sem itálico | Monoespaçados |
| Menu | Página de mangá | Adesivos | Janela de status de RPG |

Peças novas (faixa da semana, folha do ⋯, abas internas, sanfona, rodapé de descanso, tela de conclusão, balão de fala) usam `Zen.look.shape`, `smallShape`, `border`, `thinBorder` e `shadow`, e as cores de `Zen.p` e dos acentos. O marcador de seção (barra, círculo ou losango) vem de `SectionTitle`.

## 6. Onde cada função foi parar

| Hoje | Na proposta B |
|---|---|
| Aba Dojo | Tela inicial, sem barra; o 道 abre o menu |
| Aba Treino | Menu → Treino (Ficha · Agenda · Exercícios) |
| "Meus exercícios" (diálogo) | Treino → aba Exercícios |
| Aba Dieta | Menu → Dieta (Cardápio · Meta · Alimentos) |
| Meus alimentos (subtela) | Dieta → aba Alimentos |
| Lista de compras (botão no cardápio) | Ícone de carrinho no cabeçalho da Dieta |
| Aba Perfil | Menu → Perfil (só dados) |
| Evolução (subtela do Perfil) | Menu → Evolução, ou a faixa da semana no Dojo |
| Histórico (subtela do Perfil) | Evolução → aba Treino |
| Registro de peso (Perfil) | Evolução → aba Peso |
| Aba Ajustes | Menu → Ajustes (sete linhas) |
| Cartão "Atualização" | Linha "Atualização" com Verificar/Baixar |
| "Rever apresentação" | Ajustes → Sobre |
| Cartão de água | Faixa no topo do Dojo, como hoje |
| "Não realizado" / "Trocar treino de hoje" (botões no cartão) | ⋯ do cartão do treino |
| "Concluir treino" sem séries (rodapé do modo treino) | "Marcar como feito" no ⋯ do cartão do treino |
| "Treino feito · desfazer" (rodapé do modo treino) | "Desfazer conclusão" na tela de conclusão e no ⋯ do cartão do treino |
| "Não realizei" / "+ Comi outra coisa" (botões no cartão) | ⋯ do cartão da refeição |
| "Sair" / "Concluir treino" (rodapé do modo treino) | X do cabeçalho / conclusão automática ou "Concluir assim" |
| Coluna de abas em telas largas | Menu aberto como coluna à esquerda |

## 7. Mudanças de comportamento

1. Voltar de qualquer tela leva ao Dojo (hoje, cada aba é um destino da barra).
2. O treino é concluído sozinho quando a última série pendente é registrada (hoje, é preciso tocar em "Concluir treino").
3. Sair do modo treino com séries registradas pergunta antes (hoje, sai direto).
4. A dica de carga some depois da primeira série de trabalho do exercício.
5. O histórico de peso (registrar, gráfico, editar e remover) fica na Evolução. O campo "Peso" do Perfil continua, e salvar com outro peso ainda cria um registro.
6. "Marcar como feito" sem séries sai do rodapé do modo treino e vai para o ⋯ do cartão do treino no Dojo.

## 8. Testes afetados

- `NavigationUiTest` depende das abas da barra e de "Meus alimentos" como subtela: reescrever para o menu, para as abas internas e para "voltar leva ao Dojo".
- `SettingsUiTest` procura seções da tela atual de Ajustes: reescrever para as linhas e as telas curtas.
- Testes de domínio não mudam. Entra um teste puro para o XP do treino do dia (seção 5.10), se o cálculo virar função de domínio.
- `TranslationsTest` cobre as frases novas automaticamente, desde que entrem em inglês e japonês.

## 9. Deixado para quem constrói

- Como implementar a folha do ⋯: `ModalBottomSheet` do Material 3 vestido com `Zen.look`, ou uma folha própria.
- Como implementar as telas curtas de Ajustes: rotas aninhadas ou um estado interno na própria tela.
- Animações de entrada do menu, da folha e da conclusão, respeitando `LocalMotionEnabled`.
- Organização dos arquivos: novas telas e componentes; onde fica `MuscleGroup.kanji()` compartilhado.
- Ordem exata dos commits dentro de cada fase.

## 10. Fases sugeridas

Cada fase deixa o app funcionando e pode virar uma versão:

1. **Casca da navegação:** sai a barra, entram o menu e o "voltar leva ao Dojo", e o menu vira coluna em telas largas. As telas continuam como estão, só com "← Dojo".
2. **Dojo:** cartões com um botão principal e ⋯, a folha do ⋯, os kanji dos músculos, os chips de horário, a faixa da semana e o estado "Em andamento".
3. **Evolução e Perfil:** abas com rótulo, o Histórico na aba Treino, o registro de peso na aba Peso e o Perfil enxuto em Sakura.
4. **Treino e Dieta:** abas internas, sanfonas, avisos de sugestão e o carrinho no cabeçalho.
5. **Ajustes:** cartão de estilo com prévia e as sete linhas.
6. **Modo treino:** cabeçalho em faixa, ⋯ no quadro, balão de dica, rodapé único com descanso, conclusão automática e tela de conclusão.

## 11. Decisões tomadas por mim (para conferir)

- O Perfil muda de Daidai para Sakura.
- A tela de conclusão **não mostra a duração do treino**: `SetLog` não guarda horário, e o desenho não muda o banco. A maquete mostrava "58 min".
- Nas sanfonas, só um item aberto por vez; o dia de hoje (Treino) e a próxima refeição (Dieta) abrem primeiro.
- "Ver no cardápio" abre a Dieta no dia de hoje.
- A dica de carga some depois da primeira série de trabalho registrada daquele exercício.
- Em telas largas, a coluna mantém a largura do `SideRail` de hoje, só com kanji e rótulo, inclui o Dojo e esconde o selo 道.
- "Marcar como feito" (sem séries) entra no ⋯ do cartão do treino, para nenhuma função sumir junto com o "Concluir treino" do rodapé.
- Estados do modo treino depois da conclusão: "Ver resumo", "Concluir treino" manual após desfazer, e conclusão automática só uma vez (seção 5.10).
- O campo "Peso" do Perfil continua editável; só o histórico de peso vai para a Evolução.
- Com sugestão pendente, Ficha e Cardápio continuam mostrando e editando a sugestão, como hoje.
