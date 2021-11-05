# Licensed to Miguel Arregui ("marregui") under one or more contributor
# license agreements. See the LICENSE file distributed with this work
# for additional information regarding copyright ownership. You may
# obtain a copy at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations
# under the License.
#
# Copyright (c) 2019 - 2022, Miguel Arregui a.k.a. marregui
#

import argparse
import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

VERSION = '1.0.0'

# User local QuestDB installation folder
QDB_HOME = Path.home() / '.quest'

# QuestBD's storage root folder contains db, conf
QDB_DB_ROOT = QDB_HOME / 'ROOT'

# QuestBD's storage data folder
QDB_DB_DATA = QDB_DB_ROOT / 'db'

# QuestBD's configuration folder, if not exists, create a default one
QDB_DB_CONF = QDB_DB_ROOT / 'conf'

# Git clone, automatically checked out on server start, or on module command 'update'
QDB_CLONE_FOLDER = QDB_HOME / 'clone'


def _update_command(branch_name: str, force: bool):
    if not QDB_HOME.exists():
        QDB_HOME.mkdir()
        print(f'Created home: {QDB_HOME}')
    if not QDB_DB_ROOT.exists():
        QDB_DB_ROOT.mkdir()
        print(f'Created data root: {QDB_DB_ROOT}')
    if force and QDB_CLONE_FOLDER.exists():
        try:
            shutil.rmtree(QDB_CLONE_FOLDER)
            print('Deleted clone')
        except OSError as e:
            print(f'Error deleting clone: {e.filename} - {e.strerror}.')
            sys.exit(1)
    if not QDB_CLONE_FOLDER.exists():
        subprocess.check_output(
            ['git', 'clone', '-b', branch_name, 'git@github.com:questdb/questdb.git', 'clone'],
            cwd=QDB_HOME)
    else:
        subprocess.check_output(['git', 'checkout', branch_name], cwd=QDB_CLONE_FOLDER)
        subprocess.check_output(['git', 'pull'], cwd=QDB_CLONE_FOLDER)
    subprocess.check_output(['mvn', 'clean', 'install', '-DskipTests'], cwd=QDB_CLONE_FOLDER)
    print('Update completed')


def _start_command():
    qdb_jar = _find_jar()
    if not qdb_jar:
        print(f'QuestDB jar not found, try updating first.')
        sys.exit(1)
    _ensure_conf_exists()
    try:
        comand = [
            'java',
            '-ea',
            '-Dnoebug',
            '-XX:+UnlockExperimentalVMOptions',
            '-XX:+AlwaysPreTouch',
            '-XX:+UseParallelOldGC',
            '-Dout=/log.conf',
            '-cp', f'{QDB_DB_CONF}:{qdb_jar}',
            'io.questdb.ServerMain',
            '-d', str(QDB_DB_ROOT),
            '-n'  # disable handling of SIGHUP (close on terminal close)
        ]
        with subprocess.Popen(comand,
                              stdout=subprocess.PIPE,
                              universal_newlines=True,
                              cwd=QDB_CLONE_FOLDER) as qdb_proc_pipe:
            for stdout_line in iter(qdb_proc_pipe.stdout.readline, ''):
                print(stdout_line, end='')
            return_code = qdb_proc_pipe.wait()
        if return_code:
            raise subprocess.CalledProcessError(return_code, comand)
    except KeyboardInterrupt:
        pass


def _ensure_conf_exists() -> None:
    if not QDB_DB_CONF.exists():
        print('QuestDB conf folder not found')
        import pykit

        src_file = Path(pykit.__file__).parent / 'resources' / 'conf.zip'
        with zipfile.ZipFile(src_file, 'r') as zip_ref:
            for file in zip_ref.namelist():
                zip_ref.extract(member=file, path=QDB_DB_ROOT)
            print(f'Created default conf: {QDB_DB_CONF}')


def _find_jar() -> Path:
    dirs = [QDB_CLONE_FOLDER]
    while dirs:
        cur_dir = dirs.pop()
        if cur_dir.exists():
            for file_name in os.listdir(cur_dir):
                candidate = cur_dir / file_name
                if candidate.is_dir():
                    dirs.append(candidate)
                elif is_qdb_jar(file_name):
                    return candidate
    return None


def is_qdb_jar(file_name: str) -> bool:
    return file_name.endswith('.jar') and file_name.startswith('questdb-') and '-tests' not in file_name


def _args_parser() -> argparse.ArgumentParser:
    args_parser = argparse.ArgumentParser(description='quest commands to run QuestDB')
    command = args_parser.add_subparsers(dest='command')
    command.add_parser('start', help='Starts QuestDB in localhost')
    update = command.add_parser('update', help='Clones/builds QuestDB\'s github repo')
    update.add_argument(
        '--branch',
        default='master',
        type=str,
        help='prepare a QuestDB node from the latest version of BRANCH (default master)')
    update.add_argument(
        '--force',
        default=False,
        type=bool,
        help='FORCE True to delete/clone/build QuestDB\'s github repo')
    return args_parser


if __name__ == '__main__':
    parser = _args_parser()
    args = parser.parse_args()
    if args.command is None or args.command == 'start':
        _start_command()
    elif args.command == 'update':
        _update_command(args.branch, args.force)
    else:
        parser.print_usage()
