"""Prepara um ambiente separado e executa o bot no PC do usuario."""
import getpass
import hashlib
import os
from pathlib import Path
import re
import subprocess
import sys
import venv

BASE = Path(__file__).resolve().parent


def configurar_token():
    print("\nAbra https://discord.com/developers/applications")
    print("Escolha seu aplicativo > Bot > Reset Token > Copy.")
    print("Cole o TOKEN DO BOT abaixo. Ele nao aparecera enquanto voce digita.")
    token = getpass.getpass("Token: ").strip()
    if not re.fullmatch(r"[A-Za-z0-9_.-]{20,}", token):
        raise ValueError("Token vazio ou com formato invalido. Cole somente o token, sem aspas.")
    target = BASE / ".env"
    temporary = BASE / ".env.tmp"
    temporary.write_text("DISCORD_TOKEN=" + token + "\n", encoding="utf-8")
    temporary.replace(target)
    print("Token salvo somente no arquivo .env deste PC. Nao compartilhe esse arquivo.")


def tem_token():
    if os.environ.get("DISCORD_TOKEN", "").strip():
        return True
    target = BASE / ".env"
    if not target.exists():
        return False
    return bool(re.search(r"(?m)^\s*DISCORD_TOKEN\s*=\s*[^\s#]+", target.read_text(encoding="utf-8-sig")))


def main():
    if sys.version_info < (3, 11):
        raise RuntimeError("Instale Python 3.12 ou mais recente em https://www.python.org/downloads/windows/")
    if "--configurar" in sys.argv:
        configurar_token()
        return 0
    env_dir = BASE / ".venv"
    python = env_dir / ("Scripts/python.exe" if os.name == "nt" else "bin/python")
    if not python.exists():
        print("Preparando o ambiente Python. Aguarde...", flush=True)
        venv.EnvBuilder(with_pip=True).create(env_dir)
    requirements = BASE / "requirements.txt"
    fingerprint = hashlib.sha256(requirements.read_bytes()).hexdigest()
    installed = env_dir / "dependencias.sha256"
    if not installed.exists() or installed.read_text() != fingerprint:
        print("Instalando dependencias e FFmpeg. Precisa de internet na primeira vez.", flush=True)
        subprocess.run([str(python), "-m", "pip", "install", "--disable-pip-version-check", "--upgrade", "-r", str(requirements)], check=True)
        installed.write_text(fingerprint)
    if not tem_token():
        configurar_token()
    print("\nIniciando o bot. Mantenha esta janela aberta; Ctrl+C encerra.", flush=True)
    child_env = os.environ.copy()
    child_env["PATH"] = str(python.parent) + os.pathsep + child_env.get("PATH", "")
    return subprocess.call([str(python), "-u", str(BASE / "bot.py")], cwd=BASE, env=child_env)


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        print("\nBot encerrado.")
        raise SystemExit(0)
    except (OSError, ValueError, RuntimeError, subprocess.CalledProcessError) as error:
        print("\nNao foi possivel iniciar:", error)
        print("Consulte LEIA-ME.txt. Se pedir ajuda, nunca envie seu token ou o arquivo .env.")
        raise SystemExit(1)
