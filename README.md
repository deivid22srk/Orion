# Orion - Emulador de Windows para Android

![Orion Banner](https://img.shields.io/badge/Orion_Emulator-Kotlin_%26_Compose-purple?style=for-the-badge)
![Progresso](https://img.shields.io/badge/Progresso_Orion-95.56%25-emerald)

**Orion** é uma recriação moderna e extremamente eficiente do emulador **Winlator-Ludashi** (originalmente desenvolvido por StevenMXZ, que por sua vez baseia-se no Winlator clássico de BrunoDev). 

O foco principal deste projeto é reescrever a interface de usuário do aplicativo usando **Kotlin**, **Jetpack Compose** e a especificação de design **Material You 3** (Material 3 com cores dinâmicas), mantendo o núcleo robusto de emulação nativa em C/C++ (JNI) intacto.

---

## 📊 Progresso da Migração

**Progresso Real Orion:** [█████████░] 95.56%

> [!NOTE]
> O progresso geral é calculado através de uma média ponderada das metas de desenvolvimento cadastradas no arquivo [progress_config.json](file:///workspaces/Tes/Orion/progress_config.json). Isso reflete o andamento real do projeto em relação à migração da UI para Compose, integração do sistema de temas e estabilização de builds.

### 📦 Progresso em Grandes Blocos

<!-- BLOCKS_PROGRESS_START -->
* **Migração da Interface (UI) para Jetpack Compose (Peso: 40%):** [████████░░] 88.89%
* **Design System & Material You 3 (Peso: 20%):** [██████████] 100.0%
* **Integração do Núcleo de Emulação (Peso: 25%):** [██████████] 100.0%
* **Infraestrutura & Automações (Peso: 15%):** [██████████] 100.0%
<!-- BLOCKS_PROGRESS_END -->

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

## 👩‍💻 Guia para o Desenvolvedor (Orientações da Arquitetura)

Se você é o próximo desenvolvedor a trabalhar neste projeto, aqui está um resumo de como estruturamos a migração e onde encontrar cada componente:

### 1. Origem e Base de Código
O Orion é um fork direto do [StevenMXZ/Winlator-Ludashi](https://github.com/StevenMXZ/Winlator-Ludashi). O Winlator executa emulação nativa no Android utilizando Wine, Box86/Box64, e exibe o ambiente gráfico via servidor X11.

### 2. A Separação entre Core (Java/C++) e UI (Kotlin/Compose)
* **Importante:** Não altere o namespace do pacote das classes nativas (como `core`, `xserver`, `renderer`, `alsaserver` e `sysvshm`) que ficam sob o pacote original `com.winlator.cmod`. As assinaturas das funções JNI no C++ (localizadas na pasta `app/src/main/cpp/winlator/`) possuem referências explícitas e estáticas a este pacote Java. Mudar o pacote dessas classes quebrará o link do carregamento nativo (`UnsatisfiedLinkError`).
* Toda a interface de usuário (UI) reescrita em Jetpack Compose está localizada no diretório [com/winlator/cmod/ui](file:///workspaces/Tes/Orion/app/src/main/java/com/winlator/cmod/ui):
  * **`/theme`**: Define o `OrionTheme`, `Color.kt` e a tipografia base.
  * **`/screens`**: Contém as telas principais reescritas em Compose (`ContainersScreen`, `ContainerDetailScreen`, `FileManagerScreen`, `ShortcutsScreen`, `SettingsScreen`, `InputControlsScreen`).
  * **`OrionApp.kt`**: Central de navegação em Compose usando Drawer lateral.
  * **`MainActivity.kt`**: Substituiu a activity Java legada, servindo como ponto de entrada que inicializa o Compose e invoca o OrionApp.

### 3. Como funciona a Execução Nativa a partir do Compose
Quando o usuário executa um container ou jogo (.exe), a interface Compose se comunica com a Activity de emulação nativa:
* Os callbacks do Compose disparam intents configurados para iniciar a **`XServerDisplayActivity`** passando o `container_id` e caminhos de atalhos temporários gerados em cache para executáveis (ex. `temp_run.desktop`).

---

## 🛠️ Compilação

O projeto conta com o pipeline do GitHub Actions para compilar o APK automaticamente a cada commit na branch principal.

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

## 🤖 Como Atualizar o Progresso do Projeto

Não edite o progresso diretamente neste README. Ele é calculado de forma automatizada:
1. Abra o arquivo [progress_config.json](file:///workspaces/Tes/Orion/progress_config.json).
2. Atualize o valor de `"progress"` (de 0 a 100) da tarefa que você concluiu. Se quiser, você pode adicionar novas tarefas/módulos na lista.
3. Faça commit e push. O pipeline do GitHub Actions executará o script `scripts/update_progress.py` que calcula a média ponderada do progresso das tarefas e atualiza a barra de progresso, a seção de grandes blocos e o badge do README automaticamente!
