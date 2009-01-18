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

package jlibs.xml.sax.sniff;

import jlibs.xml.sax.sniff.events.DocumentOrder;
import jlibs.xml.sax.sniff.events.Event;
import jlibs.xml.sax.sniff.model.Node;
import jlibs.xml.sax.sniff.model.Position;
import jlibs.xml.sax.sniff.model.Predicate;
import jlibs.xml.sax.sniff.model.Root;
import jlibs.xml.sax.sniff.model.functions.Function;

import java.util.*;

/**
 * @author Santhosh Kumar T
 */
public class XPathResults implements Debuggable{
    static final RuntimeException STOP_PARSING = new RuntimeException();
    
    int minHits = -1;
    private Map<Node, TreeMap<Integer, String>> resultsMap = new HashMap<Node, TreeMap<Integer, String>>();
    private Map<Predicate, Map<Integer, String>> predicateResultsMap = new HashMap<Predicate, Map<Integer, String>>();

    private Root root;
    private DocumentOrder documentOrder;
    public XPathResults(Root root, DocumentOrder documentOrder, int minHits){
        this.root = root;
        this.documentOrder = documentOrder;
        this.minHits = minHits;
    }

    private Map<Predicate, PredicateResult> cachedMap = new HashMap<Predicate, PredicateResult>();

    class PredicateResult{
        public List<Node> nodes = new ArrayList<Node>();
        public List<Predicate> predicates = new ArrayList<Predicate>();

        public Map<Predicate, TreeMap<Integer, String>> predicateResultMap = new HashMap<Predicate, TreeMap<Integer, String>>();
        public TreeMap<Integer, String> resultStack = new TreeMap<Integer, String>();

        private Predicate predicate;
        public PredicateResult(Predicate predicate){
            this.predicate = predicate;
            nodes.addAll(predicate.nodes);
            predicates.addAll(predicate.predicates);

            for(Predicate p: predicate.memberOf)
                predicateResultMap.put(p, new TreeMap<Integer, String>());
        }

        public void addResult(String result){
            resultStack.put(documentOrder.get(), result);
            for(TreeMap<Integer, String> stack: predicateResultMap.values())
                stack.put(documentOrder.get(), result);
            
            if(Sniffer.debug)
                System.out.format("Cached Predicate Result %2d: %s ---> %s %n", resultStack.size(), predicate, result);
        }

        public void removeResult(Node node){
            resultStack.remove(resultStack.lastKey());
        }
        
        public void removeResult(Predicate p){
            TreeMap<Integer, String> map = predicateResultMap.get(p);
            map.remove(map.lastKey());
        }

        public Map.Entry<Integer, String> hit(Node node){
            nodes.remove(node);
            return getResult(node);
        }

        public Map.Entry<Integer, String> hit(Predicate predicate){
            predicates.remove(predicate);
            return getResult(predicate);
        }

        public Map.Entry<Integer, String> getResult(Node node){
            return canGiveResult() ? resultStack.lastEntry() : null;
        }

        public Map.Entry<Integer, String> getResult(Predicate p){
            if(canGiveResult()){
                TreeMap<Integer, String> stack = predicateResultMap.get(p);
                return stack==null ? null : stack.lastEntry();
            }else
                return null;
        }

        private boolean canGiveResult(){
            return nodes.size()==0 && predicates.size()==0 && !resultStack.isEmpty();
        }
    }

    /*-------------------------------------------------[ Hit ]---------------------------------------------------*/

    private PositionTracker positionTracker = new PositionTracker();

    public boolean hit(Context context, Event event, Node node){
        if(node instanceof Position){
            Position position = (Position)node;
            if(!positionTracker.hit(context, position))
                return false;
        }

        if(node.resultInteresed()){
            if(node instanceof Function){
                Function function = (Function)node;
                if(node.userGiven){ // functions have single result
                    if(function.singleHit()){
                        if(function.consumable(event)){
                            if(context.node!=node){
                                if(resultsMap.get(function)==null){
                                    addResult(function, function.evaluate(event, null));
                                    return true;
                                }else
                                    return false;
                            }else{
                                evaluateWithLastResult(function, event);
                                return true;
                            }
                        }else{
                            if(resultsMap.get(function)==null)
                                addResult(function, function.evaluate(event, null));
                        }
                    }else{
                        if(function.consumable(event)){
                            if(context.node!=node){
                                joinResults(function);
                                addResult(function, function.evaluate(event, null));
                            }else
                                evaluateWithLastResult(function, event);
                            
                            return true;
                        }else
                            evaluateWithLastResult(function, event);
                    }
                }
                return false;
            }

            if(node.userGiven)
                addResult(node, event.getResult());

            for(Predicate predicate: node.predicates()){
                PredicateResult predicateResult = cachedMap.get(predicate);
                if(predicateResult==null)
                    cachedMap.put(predicate, predicateResult=new PredicateResult(predicate));
                predicateResult.addResult(event.getResult());

                checkMembers(predicate);

//                Integer result = predicateResult.getResult();
//                if(result!=null){
//                    if(predicate.userGiven)
//                        addResult(predicate, result);
//                    hitMemberOf(predicate);
//                }
            }
            
            for(Predicate member: node.memberOf()){
                PredicateResult predicateResult = cachedMap.get(member);
                Map.Entry<Integer, String> result = predicateResult.hit(node);
                if(result!=null){
                    if(member.userGiven){
                        addResult(member, result);
                        predicateResult.removeResult(node);
                    }
                    hitMemberOf(member);
//                    int consumed = hitMemberOf(member);
//                    if(consumed>0)
//                        predicateResult.removeResult();
                }
            }
            
            for(Predicate predicate: node.predicates()){
                if(node.memberOf.contains(predicate))
                    return true;
                PredicateResult predicateResult = cachedMap.get(predicate);
                if(predicateResult!=null){
                    Map.Entry<Integer, String> result = predicateResult.getResult(node);
                    if(result!=null){
                        if(predicate.userGiven){
                            addResult(predicate, result);
                            predicateResult.removeResult(node);
                        }
                        hitMemberOf(predicate);
                    }
                }
            }
        }
        return true;
    }

    private int checkMembers(Predicate predicate){
        int consumed = 0;
        for(Predicate member: predicate.predicates){
            PredicateResult predicateResult = cachedMap.get(member);
            if(predicateResult!=null){
                consumed++;
                Map.Entry<Integer, String> result = predicateResult.getResult(predicate);
                if(result!=null){
                    if(predicate.userGiven)
                        cachedMap.get(predicate).hit(member);
                    checkMembers(member);
                }
            }
        }
        return consumed;
    }

    private int hitMemberOf(Predicate predicate){
        int consumed = 0;
        for(Predicate member: predicate.memberOf){
            PredicateResult predicateResult = cachedMap.get(member);
            if(predicateResult!=null){
                consumed++;
                Map.Entry<Integer, String> result = predicateResult.hit(predicate);
                if(member.userGiven){
                    Map.Entry<Integer, String> userResult = predicateResult.getResult((Node)null);
                    if(userResult!=null){
                        addResult(member, userResult);
                        predicateResult.removeResult((Node)null);
                    }
                }
                if(result!=null){
                    hitMemberOf(member);
                }
            }
        }
        return consumed;
    }

    private void hit(){
        if(minHits>0){
            minHits--;
            if(minHits==0)
                throw STOP_PARSING;
        }
    }

    void clearHitCounts(Context context){
        positionTracker.contextEnded(context);
    }

    void clearPredicateCache(int depth, Node node){
        for(Predicate predicate: node.predicates()){
            for(Node n: predicate.nodes){
                while(n!=null){
                    n = n.parent;
                    if(n==node){
                        clearCache(predicate);
                        break;
                    }
                }
            }
        }
    }

    private void clearCache(Predicate predicate){
        if(debug)
            System.out.println("cleared cache of: "+predicate);
        cachedMap.remove(predicate);
        for(Predicate member: predicate.memberOf)
            clearCache(member);
    }

    /*-------------------------------------------------[ Add Result ]---------------------------------------------------*/

    private void addResult(Node node, String result){
        TreeMap<Integer, String> map = resultsMap.get(node);
        if(map==null)
            resultsMap.put(node, map=new TreeMap<Integer, String>());
        map.put(documentOrder.get(), result);
        if(debug)
            System.out.format("Node-Hit %2d: %s ---> %s %n", map.size(), node, result);

        hit();
    }

    private void addResult(Predicate predicate, Map.Entry<Integer, String> result){
        Map<Integer, String> map = predicateResultsMap.get(predicate);
        if(map==null)
            predicateResultsMap.put(predicate, map=new HashMap<Integer, String>());
        map.put(result.getKey(), result.getValue());
        if(debug)
            System.out.format("Predicate-Hit %2d: %s ---> %s %n", map.size(), predicate, result);

        hit();
    }

    /*-------------------------------------------------[ Functions ]---------------------------------------------------*/
    
    private void evaluateWithLastResult(Function function, Event event){
        String lastResult = null;
        TreeMap<Integer, String> results = resultsMap.get(function);
        if(results!=null)
            lastResult = results.remove(results.lastKey());
        addResult(function, function.evaluate(event, lastResult));
    }

    private void joinResults(Function function){
        joinResults(function, resultsMap.get(function));
    }

    private void joinResults(Function function, TreeMap<Integer, String> results){
        if(results!=null && results.size()==2){
            String result1 = results.remove(results.firstKey());
            String result2 = results.lastEntry().getValue();
            String result = function.join(result1, result2);
            results.put(results.lastKey(), result);
        }
    }

    /*-------------------------------------------------[ Get Result ]---------------------------------------------------*/

    public List<String> getResult(XPath xpath){
        Map<Integer, String> results = new TreeMap<Integer, String>();

        for(Node node: xpath.nodes){
            node = node.locateIn(root);
            TreeMap<Integer, String> map = resultsMap.get(node);
            if(map!=null && !map.isEmpty()){
                if(node instanceof Function)
                    joinResults((Function) node, map);
                results.putAll(map);
            }else if(node instanceof Function)
                results.put(-1, ((Function)node).defaultResult());
        }

        for(Predicate predicate: xpath.predicates){
            predicate = predicate.locateIn(root);
            Map<Integer, String> map = predicateResultsMap.get(predicate);
            if(map!=null)
                results.putAll(map);
        }

        return new ArrayList<String>(results.values());
    }
}
