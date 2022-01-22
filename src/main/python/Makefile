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

.DEFAULT_GOAL := build

poetry ?= poetry

test:
	$(poetry) run pytest --junitxml=test-report.xml tests

install:
	$(poetry) install

format-code:
	$(poetry) run black .
	$(poetry) run isort . --profile black

run-code-quality:
	$(poetry) run flake8
	$(poetry) run black . --check
	$(poetry) run isort . --check-only --profile black
	$(poetry) run bandit .
	$(poetry) run safety check

-include ../Mk/phonies
