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

import unittest

from qdbpy import (
    to_timestamp,
    from_timestamp,
    to_date,
    from_date
)


class TimestampTest(unittest.TestCase):
    def test_timestamp(self):
        timestamp_value = to_timestamp('2021-10-01 09:38:42.123456')
        self.assertEqual('2021-10-01 09:38:42.123456', from_timestamp(timestamp_value))

    def test_date(self):
        date_value = to_date('2021-10-01')
        self.assertEqual('2021-10-01', from_date(date_value))
