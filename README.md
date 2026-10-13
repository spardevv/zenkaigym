# Zenkaigym 禅

App Android de treino e dieta com visual de mangá. Você informa seus dados, e o app sugere metas, treino e dieta; **a decisão é sempre sua**: ajuste, troque itens, aceite ou descarte. Tudo funciona **offline** e os dados ficam **salvos só no aparelho** (sem login, sem internet, sem permissão de armazenamento).

## Funcionalidades

- **Dojo (início):** o treino de hoje e a próxima refeição, com um toque em **Feito ✓** para marcar. Deslize o card para ver as outras refeições do dia.
- **Treino:** dias e horários de treino (salvos automaticamente) e uma ficha sugerida (ABC, superior/inferior, push/pull/legs etc.) conforme os dias disponíveis e o objetivo.
- **Dieta:** meta diária calculada (Mifflin-St Jeor), quantidade e horário de cada refeição (3 a 6 por dia) e um plano em que cada refeição recebe alimentos adequados ao período (ovos, queijo, pão e whey de manhã; frango, carne, arroz e legumes no almoço e no jantar; iogurte e fruta na ceia).
- **Trocas:** toque em qualquer alimento ou exercício para ajustar a quantidade, trocar por uma alternativa sugerida (com a porção equivalente calculada), cadastrar uma opção própria ou remover.
- **Alimentos:** sua lista de alimentos (valores por 100 g), com lista inicial pronta, edição, "refazer sugestão" e "remover tudo".
- **Reponderação automática:** ao mudar perfil, peso, dias de treino, meta ou número de refeições, o app monta novas sugestões de treino e dieta para você revisar.
- **Tema:** claro, escuro ou seguindo o sistema (em Ajustes), além de uma configuração inicial na primeira abertura.

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
# testes unitários
./gradlew testDebugUnitTest

# gerar o APK de debug (app/build/outputs/apk/debug/app-debug.apk)
./gradlew assembleDebug
```

No Windows, use `gradlew.bat`. Também dá para abrir o projeto no Android Studio e executar no emulador ou em um aparelho.

## Licença

[MIT](LICENSE) © 2026 Gabriel Ramos
