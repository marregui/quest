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

from datetime import datetime as dt

import pytz

TIMESTAMP_FORMAT = "%Y-%m-%d %H:%M:%S.%f"
DATE_FORMAT_DAY = "%Y-%m-%d"


def to_date(date_value: str) -> int:
    return to_timestamp(date_value, DATE_FORMAT_DAY)


def from_date(date_micros: int) -> str:
    return from_timestamp(date_micros, DATE_FORMAT_DAY)


def to_timestamp(timestamp_value: str, timestamp_format: str = TIMESTAMP_FORMAT) -> int:
    timestamp = dt.strptime(timestamp_value, timestamp_format)
    timestamp = pytz.utc.localize(timestamp, is_dst=None).astimezone(pytz.utc)
    return int(timestamp.timestamp() * 1e6)


def from_timestamp(
    timestamp_micros: int, timestamp_format: str = TIMESTAMP_FORMAT
) -> str:
    return dt.fromtimestamp(timestamp_micros / 1e6, pytz.utc).strftime(timestamp_format)
