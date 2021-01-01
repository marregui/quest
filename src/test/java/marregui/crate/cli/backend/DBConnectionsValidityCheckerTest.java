/* **
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2020, Miguel Arregui a.k.a. marregui
 */

package marregui.crate.cli.backend;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Simple program that exemplifies the use of
 * {@link DBConnsValidityChecker} enabling manual testing against a
 * running database.
 * <p>
 * To run the database follow the instructions found here <a href=
 * "https://github.com/marregui/cratedb-local-install-tools">cratedb-local-install-tools</a>.
 * <p>
 * Once the database is running, you can:
 * <ul>
 * <li>Run this program, which gives you 90 seconds to complete the next
 * steps.</li>
 * <li>Kill the running database process.</li>
 * <li>Wait 30 seconds for the validity checks to confirm all connections were
 * lost.</li>
 * </ul>
 * If 90 seconds are too long for you, change
 * {@link DBConn#ISVALID_TIMEOUT_SECS} to a lower value, as the
 * period for the checker is 3x this value.
 */
public class DBConnectionsValidityCheckerTest {

    /**
     * To exemplify the use of the checker.
     * 
     * @param args none expected
     * @throws Exception ignored
     */
    public static void main(String[] args) throws Exception {
        int runtimeSeconds = 90;
        DBConn conn1 = new DBConn("default1");
        DBConn conn2 = new DBConn("default2");
        List<DBConn> conns = new ArrayList<>(2);
        conns.add(conn1);
        conns.add(conn2);
        DBConnsValidityChecker checker = new DBConnsValidityChecker(() -> conns, (lostConns) -> {
            System.out.println("Lost connections: " + lostConns);
        });
        try {
            AtomicInteger failedOpenCount = new AtomicInteger();
            conns.forEach(conn -> {
                try {
                    conn.open();
                }
                catch (SQLException e) {
                    failedOpenCount.incrementAndGet();
                    System.out.println("Failed to open: " + conn);
                }
            });
            if (conns.size() - failedOpenCount.get() > 0) {
                checker.start();

                // wait to allow user to stop the database
                System.out.printf("Will run for %d seconds%n", runtimeSeconds);
                TimeUnit.SECONDS.sleep(runtimeSeconds);
            }
            else {
                System.out.println("Not a single open connection to check");
            }
        }
        finally {
            conns.forEach(DBConn::close);
            if (checker.isRunning()) {
                checker.close();
            }
            System.out.println("Done");
        }
    }
}
