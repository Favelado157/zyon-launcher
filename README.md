# Servidor local Minecraft Java 1.8.9 para Windows

[Baixar o ZIP](https://github.com/Favelado157/zyon-launcher/raw/refs/heads/minecraft-local-server/Servidor-Minecraft-Local-1.8.9.zip)

1. Extraia o ZIP e abra **INICIAR-SERVIDOR.bat**.
2. Leia a EULA do Minecraft e aceite somente se concordar.
3. Aguarde os downloads oficiais e a mensagem **Done**.
4. No Minecraft Java 1.8.9, abra **Multijogador > Conexão direta** e entre em **127.0.0.1:25565**.
5. Para salvar e fechar, digite **stop** no terminal do servidor.

O servidor aceita nick offline e é acessível somente neste PC, em loopback. Não configura portas, roteador ou acesso público. Requer Windows 10/11 de 64 bits e internet na primeira preparação. O Java 8 portátil é baixado automaticamente.

O mundo fica em **servidor/world**. Memória padrão: 1 GB, configurável em **config.json**. Mais instruções no **LEIA-ME.txt**.

Servidor vanilla oficial; não acompanha plugins ou mods de servidor. Os binários de Minecraft e Java não estão incluídos: são baixados de Mojang e Eclipse Temurin/Adoptium com verificação de integridade.

Verificação realizada com Java 8: inicialização do servidor real, resposta ao protocolo Minecraft 1.8.9 em loopback e salvamento do mundo ao encerrar. A sintaxe do script PowerShell foi verificada; o iniciador Windows ainda precisa ser executado no PC do usuário.
