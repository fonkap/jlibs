/**
 * JLibs: Common Utilities for Java
 * Copyright (C) 2009  Santhosh Kumar T
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package jlibs.xml.sax.sniff.model;

import jlibs.core.graph.*;
import jlibs.core.graph.sequences.ConcatSequence;
import jlibs.core.graph.sequences.IterableSequence;
import jlibs.core.graph.walkers.PreorderWalker;
import jlibs.xml.sax.sniff.events.Event;
import org.jaxen.saxpath.Axis;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Santhosh Kumar T
 */
public abstract class Node implements Serializable{
    Root root;
    public Node parent;
    public Node constraintParent;

    public boolean hasAttibuteChild;

    public abstract boolean equivalent(Node node);
    
    /*-------------------------------------------------[ Children ]---------------------------------------------------*/
    
    private List<AxisNode> children = new ArrayList<AxisNode>();

    public Iterable<AxisNode> children(){
        return children;
    }

    @SuppressWarnings({"unchecked"})
    public <N extends AxisNode> N addChild(N axisNode){
        for(AxisNode child: children()){
            if(child.equivalent(axisNode))
                return (N)child;
        }

        children.add(axisNode);
        axisNode.parent = this;
        axisNode.root = root;

        if(axisNode.type==Axis.ATTRIBUTE)
            hasAttibuteChild = true;

        return axisNode;
    }

    /*-------------------------------------------------[ Constraints ]---------------------------------------------------*/

    private List<Node> constraints = new ArrayList<Node>();

    public Iterable<Node> constraints(){
        return constraints;
    }

    @SuppressWarnings({"unchecked"})
    public <N extends Node> N addConstraint(N node){
        for(Node constraint: constraints()){
            if(constraint.equivalent(node))
                return (N)constraint;
        }
        constraints.add(node);
        node.constraintParent = this;
        node.parent = this.parent;
        node.root = root;
        return node;
    }

    /*-------------------------------------------------[ Predicates ]---------------------------------------------------*/

    protected List<Predicate> predicates = new ArrayList<Predicate>();

    public Iterable<Predicate> predicates(){
        return predicates;
    }

    public Predicate addPredicate(Predicate predicate){
        for(Predicate p: predicates){
            if(p.equivalent(predicate))
                return p;
        }

        predicates.add(predicate);
        predicate.parentNode = this;
        for(Node member: predicate.nodes)
            member.memberOf.add(predicate);
        for(Predicate member: predicate.predicates)
            member.memberOf.add(predicate);
        return predicate;
    }

    /*-------------------------------------------------[ Member-Of ]---------------------------------------------------*/

    public List<Predicate> memberOf = new ArrayList<Predicate>();

    public Iterable<Predicate> memberOf(){
        return memberOf;
    }

    public Predicate addMemberOf(Predicate predicate){
        memberOf.add(predicate);
        return predicate;
    }

    /*-------------------------------------------------[ Matches ]---------------------------------------------------*/
    
    public boolean consumable(Event event){
        return false;
    }

    public boolean matches(Event event){
        return false;
    }
    
    /*-------------------------------------------------[ Requires ]---------------------------------------------------*/

    public boolean userGiven;
    public boolean resultInteresed(){
        return userGiven || predicates.size()>0 || memberOf.size()>0;
    }

    /*-------------------------------------------------[ Locate ]---------------------------------------------------*/

    @SuppressWarnings({"SuspiciousMethodCalls"})
    public Node locateIn(Root root){
//        root.print();
        if(this.root==root)
            return this;

        ArrayDeque<Integer> typeStack = new ArrayDeque<Integer>();
        ArrayDeque<Integer> indexStack = new ArrayDeque<Integer>();
        Node node = this;
        while(node!=null){
            if(node.constraintParent!=null){
                typeStack.push(2);
                indexStack.push(node.constraintParent.constraints.indexOf(node));
                node = node.constraintParent;
            }else if(node.parent!=null){
                typeStack.push(1);
                indexStack.push(node.parent.children.indexOf(node));
                node = node.parent;
            }else
                break;
        }

        node = root;
        while(!indexStack.isEmpty()){
            switch(typeStack.pop()){
                case 1:
                    node = node.children.get(indexStack.pop());
                    break;
                case 2:
                    node = node.constraints.get(indexStack.pop());
                    break;
            }
        }

        return node;
    }

    /*-------------------------------------------------[ Debug ]---------------------------------------------------*/
    
    public void print(){
        Navigator<Node> navigator = new Navigator<Node>(){
            @Override
            public Sequence<? extends Node> children(Node elem){
                return new ConcatSequence<Node>(new IterableSequence<Node>(elem.constraints), new IterableSequence<AxisNode>(elem.children));
            }
        };
        Walker<Node> walker = new PreorderWalker<Node>(this, navigator);
        WalkerUtil.print(walker, new Visitor<Node, String>(){
            @Override
            public String visit(Node elem){
                String str = elem.toString();
                if(elem.userGiven)
                    str += " --> userGiven";
                if(elem.predicates.size()>0){
                    str += " --> ";
                    for(Predicate predicate: elem.predicates)
                        str += predicate+" ";
                }
                if(elem.memberOf.size()>0){
                    str += " ==>";
                    for(Predicate predicate: elem.memberOf)
                        str += predicate+" ";
                }
                return str;
            }
        });
    }
}
