# Zyon Visual Client 1.3.0

Cliente Forge para Minecraft Java 1.8.9 / Forge 11.15.1.2318. Contém exclusivamente HUD, keystrokes, mira, zoom e tema. Não contém o motor de combate/movimento da edição anterior, visualização de entidades ou automação. Não fornece capas de terceiros nem integração com OptiFine.

O launcher da série 1.3 exige login Microsoft e consulta de propriedade antes de preparar ou iniciar. A autorização do aplicativo ainda está pendente após recusa anterior; o JAR não concede acesso às APIs. Não é um addon nativo LabyMod e não foi testado nessa integração.

## Controles

Shift direito abre o painel organizado por Visual, HUD e Aparência. Clique no nome para selecionar ajustes e no interruptor para ativar. Passe o mouse no ? para ver a ajuda sem ativar o recurso. Busca e roda do mouse percorrem os painéis.

Ctrl direito ou Editar HUD abre o editor dos keystrokes. Arraste para posicionar; tamanho, cor e transparência ficam nos perfis. C segura o zoom; End oculta os extras. Teclas reservadas podem ser alteradas em Opções > Controles > Zyon. Perfis locais ficam em config/zyon; somente os cinco IDs registrados podem ser carregados.

Comandos locais: `/zyon`, `/zyon toggle crosshair`, `/zyon bind crosshair R`, `/zyon bind crosshair none`, `/zyon profile save principal`, `/zyon profile load principal`, `/zyon profile list`, `/zyon panic`.

## Compilar

Instale JDK 21 para executar Gradle e JDK 8 para compilar o cliente. Execute `./gradlew build` ou `.\gradlew.bat build`. Se necessário, informe `-Porg.gradle.java.installations.paths=/caminho/para/jdk8`. O JAR distribuível é `build/libs/zyon-1.8.9-1.3.0.jar`, sem sufixos dev/sources.

Os testes verificam os cinco recursos registrados, a matemática do zoom, limites de layout, ajuda e persistência de perfis. O ambiente atual não valida uma sessão Microsoft real nem uma partida autenticada. Veja [informações para revisão](../docs/API-review.md).

Aplicativo independente de terceiros. NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
