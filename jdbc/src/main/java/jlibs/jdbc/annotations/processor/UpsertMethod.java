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

package jlibs.jdbc.annotations.processor;

import jlibs.core.annotation.processing.AnnotationError;
import jlibs.core.annotation.processing.Printer;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import java.util.ArrayList;
import java.util.List;

import static jlibs.core.annotation.processing.Printer.MINUS;
import static jlibs.core.annotation.processing.Printer.PLUS;

/**
 * @author Santhosh Kumar T
 */
public class UpsertMethod extends DMLMethod{
    protected UpsertMethod(Printer printer, ExecutableElement method, AnnotationMirror mirror, Columns columns){
        super(printer, method, mirror, columns);
        if(method.getReturnType().getKind()!= TypeKind.VOID)
            throw new AnnotationError("method with @Upsert annotation should return void");
    }

    @Override
    protected String[] code(){
        if(method.getParameters().size()==0)
            throw new AnnotationError(method, "method with @Upsert annotation should take atleast one argument");

        InsertMethod insertMethod = new InsertMethod(printer, method, mirror, columns){
            @Override
            protected String methodName(){
                return "insert";
            }
        };
        String insertCode = insertMethod.queryMethod(insertMethod.defaultSQL(UpdateMethod.SET_WHERE_VISITOR));

        UpdateMethod updateMethod = new UpdateMethod(printer, method, mirror, columns){
            @Override
            protected String methodName(){
                return "update";
            }
        };
        String updateCode = updateMethod.queryMethod(updateMethod.defaultSQL());

        List<String> code = new ArrayList<String>();
        code.add("int count = "+updateCode+';');
        if(method.getReturnType().getKind()== TypeKind.VOID){
            code.add("if(count==0)");
            code.add(PLUS);
            code.add(insertCode+';');
            code.add(MINUS);
        }else
            code.add("return count==0 ? "+insertCode+" : count;");

        return code.toArray(new String[code.size()]);
    }
}