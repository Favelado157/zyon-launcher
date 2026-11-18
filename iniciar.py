import hashlib
import os
from pathlib import Path
import subprocess
import sys
import venv

BASE = Path(__file__).resolve().parent


def main():
    if sys.version_info < (3, 11):
        raise RuntimeError('Instale Python 3.12 para Windows em https://www.python.org/downloads/windows/')
    folder = BASE / '.venv'
    python = folder / ('Scripts/python.exe' if os.name == 'nt' else 'bin/python')
    if not python.exists():
        print('Preparando o ambiente Python...', flush=True)
        venv.EnvBuilder(with_pip=True).create(folder)
    requirements = BASE / 'requirements.txt'
    digest = hashlib.sha256(requirements.read_bytes()).hexdigest()
    marker = folder / 'dependencias.sha256'
    if not marker.exists() or marker.read_text() != digest:
        print('Instalando discord.py. Aguarde...', flush=True)
        subprocess.run([str(python), '-m', 'pip', 'install', '--disable-pip-version-check', '-r', str(requirements)], check=True)
        marker.write_text(digest)
    return subprocess.call([str(python), '-u', str(BASE / 'aplicar.py')], cwd=BASE)


if __name__ == '__main__':
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        print('\nCancelado.')
        raise SystemExit(0)
    except (RuntimeError, OSError, subprocess.CalledProcessError) as error:
        print('Nao foi possivel iniciar:', str(error))
        print('Consulte LEIA-ME.txt. Nao compartilhe tokens.')
        raise SystemExit(1)
