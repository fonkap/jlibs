/**
 * JLibs: Common Utilities for Java
 * Copyright (C) 2009  Santhosh Kumar T <santhosh.tekuri@gmail.com>
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
 */

package jlibs.nblr.matchers;

import jlibs.xml.sax.XMLDocument;
import org.xml.sax.SAXException;

/**
 * @author Santhosh Kumar T
 */
public class And extends Matcher{
    public Matcher operands[];

    public And(Matcher... operands){
        this.operands = operands;
    }

    @Override
    public boolean matches(char ch){
        for(Matcher operand: operands){
            if(!operand.matches(ch))
                return false;
        }
        return true;
    }

    @Override
    public String javaCode(String variable){
        StringBuilder buff = new StringBuilder();
        for(Matcher operand: operands){
            if(buff.length()>0)
                buff.append(" && ");
            buff.append('(').append(operand._javaCode(variable)).append(')');
        }
        return buff.toString();
    }

    @Override
    public String toString(){
        StringBuffer buff = new StringBuffer();
        for(Matcher operand: operands){
            String msg = operand._toString();
            if(buff.length()==0)
                msg = msg.substring(1, msg.length()-1);
            else
                buff.append("&&");
            buff.append(msg);
        }
        return '['+buff.toString()+']';
    }

    /*-------------------------------------------------[ SAXProducer ]---------------------------------------------------*/

    @Override
    protected void addBody(XMLDocument xml) throws SAXException{
        for(Matcher operand: operands)
            operand.serializeTo(xml);
    }
}
