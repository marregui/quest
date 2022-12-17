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
 * Copyright (c) 2019 - 2022, Miguel Arregui a.k.a. marregui
 */

package io.quest.plot;

import java.awt.geom.Point2D;

public class Range implements Cloneable {

    static final double UNDEFINED = Double.MIN_VALUE;
    Point2D.Double min;
    Point2D.Double max;

    public Range() {
        min = new Point2D.Double(UNDEFINED, UNDEFINED);
        max = new Point2D.Double(UNDEFINED, UNDEFINED);
    }

    public Range(double minx, double maxx, double miny, double maxy) {
        min.x = minx;
        max.x = maxx;
        min.y = miny;
        max.y = maxy;
    }

    public Range(Point2D.Double min, Point2D.Double max) {
        this.min = min;
        this.max = max;
    }

    public void setMin(Point2D.Double min) {
        this.min = min;
    }

    public void setMin(double minx, double miny) {
        min = new Point2D.Double(minx, miny);
    }

    public void setMax(Point2D.Double max) {
        this.max = max;
    }

    public void setMax(double maxx, double maxy) {
        max = new Point2D.Double(maxx, maxy);
    }

    public Point2D.Double getInside(Point2D.Double point) {
        Point2D.Double pointInside = (Point2D.Double) point.clone();
        if (pointInside.x < min.x) {
            pointInside.x = min.x;
        } else if (pointInside.x > max.x) {
            pointInside.x = max.x;
        }
        if (pointInside.y < min.y) {
            pointInside.y = min.y;
        } else if (pointInside.y > max.y) {
            pointInside.x = max.y;
        }
        return pointInside;
    }

    @Override
    public Object clone() {
        return new Range((Point2D.Double) min.clone(), (Point2D.Double) max.clone());
    }

    @Override
    public String toString() {
        return "min: " + min + ", max: " + max;
    }

    public boolean isUndefined() {
        return min.x == UNDEFINED && min.y == UNDEFINED && max.x == UNDEFINED && max.y == UNDEFINED;
    }
}
