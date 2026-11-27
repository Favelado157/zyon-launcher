# Integração independente do Zyon

Compilada pelo `node build/client.cjs` na raiz do projeto (JDK 8).

O launcher baixa o JAR vanilla oficial verificado e executa `dev.zyon.bootstrap.PatchMain` para criar uma cópia integrada **localmente**. A entrada `dev.zyon.platform.Main` verifica a ausência de Forge/Launchwrapper e inicializa o vanilla com hooks próprios. Nenhum código Minecraft é distribuído no SDK.

`src/main/java/dev/zyon` contém HUD, perfis, funções da aba Xit e capas públicas OptiFine. `src/test/java` contém os testes; o workflow executa Java e Node antes da publicação.
