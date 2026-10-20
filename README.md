# Zenkaigym 禅

App Android de treino e dieta com visual de mangá. Você informa seus dados, e o app sugere metas, treino e dieta; **a decisão é sempre sua**: ajuste, troque itens, aceite ou descarte. Tudo funciona **offline** e os dados ficam **salvos só no aparelho** (sem login, sem internet, sem permissão de armazenamento).

## Funcionalidades

- **Dojo (início):** o treino de hoje e a próxima refeição, com um toque em **Feito ✓** para marcar. Deslize o card para ver as outras refeições do dia.
- **Treino:** dias e horários de treino (salvos automaticamente) e uma ficha sugerida (ABC, superior/inferior, push/pull/legs etc.) conforme os dias disponíveis e o objetivo.
- **Dieta:** meta diária calculada (Mifflin-St Jeor), quantidade e horário de cada refeição (3 a 6 por dia) e um plano em que cada refeição recebe alimentos adequados ao período (ovos, queijo, pão e whey de manhã; frango, carne, arroz e legumes no almoço e no jantar; iogurte e fruta na ceia).
- **Trocas:** toque em qualquer alimento ou exercício para ajustar a quantidade, trocar por uma alternativa sugerida (com a porção equivalente calculada), cadastrar uma opção própria ou remover.
- **Lista de compras:** a partir do cardápio ativo, soma cada alimento para a semana, a quinzena ou o mês (a partir de hoje), agrupado por categoria e arredondado para cima (unidades como ovos ou pães em quantidade inteira). Dá para compartilhar (WhatsApp, notas, e-mail…) ou copiar o texto.
- **Alimentos:** sua lista de alimentos (valores por 100 g), com lista inicial pronta, edição, "refazer sugestão" e "remover tudo".
- **Reponderação automática:** ao mudar perfil, peso, dias de treino, meta ou número de refeições, o app monta novas sugestões de treino e dieta para você revisar.
- **Modo treino:** um exercício por vez, deslizando na horizontal, com atalhos para cada exercício. Dá para trocar um exercício por outro do mesmo grupo muscular só no treino do dia (a ficha não muda).
- **Comi outra coisa:** no Dojo, anote um alimento da lista ou um prato livre (nome + kcal estimadas) numa refeição; as refeições que faltam no dia se reajustam para fechar a meta, sem mudar o cardápio.
- **Tema e estilo:** claro, escuro ou seguindo o sistema, e o estilo **Shōnen** (mangá de ação) ou **Kawaii** (tons pastel de rosa, lilás, roxo e azul-bebê, cantos redondos), em Ajustes e na configuração inicial.

## Tecnologias

Kotlin · Jetpack Compose (Material 3) · Room · Navigation Compose · Coroutines/Flow. `minSdk 24`, `targetSdk 35`.

## Estrutura

```
app/src/main/java/com/zenkaigym/
├── data/      Room (entidades, DAOs, migrações), repositório e preferências
├── domain/    Regras puras e testáveis: planejadores de dieta e treino, nutrição, agenda de refeições
└── ui/        Telas, componentes e tema em Compose, ViewModel e navegação
```

## Como rodar

Pré-requisitos: Android Studio recente e JDK 17+.

```bash
# testes unitários (JVM, sem emulador): regras de domínio, validadores, traduções, datas
./gradlew testDebugUnitTest

# testes instrumentados e de interface (Compose + Room): exigem emulador ou aparelho conectado
./gradlew connectedDebugAndroidTest

# toda a suíte de uma vez
./gradlew testAll

# gerar o APK de debug (app/build/outputs/apk/debug/app-debug.apk)
./gradlew assembleDebug
```

No Windows, use `gradlew.bat`. Também dá para abrir o projeto no Android Studio e executar no emulador ou em um aparelho.

## Licença

[MIT](LICENSE) © 2026 Gabriel Ramos
