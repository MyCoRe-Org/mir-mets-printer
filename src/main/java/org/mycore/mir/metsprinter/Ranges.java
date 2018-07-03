/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  This program is free software; you can use it, redistribute it
 *  and / or modify it under the terms of the GNU General Public License
 *  (GPL) as published by the Free Software Foundation; either version 2
 *  of the License or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program, in a file called gpl.txt or license.txt.
 *  If not, write to the Free Software Foundation Inc.,
 *  59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 */

package org.mycore.mir.metsprinter;

import java.util.TreeSet;

public class Ranges {
    TreeSet<Range> ranges;

    public Ranges(String expr) {
        String[] split = expr.split(",");
        ranges = new TreeSet<Range>();
        for (String exp : split) {
            ranges.add(new Range(exp));
        }
    }

    public boolean isIn(int number) {
        for (Range r : ranges) {
            if (r.isIn(number)) {
                return true;
            }
        }
        return false;
    }

    public int getAmount() {
        int amount = 0;
        for (Range r : ranges) {
            amount += r.getAmount();
        }
        return amount;
    }

    public String toString() {
        boolean c = false;
        StringBuilder sb = new StringBuilder();
        for (Range range : ranges) {
            if (c) {
                sb.append(',');
            } else {
                c = true;
            }
            sb.append(range.toString());
        }
        return sb.toString();
    }
}
