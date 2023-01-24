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

import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;

public class Conf {

    public static final String BUCKET = "detached-partitions";
    public static final String ENDPOINT = "http://127.0.0.1:9000";
    public static final String ACCESS_KEY = "AKIAVJ5HNS7AWVW7ES6U";
    public static final String SECRET_KEY = "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";
    private static final String PROPERTIES_FILE_NAME = "datachest.properties";
    private static final Properties props = new Properties();

    static {
        // Defaults
        props.put("datachest.service.data.folder", "/services/www/isoc/Datachest");
        props.put("datachest.client.service.url", "http://integral.esac.esa.int/datachest");
        props.put("datachest.client.images.folder.url", "http://integral.esac.esa.int/isoc/lightCurveViewer/images");
        props.put("datachest.client.downloads.folder.name", "Downloads");

        try {
            URL configFileUrl = Conf.class.getResource("/" + PROPERTIES_FILE_NAME);
            props.load(new FileInputStream(configFileUrl.getPath()));
            for (Key key : Key.values()) {
                String value = props.getProperty(key.get());
                if (null == value) {
                    throw new Exception(String.format("Missing value for property: %s", key.get()));
                }
                System.out.printf("key: %s -> %s%n", key.get(), value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getProperty(Key key) {
        return props.getProperty(key.get());
    }

    public enum Key {
        ClientImagesFolderUrl("datachest.client.images.folder.url"),
        ClientDownloadsFolderName("datachest.client.downloads.folder.name"),
        ServiceUrl("datachest.client.service.url"),
        ServiceDataFolder("datachest.service.data.folder");

        private final String key;

        Key(String key) {
            this.key = key;
        }

        public String get() {
            return key;
        }
    }
}
