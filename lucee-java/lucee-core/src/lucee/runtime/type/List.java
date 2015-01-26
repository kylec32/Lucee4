/**
 *
 * Copyright (c) 2014, the Railo Company Ltd. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 **/
package lucee.runtime.type;

import lucee.runtime.type.util.ListUtil;

/**
 * @deprecated BACKCOMP this class only exists for backward compatibility to code genrated for .ra files, DO NOT USE
 */
public class List {

	public static Array listToArrayRemoveEmpty(String list, String delimiter){
		return ListUtil.listToArrayRemoveEmpty(list, delimiter);
	}

	public static Array listToArrayRemoveEmpty(String list, char delimiter){
		return ListUtil.listToArrayRemoveEmpty(list, delimiter);
	}

	public static int listFindForSwitch(String list, String value, String delimiter){
		return ListUtil.listFindForSwitch(list, value, delimiter);
	}
	

}
