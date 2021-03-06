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
package a1.net;

import static a1.Main.*;
import static a1.net.NetPacketsID.*;

import a1.Log;


import a1.ActionsMenu;
import a1.Coord;
import a1.Cursor;
import a1.FlyText;
import a1.Main;
import a1.MapCache;
import a1.ObjCache;
import a1.Packet;
import a1.Player;
import a1.dialogs.Dialog;
import a1.dialogs.dlg_Context;
import a1.dialogs.dlg_Game;
import a1.dialogs.dlg_Progress;
import a1.dialogs.dlg_SysMsg;
import a1.gui.GUI;
import a1.gui.GUI_Chat;
import a1.gui.GUI_Control;

public class NetGame {
	static final int PING_TIME = 3000;
	
	public static long LastPingTime = 0;
	public static long Ping = 0;
	
	static public void ProcessPackets() {
		if (GameConnect != null) {
			while (true) {
				Packet pkt = null;
				if (GameConnect != null)
					synchronized (GameConnect.packets) {
						if (!GameConnect.packets.isEmpty())
							pkt = GameConnect.packets.removeLast();
						else
							break;
					}
				else
					break;
				if (pkt != null)
					try {
						ParsePacket(pkt);
					} catch (Exception e) {
						e.printStackTrace();
						NetError("error_parse_packet");
					}
			}
		}
		
		if (Player.CharID != 0 && GameConnect != null && GameConnect.Alive()) {
			if (System.currentTimeMillis() > LastPingTime + PING_TIME) {
				LastPingTime = System.currentTimeMillis();
				Packet p = new Packet(GAMESERVER_PING);
				GameConnect.Send(p);
			}
		}
	}
	
	static public void Reset() {
		LastPingTime = 0;
		Player.CharID = 0;
		Ping = 0;
	}

	static void ParsePacket(Packet pkt) {
		if (net_Craft.PacketHandler(pkt)) return;
		
		int val, id, x, y, i, count;
		String name, s;
		Packet p;
		GUI_Control c;
		switch (pkt.type) {
		case GAMESERVER_HELO :
			val = pkt.read_int();
			if (val != 333) {
				NetError("wrong_helo");
				return;
			}
			p = new Packet(GAMESERVER_COOKIE);
			p.write_bytes(NetLogin.cookie);
			GameConnect.Send(p);
			break;
		case GAMESERVER_LOGGED :
			Player.CharID = pkt.read_int();
			Player.CharName = pkt.read_string_ascii();
			Player.GlobalTime = pkt.read_int();
			Log.info("Success logged!");

			Dialog.Show("dlg_game");
			Main.StartMusic();
			break;
		case GAMESERVER_PONG :
			Ping = System.currentTimeMillis() - LastPingTime;
			Player.GlobalTime = pkt.read_int();
			//Log.info("RECV Pong, ping="+Ping);
			break;
		case GAMESERVER_SET_KIN_INFO :
			id = pkt.read_int();
			name = pkt.read_string_ascii();
			ObjCache.KinInfo(id, name);
			break;
		case GAMESERVER_SET_FOLLOW : // TODO : delete
			id = pkt.read_int();
			int tgt,offx, offy, addz;
			tgt = pkt.read_int();
			offx = pkt.read_int();
			offy = pkt.read_int();
			addz = pkt.read_int();
			ObjCache.Follow(id,tgt,offx,offy,addz);
			break;
		case GAMESERVER_OBJ_DELETE_PARAM :
			id = pkt.read_int();
			int par = pkt.read_int();
			ObjCache.remove_param(id, par);
			break;
		case GAMESERVER_OBJ_CLEAR_PARAMS:
			id = pkt.read_int();
			ObjCache.clear_params(id);
			break;
        case GAMESERVER_OBJ_TYPE:
            id = pkt.read_int();
            s = pkt.read_string_ascii();
            ObjCache.obj_type(id, s);
            break;
		case GAMESERVER_OBJ_MOVE : 
			id = pkt.read_int();
			x = pkt.read_int();
			y = pkt.read_int();
			//Log.info("RECV obj move id "+id+" x "+x+" y "+y);
			ObjCache.move(id, x, y);
			break;
		case GAMESERVER_OBJ_DELETE :
			id = pkt.read_int(); 
			Log.info("RECV obj delete id "+id);
			ObjCache.remove(id);
			break;
		case GAMESERVER_MAP_DATA :
			x = pkt.read_int();
			y = pkt.read_int();
			// читаем координаты гридов вокруг игрока (список доступных гридов)
			count = pkt.read_int();
			Coord[] coords = new Coord[9];
			for (i = 0; i < count; i++) {
				coords[i] = new Coord(pkt.read_int(), pkt.read_int());
			}
			// данные грида в бинарном формате
			byte[] data = pkt.read_map_data();
			// обработчик
			if (data != null)
				MapCache.RecvMapData(coords, x, y, data);
			Log.info("RECV map data x "+x+" y "+y);
			break;
		case GAMESERVER_OBJ_LINE_MOVE :
			id = pkt.read_int();
			int speed = pkt.read_int();
			x = pkt.read_int();
			y = pkt.read_int();
			int vx = pkt.read_int();
			int vy = pkt.read_int();
			ObjCache.line_move_attr(id, x, y, vx, vy, speed);
			break;
		case GAMESERVER_SET_DRAWABLE : 
			id = pkt.read_int();
			name = pkt.read_string_ascii();
			//Log.info("RECV drawable id="+id+" name="+name);
			ObjCache.draw_attr(id, name);
			break;	
		case GAMESERVER_SET_LIGHT :
			id = pkt.read_int();
			int radius = pkt.read_int();
			int str = pkt.read_byte();
			ObjCache.light_attr(id, radius, (byte) str);
			break;
		case GAMESERVER_SET_OPENED :
			id = pkt.read_int();
			ObjCache.opened_attr(id);
			break;
		case GAMESERVER_CONTEXT_MENU :
			x = pkt.read_int();
			y = pkt.read_int();
			count = pkt.read_int();
			String[] items = new String[count];
			while (count > 0) {
				count--;
				items[count] = pkt.read_string_ascii();
			}
			if (items.length > 0) {
				Dialog.Show("dlg_context");
				dlg_Context.dlg.AddItems(x,y,items);
			} else {
				SEND_context_action("");
			}
			//Log.info("RECV GAMESERVER_CONTEXT_MENU "+items);
			break;
		case GAMESERVER_PROGRESS :
			count = pkt.read_int();
			dlg_Progress.SetProgress(count);
			//Log.info("RECV GAMESERVER_PROGRESS val="+count);
			break;
		case GAMESERVER_SERVER_SAY :
			int chan = pkt.read_int();
			String nick = "";
			String msg = "";
			int objid = 0;
			if (chan == 0) {
				objid = pkt.read_int();
				msg = pkt.read_string_utf();
			}
			if (chan == 1) {
				msg = pkt.read_string_utf();
			}
			GUI_Chat.RECV_Say(chan, objid, nick, msg);
			ObjCache.obj_say(chan, msg, objid);
			break;
		case GAMESERVER_ACTIONS_LIST :
			count = pkt.read_int();
			String parent;
			ActionsMenu.Clear();
			while (count > 0) {
				count--;
				parent = pkt.read_string_ascii();
				name = pkt.read_string_ascii();
				ActionsMenu.RECV_Action(parent, name);
				//Log.info("RECV action: "+name+" parent="+parent);
			}
			if (dlg_Game.dlg != null)
				dlg_Game.dlg.actions_panel.LoadFromActionsMenu();
			break;
		case GAMESERVER_CURSOR :
			String cursor = pkt.read_string_ascii();
			Log.info("RECV cursor: "+cursor);
			Cursor.setCursor(cursor);
			break;
		case GAMESERVER_GUI_ADD :
			int t = pkt.read_byte();
			GUI_Control pp = GUI.getInstance().normal;
			switch (t) {
				case 0: pp =GUI.getInstance().normal; break;
				case 1: pp =GUI.getInstance().popup; break;
				case 2: pp =GUI.getInstance().modal; break;
			}
			c = GUI.GetType(pkt.read_string_ascii()).create(pp);
			c.NetReadAdd(pkt);
			break;
		case GAMESERVER_GUI_REMOVE :
			id = pkt.read_int();
			c = GUI.getInstance().getRemoteControl(id);
			if (c != null) {
				c.Unlink();
			} else
				Log.info("remote control not found!");
			break;
		case GAMESERVER_GUI_UPDATE :
			id = pkt.read_int();
			c = GUI.getInstance().getRemoteControl(id);
			if (c != null) {
				Log.info("gui control update "+c.toString());
				c.NetReadUpdate(pkt);
			}
			else
				Main.GlobalError("remote control not found!");
			break;
		case GAMESERVER_PLACE_OBJECT:
			s = pkt.read_string_ascii();
			if (GUI.map != null) {
				GUI.map.set_place(s);
			}
			break;
		case GAMESERVER_SYSTEM_MSG :
			s = pkt.read_string_ascii();
			dlg_SysMsg.ShowSysMessage(s);
			break;
		case GAMESERVER_GAIN_EXP :
			objid = pkt.read_int();
			int combat = pkt.read_int();
			int industry = pkt.read_int();
			int nature = pkt.read_int();
			ObjCache.gain_exp(objid, combat, industry, nature);
			Log.info("exp gained"+combat+" "+industry+" "+nature);
			break;
		case GAMESERVER_FLY_TEXT :
			x = pkt.read_int();
			y = pkt.read_int();
			i = pkt.read_int();
			s = pkt.read_string_ascii();
			FlyText.Add(x, y, s, i);
			break;
		case GAMESERVER_BUFF_ADD :
			Player.BuffAdd(pkt);
			break;
		case GAMESERVER_BUFF_DELETE :
			Player.BuffDelete(pkt);
			break;
		case GAMESERVER_TARGET :
			Player.SetTarget(pkt);
			break;
		case GAMESERVER_REUSE_TIME :
			dlg_Progress.SetReuse(pkt);
			break;
		case GAMESERVER_SET_PARAM :
			ObjCache.SetParam(pkt);
			break;
		case GAMESERVER_CLIENT_SPEED : 
			Player.SetSpeed(pkt);
			break;
		case GAMESERVER_SET_PLAYER_PARAM :
			Player.SetParam(pkt);
			break;
			
		default :
			Log.info("Unhandled packet! id="+pkt.type);
		}
	}
	
	static public void SEND_map_click(int objid, int x, int y, int mx, int my, int btn, int mod) {
		Packet p = new Packet(GAMESERVER_MAP_CLICK);
		p.write_int(objid);
		p.write_int(x);
		p.write_int(y);
		p.write_int(mx); 
		p.write_int(my);
		p.write_byte((byte) btn);
		p.write_byte((byte) mod);
		p.Send(GameConnect);
	}
	
	static public void SEND_obj_click(int x, int y, int id, int btn) {
		Packet p = new Packet(GAMESERVER_OBJ_CLICK);
		p.write_int(x);
		p.write_int(y);
		p.write_int(id);
		p.write_byte((byte) btn);
		p.Send(GameConnect);
	}
	
	static public void SEND_context_action(String ac) {
		Packet p = new Packet(GAMESERVER_CONTEXT_ACTION);
		p.write_string_ascii(ac);
		p.Send(GameConnect);
	}
	
	static public void SEND_action(String ac) {
		Packet p = new Packet(GAMESERVER_ACTION);
		p.write_string_ascii(ac);
		p.Send(GameConnect);
	}
	
	static public void SEND_Chat(int c, String nick, String msg) {
		Packet p = new Packet(GAMESERVER_CLIENT_SAY);
		p.write_int(c);
		p.write_string_ascii(nick);
		p.write_string_utf(msg);
		p.Send(GameConnect); 
	}
	
	static public void SEND_gui_click(int x, int y, int mx, int my, int ctrl_id, int btn) {
		// x, y - выполняют разную функцию. в зависимости от типа контрола (относительные координаты мыши внутри контрола)
		// mx, my - абсолютные координаты мыши
		Packet p = new Packet(GAMESERVER_GUI_CLICK);
		p.write_int(x);
		p.write_int(y);
		p.write_int(mx);
		p.write_int(my);
		p.write_int(ctrl_id);
		p.write_byte((byte) btn);
		p.write_int(GUI.getInstance().mouse_pos.x);
		p.write_int(GUI.getInstance().mouse_pos.y);
		p.Send(GameConnect);
	}
	
	static public void SEND_gui_destroy(int id) {
		Packet p = new Packet(GAMESERVER_GUI_DESTROY);
		p.write_int(id);
		p.Send(GameConnect);
	}
	
	static public void SEND_target_reset() {
		Packet p = new Packet(GAMESERVER_TARGET_RESET);
		p.Send(GameConnect);
	}
	
	static public void SEND_speed(int speed) {
		Packet p = new Packet(GAMESERVER_SET_SPEED);
		p.write_int(speed);
		p.Send(GameConnect);
	}

    static public void SEND_bug_report(String subj, String text) {
        Packet p = new Packet(GAMESERVER_BUG_REPORT);
        p.write_string_utf(subj);
        p.write_string_utf(text);
        p.Send(GameConnect);

    }

	
	static public void NetError(String msg) {
		NetLogin.error_text = msg;
		Main.ReleaseAll();
	}
}
