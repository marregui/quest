# Licensed to Miguel Arregui ('marregui') under one or more contributor
# license agreements. See the LICENSE file distributed with this work
# for additional information regarding copyright ownership. You may
# obtain a copy at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an 'AS IS' BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations
# under the License.
#
# Copyright (c) 2019 - 2022, Miguel Arregui a.k.a. marregui
#

from distutils.core import setup

_REQUIREMENTS = [
    'setuptools==57.4.0',
    'argparse==1.4.0',
    'pylint==2.10.2',
    'psutil==5.8.0',
    'psycopg2==2.9.1'
]

setup(
    name='quest',
    version='1.0',
    description='Toolkit for Postgres protocol wire compatible databases',
    author='Miguel Arregui',
    author_email='miguel.arregui@gmail.com',
    url='https://github.com/marregui/mygupsql',
    packages=['quest'],
    install_requires=_REQUIREMENTS,
    license='Apache License v2.0',
    classifiers=[
        'Programming Language :: Python :: 3.9',
        'License :: OSI Approved :: Apache Software License',
        'Operating System :: Unix',
        'Operating System :: Microsoft :: Windows',
        'Operating System :: MacOS',
        'Intended Audience :: Developers',
        'Topic :: Scientific/Engineering',
        'Topic :: Software Development'
        'Topic :: Time series'
        'Topic :: High performance'
    ]
)
