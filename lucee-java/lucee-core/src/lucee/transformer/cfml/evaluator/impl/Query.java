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
package lucee.transformer.cfml.evaluator.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lucee.runtime.functions.list.ListQualify;
import lucee.runtime.functions.other.PreserveSingleQuotes;
import lucee.runtime.functions.other.QuotedValueList;
import lucee.runtime.functions.query.ValueList;
import lucee.transformer.bytecode.Body;
import lucee.transformer.bytecode.Literal;
import lucee.transformer.bytecode.Statement;
import lucee.transformer.bytecode.cast.CastOther;
import lucee.transformer.bytecode.cast.CastString;
import lucee.transformer.bytecode.expression.Expression;
import lucee.transformer.bytecode.expression.var.Argument;
import lucee.transformer.bytecode.expression.var.BIF;
import lucee.transformer.bytecode.expression.var.Member;
import lucee.transformer.bytecode.expression.var.UDF;
import lucee.transformer.bytecode.expression.var.Variable;
import lucee.transformer.bytecode.literal.LitBoolean;
import lucee.transformer.bytecode.literal.LitString;
import lucee.transformer.bytecode.statement.PrintOut;
import lucee.transformer.bytecode.statement.tag.Tag;
import lucee.transformer.cfml.evaluator.EvaluatorException;
import lucee.transformer.cfml.evaluator.EvaluatorSupport;


/**
 * sign print outs for preserver
*/
public final class Query extends EvaluatorSupport {

	/**
	 * @see lucee.transformer.cfml.evaluator.EvaluatorSupport#evaluate(org.w3c.dom.Element)
	 */
	public void evaluate(Tag tag) throws EvaluatorException { 
		translateChildren(tag.getBody().getStatements().iterator());
	}
	
	private void translateChildren(Iterator it) {
		Statement stat;
		 
		while(it.hasNext()) {
			stat=(Statement) it.next();
			if(stat instanceof PrintOut) {
				PrintOut printOut = ((PrintOut)stat);
				Expression e = printOut.getExpr();
				if(!(e instanceof Literal)) {
					Expression expr=removeCastString(e);
					
					if(expr instanceof Variable) {
						// do not preserve BIF PreserveSingleQuotes return value
						Member member = ((Variable)expr).getFirstMember();
						if(member instanceof BIF) {
							BIF bif=(BIF) member;

							if(bif.getClazz().getName().equals(PreserveSingleQuotes.class.getName())) {
								printOut.setExpr(bif.getArguments()[0].getValue());
								continue;
							}
							else if(bif.getClazz().getName().equals(ListQualify.class.getName())) {
								Argument[] args = bif.getArguments();
								List<Argument> arr=new ArrayList<Argument>();
								
								// first get existing arguments
								arr.add(args[0]);
								arr.add(args[1]);
								if(args.length>=3)arr.add(args[2]);
								else arr.add(new Argument(LitString.toExprString(","),"string"));
								if(args.length>=4)arr.add(args[3]);
								else arr.add(new Argument(LitString.toExprString("all"),"string"));
								if(args.length>=5)arr.add(args[4]);
								else arr.add(new Argument(LitBoolean.toExprBoolean(false),"boolean"));
								
								
								// PSQ-BIF DO NOT REMOVE THIS COMMENT
								arr.add(new Argument(LitBoolean.toExprBoolean(true),"boolean"));
								bif.setArguments(arr.toArray(new Argument[arr.size()]));
								continue;
							}
							else if(
								bif.getClazz().getName().equals(QuotedValueList.class.getName()) ||
								bif.getClazz().getName().equals(ValueList.class.getName())
								) {
								//printOut.setPreserveSingleQuote(false);
								continue;
							}
						}
						
						// do not preserve UDF return value
						member= ((Variable)expr).getLastMember();
						if(member instanceof UDF) continue;
					}
					printOut.setCheckPSQ(true);
					if(e!=expr)printOut.setExpr(expr);
				}
			}
			else if(stat instanceof Tag){
				Body b=((Tag)stat).getBody();
				if(b!=null) 
					translateChildren(b.getStatements().iterator());
			}
			else if(stat instanceof Body){
				translateChildren(((Body)stat).getStatements().iterator());
			}
		}
	}

	private Expression removeCastString(Expression expr) {
		while(true) {
			if(expr instanceof CastString){
				expr=((CastString)expr).getExpr();
				
			}
			else if(
					expr instanceof CastOther && 
					(
							((CastOther) expr).getType().equalsIgnoreCase("String") || 
							((CastOther) expr).getType().equalsIgnoreCase("java.lang.String")
					)
				){
					expr=((CastOther) expr).getExpr();
			}
			else break;
		}
		return expr;
	}
}




