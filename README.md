# Orion - Emulador de Windows para Android

![Orion Banner](https://img.shields.io/badge/Orion_Emulator-Kotlin_%26_Compose-purple?style=for-the-badge)
![Progresso](https://img.shields.io/badge/Progresso_Orion-91.11%25-emerald)

**Orion** é uma recriação moderna e extremamente eficiente do emulador **Winlator-Ludashi** (originalmente desenvolvido por StevenMXZ). O foco principal deste projeto é reescrever a interface de usuário do aplicativo usando **Kotlin**, **Jetpack Compose** e a especificação de design **Material You 3** (Material 3 com cores dinâmicas), mantendo o núcleo robusto de emulação nativa em C/C++ (JNI).

---

## 📊 Progresso da Migração

**Progresso da Conversão Geral:** [█████████░] 91.11%

> [!NOTE]
> O progresso geral é calculado através de uma média ponderada das metas de desenvolvimento cadastradas no arquivo [progress_config.json](file:///workspaces/Tes/Orion/progress_config.json). Isso reflete o andamento real do projeto em relação à migração da UI para Compose, integração do sistema de temas e estabilização de builds.

### Status dos Módulos da UI

| Módulo/Tela | Tecnologia Original | Nova Tecnologia | Status |
| :--- | :--- | :--- | :--- |
| **MainActivity** | Java + XML | Kotlin + Jetpack Compose |  Concluído |
| **Containers List** | Java + XML (RecyclerView) | Jetpack Compose (LazyColumn) |  Concluído |
| **Container Editor** | Java + XML (Nested Forms) | Jetpack Compose (Material 3 Tabs) |  Concluído |
| **Input Controls** | Java + XML (Custom Dialogs) | Jetpack Compose (Material 3 Cards) |  Concluído |
| **App Settings** | Java PreferenceFragment | Jetpack Compose (Material 3 Switches) |  Concluído |
| **Navegação (Sidebar)** | DrawerLayout + NavigationView | ModalNavigationDrawer (Material 3) |  Concluído |
| **Material You 3 Theme** | XML AppTheme (Estático) | OrionTheme (Dynamic Color support) |  Concluído |

---

##  Recursos Principais

* **Material You 3 Integration:** Interface limpa que se adapta dinamicamente às cores do papel de parede do usuário (Android 12+).
* **UI mais Fluida:** Substituição do sistema de Views antigo e layouts pesados XML por renderização reativa em Jetpack Compose.
* **Afinidade de CPU Avançada:** Interface de edição com suporte a switches individuais por núcleo de processador para otimização fácil.
* **Workflow Integrado:** Compilação automatizada do APK via GitHub Actions e atualizações automáticas do status de progresso no README.md.

---

## 🛠️ Compilação

O projeto conta com o pipeline do GitHub Actions para compilar o APK automaticamente a cada commit na branch principal (`main` ou `master`).

### Compilação Local

Se desejar compilar localmente, garanta que você tenha o JDK 17 e o Android NDK instalados:

```bash
# Dar permissão de execução
chmod +x gradlew

# Compilar o APK de depuração (Debug)
./gradlew assembleDebug
```

O APK gerado estará disponível em `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🤖 Atualização Automática do Progresso

A barra e o badge de progresso deste README são atualizados de forma totalmente automatizada. Toda vez que um commit é enviado para o repositório, o GitHub Actions executa o script `/scripts/update_progress.py` que calcula a proporção do código migrado e faz o commit de atualização do README.md.
