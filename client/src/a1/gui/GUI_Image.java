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
package a1.gui;

import org.newdawn.slick.Color;

import a1.Coord;
import a1.Packet;
import a1.net.NetGame;

// класс для вывода картинки из скина
public class GUI_Image extends GUI_Control {
	// прилеплена к мыши
	boolean drag = false;
	Coord drag_offset = Coord.z;
	
	static {
		GUI.AddType("gui_image", new ControlFactory() {		
			public GUI_Control create(GUI_Control parent) {
				return new GUI_Image(parent);
			}
		});
	}	
	
	public GUI_Image(GUI_Control parent) {
		super(parent);
	}
	
	public boolean DoMouseBtn(int btn, boolean down) {
		if (drag) return false;
		
		if (MouseInMe() && down) {
			if (id > 0)
				NetGame.SEND_gui_click(pos.x, pos.y, gui.mouse_pos.x - abs_pos.x, gui.mouse_pos.y - abs_pos.y, id, btn);
			return true;
		}
		return false;
	}
	
	public boolean CheckMouseInControl() {
		if (drag) return false;
		else return visible;
	}
	
	public void DoUpdate() {
		if (drag) {
			SetSize(getSkin().GetElementSize(skin_element));
			SetPos(gui.mouse_pos.sub(drag_offset));
			BringToFront();
		}
	}
	
	public void DoRender() {
		Color c = drag?(new Color(255,255,255,200)):Color.white;
		getSkin().Draw(skin_element, abs_pos.x, abs_pos.y,size.x,size.y,Skin.StateNormal,c);
	}
	
	// прочитать параметры контрола из сети
	public void DoNetRead(Packet pkt) {
		//Log.info("GUI_Image DoNetRead");
		skin_element = pkt.read_string_ascii();
		SetSize(getSkin().GetElementSize(skin_element));
		int d = pkt.read_byte();
		if (d == 1) {
			drag = true;
			int x = pkt.read_int();
			int y = pkt.read_int();
			drag_offset = new Coord(x, y);
		} else {
			drag = false;
			drag_offset = Coord.z;
		}
	} 
}
