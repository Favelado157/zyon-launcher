import hashlib
import os
from pathlib import Path
import subprocess
import sys
import venv
from contextlib import contextmanager

BASE = Path(__file__).resolve().parent


@contextmanager
def instance_lock():
    with (BASE / '.aplicador.lock').open('a+b') as handle:
        if handle.tell() == 0:
            handle.write(b'0'); handle.flush()
        handle.seek(0)
        try:
            if os.name == 'nt':
                import msvcrt
                msvcrt.locking(handle.fileno(), msvcrt.LK_NBLCK, 1)
            else:
                import fcntl
                fcntl.flock(handle.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
        except OSError:
            raise RuntimeError('O aplicador já está aberto nesta pasta. Use apenas uma janela por vez.')
        try:
            yield
        finally:
            handle.seek(0)
            if os.name == 'nt':
                msvcrt.locking(handle.fileno(), msvcrt.LK_UNLCK, 1)
            else:
                fcntl.flock(handle.fileno(), fcntl.LOCK_UN)


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
    return subprocess.call([str(python), '-u', str(BASE / 'aplicar.py'), *sys.argv[1:]], cwd=BASE)


if __name__ == '__main__':
    try:
        with instance_lock():
            raise SystemExit(main())
    except KeyboardInterrupt:
        print('\nCancelado.')
        raise SystemExit(0)
    except (RuntimeError, OSError, subprocess.CalledProcessError) as error:
        print('Nao foi possivel iniciar:', str(error))
        print('Consulte LEIA-ME.txt. Nao compartilhe tokens.')
        raise SystemExit(1)
