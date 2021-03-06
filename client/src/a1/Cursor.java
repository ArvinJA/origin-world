/*
 *  This file is part of the Origin-World game client.
 *  Copyright (C) 2012 Arkadiy Fattakhov <ark@ark.su>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package a1;

import java.io.ByteArrayInputStream;

import a1.utils.CursorLoader;
import a1.utils.Resource;
import a1.utils.Resource.ResCursor;

public class Cursor {

	// handle net
	public static void setCursor(String name) {
		if (name.equals("")) name = "arrow";
		ResCursor c = Resource.cursors.get(name);
		if (c != null) {
			ByteArrayInputStream in = new ByteArrayInputStream(c.png_data);
			CursorLoader.SetCursor(in, c.offx, c.offy);
		}
	}
}
