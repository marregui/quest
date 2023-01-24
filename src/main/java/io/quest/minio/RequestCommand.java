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
 * Copyright (c) 2019 - 2023, Miguel Arregui a.k.a. marregui
 */

package io.quest.minio;

import java.util.HashMap;
import java.util.Map;

/**
 * Parser for commands received from a HTTP request
 *
 * @author marregui
 */
public class RequestCommand {
    private static final String COMMAND_SEP = "&";
    private static final String PARAM_SEP = "=";
    public Command cmd;
    public Map<String, String> parameters;

    private RequestCommand(String queryString) {
        this.cmd = Command.UNKNOWN;
        this.parameters = new HashMap<String, String>();
        if (null != queryString && false == queryString.isEmpty()) {
            String[] parts = queryString.split(COMMAND_SEP);
            this.cmd = Command.match(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                String params[] = parts[i].split(PARAM_SEP);
                if (false == params[0].isEmpty() && false == params[0].trim().isEmpty()) {
                    this.parameters.put(params[0].trim(), consolidate(params, PARAM_SEP));
                }
            }
        }
    }

    /**
     * Factory method
     *
     * @param queryString
     * @return The command
     */
    public static RequestCommand apply(String queryString) {
        return new RequestCommand(queryString);
    }

    private static String consolidate(String[] params, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < params.length; i++) {
            sb.append(params[i]).append(separator);
        }
        if (params.length > 1) {
            sb.setLength(sb.length() - separator.length());
        }
        return sb.toString().trim();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Command: ").append(this.cmd).append("\n");
        for (String key : this.parameters.keySet()) {
            sb.append("  - ").append(key).append(": ").append(this.parameters.get(key)).append("\n");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    /**
     * Represents the different commands available from the servlet
     */
    public static enum Command {
        UNKNOWN, LIST, GET;

        public static Command match(String command) {
            Command target = Command.UNKNOWN;
            if (null != command) {
                String lowerCaseCommand = command.trim().toLowerCase();
                for (Command cmd : Command.values()) {
                    if (lowerCaseCommand.equals(cmd.name().toLowerCase())) {
                        target = cmd;
                        break;
                    }
                }
            }
            return target;
        }
    }
}