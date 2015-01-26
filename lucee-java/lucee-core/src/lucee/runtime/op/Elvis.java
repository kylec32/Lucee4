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
package lucee.runtime.op;

import lucee.runtime.PageContext;
import lucee.runtime.interpreter.VariableInterpreter;
import lucee.runtime.type.Collection;
import lucee.runtime.type.KeyImpl;
import lucee.runtime.type.scope.Scope;
import lucee.runtime.util.VariableUtilImpl;

public class Elvis {
	
	/**
	 *  called by the Elvis operator from generated bytecode
	 * @param pc
	 * @param scope
	 * @param varNames
	 * @return
	 */
	public static boolean operate(PageContext pc , double scope,Collection.Key[] varNames) {
		return _operate(pc, scope, varNames,0); 
	}

	/**
	 *  called by the Elvis operator from generated bytecode
	 * @param pc
	 * @param scope
	 * @param varNames
	 * @return
	 */
	public static boolean operate(PageContext pc , double scope,String[] varNames) {
		return _operate(pc, scope, KeyImpl.toKeyArray(varNames),0);
	}
	
	/**
	 *  called by the Elvis operator from the interpreter
	 * @param pc
	 * @param scope
	 * @param varNames
	 * @return
	 */
	public static boolean operate(PageContext pc , String[] varNames) {
		int scope = VariableInterpreter.scopeString2Int(varNames[0]);
		return _operate(pc, scope, KeyImpl.toKeyArray(varNames), scope==Scope.SCOPE_UNDEFINED?0:1);
	}
	
	private static boolean _operate(PageContext pc , double scope,Collection.Key[] varNames, int startIndex) {
		Object defVal=null;
		try {
			Object coll =VariableInterpreter.scope(pc, (int)scope, false); 
			//Object coll =pc.scope((int)scope);
			VariableUtilImpl vu = ((VariableUtilImpl)pc.getVariableUtil());
			for(int i=startIndex;i<varNames.length;i++) {
				coll=vu.getCollection(pc,coll,varNames[i],defVal);
				if(coll==defVal)return false;
			}
		} catch (Throwable t) {
	        return false;
	    }
		return true; 
	}
}
