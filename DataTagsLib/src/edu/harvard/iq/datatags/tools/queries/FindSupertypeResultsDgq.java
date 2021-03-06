package edu.harvard.iq.datatags.tools.queries;

import edu.harvard.iq.datatags.model.PolicyModel;
import edu.harvard.iq.datatags.model.graphs.Answer;
import edu.harvard.iq.datatags.model.graphs.ConsiderOption;
import edu.harvard.iq.datatags.model.graphs.nodes.AskNode;
import edu.harvard.iq.datatags.model.graphs.nodes.CallNode;
import edu.harvard.iq.datatags.model.graphs.nodes.ConsiderNode;
import edu.harvard.iq.datatags.model.graphs.nodes.EndNode;
import edu.harvard.iq.datatags.model.graphs.nodes.Node;
import edu.harvard.iq.datatags.model.graphs.nodes.RejectNode;
import edu.harvard.iq.datatags.model.graphs.nodes.SectionNode;
import edu.harvard.iq.datatags.model.graphs.nodes.SetNode;
import edu.harvard.iq.datatags.model.graphs.nodes.ThroughNode;
import edu.harvard.iq.datatags.model.graphs.nodes.ToDoNode;
import edu.harvard.iq.datatags.model.values.CompoundValue;
import edu.harvard.iq.datatags.runtime.exceptions.DataTagsRuntimeException;
import static edu.harvard.iq.datatags.util.CollectionHelper.C;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Gets a decision graph and a {@link CompoundValue}, and returns all the runs
 * that result in this value, or in a value that is a superset of this value.
 * 
 * The "Dgq" in the class name stand for "Decision Graph Query". We'll have more of those.
 * @author michael
 */
public class FindSupertypeResultsDgq implements DecisionGraphQuery {
    private final PolicyModel subject;
    private final CompoundValue value;
    private GraphTraverser graphTraverser;
    
    public FindSupertypeResultsDgq( PolicyModel aPolicyModel, CompoundValue aValue) {
        subject = aPolicyModel;
        value = aValue;
    }
    
    public void get( DecisionGraphQuery.Listener aListener ) {
        graphTraverser = new GraphTraverser(aListener);
        aListener.started(this);
        subject.getDecisionGraph().getStart().accept(graphTraverser);
        aListener.done(this);
    }

    @Override
    public RunTrace getCurrentTrace() {
        return new RunTrace(graphTraverser.currentTrace, graphTraverser.currentAnswers, graphTraverser.valueStack.peek());
    }
    
    class GraphTraverser extends Node.VoidVisitor {
        
        final DecisionGraphQuery.Listener listener;
        
        LinkedList<Node> currentTrace = new LinkedList<>();
        LinkedList<ThroughNode> nodeStack = new LinkedList<>();
        LinkedList<Answer> currentAnswers = new LinkedList<>();
        Deque<CompoundValue> valueStack = new LinkedList<>();
        
        public GraphTraverser( DecisionGraphQuery.Listener aListener ) {
            listener = aListener;
            valueStack.push( subject.getSpaceRoot().createInstance() );
        }
        
        @Override
        public void visitImpl(AskNode nd) throws DataTagsRuntimeException {
            currentTrace.addLast( nd );
            nd.getAnswers().forEach( ans -> {
                currentAnswers.addLast(ans);
                // process answer nodes
                nd.getNodeFor(ans).accept(this);
                
                currentAnswers.removeLast();
            });
            currentTrace.removeLast();
        }
        
        @Override
        public void visitImpl(ConsiderNode nd) throws DataTagsRuntimeException {
            currentTrace.addLast( nd );
            boolean matchFound = false;
            for (ConsiderOption ans : nd.getAnswers()) {
                CompoundValue answer = ans.getValue();
                if (valueStack.peek().isSupersetOf(answer)) {
                    matchFound = true;
                    nd.getNodeFor(ans).accept(this);
                }
            }
            if ( ! matchFound ) {
                nd.getElseNode();
            }
            
            currentTrace.removeLast();
        }
        
        @Override
        public void visitImpl(SetNode nd) throws DataTagsRuntimeException {
            currentTrace.addLast(nd);
            valueStack.push( valueStack.peek().composeWith(nd.getTags()) );
            
            //Value inference
            // Process inferred values (ValueInference)
            CompoundValue previousValue;
            CompoundValue inferredValue = valueStack.peek();
            
            if ( ! subject.getValueInferrers().isEmpty() ) {
                do {
                    previousValue = inferredValue;
                    CompoundValue infCapture = inferredValue; // passing to lambda, has to be effectively final.
                    inferredValue = subject.getValueInferrers().stream().map( vi -> vi.apply(infCapture) ).collect( C.compose(previousValue.getSlot()));                
                } while ( !inferredValue.equals(previousValue) );

                if ( ! inferredValue.equals(valueStack.peek()) ) {
                    valueStack.pop();
                    valueStack.push(inferredValue);
                }
            }
            
            // go forward
            nd.getNextNode().accept(this);
            
            // came back, clean up.
            valueStack.pop();
            currentTrace.removeLast();
        }

        @Override
        public void visitImpl(RejectNode nd) throws DataTagsRuntimeException {
            // Do nothing - no tags will be generated, so no matching will be done
        }

        @Override
        public void visitImpl(CallNode nd) throws DataTagsRuntimeException {
            currentTrace.addLast(nd);
            nodeStack.push(nd);
            if (currentTrace.contains(nd.getCalleeNode())){
                listener.loopDetected(FindSupertypeResultsDgq.this);
            } else {
                nd.getCalleeNode().accept(this);
            }
            nodeStack.pop();
            currentTrace.removeLast();
            
            
        }

        @Override
        public void visitImpl(ToDoNode nd) throws DataTagsRuntimeException {
            currentTrace.addLast( nd );
            nd.getNextNode().accept( this );
            currentTrace.removeLast();
        }

        @Override
        public void visitImpl(EndNode nd) throws DataTagsRuntimeException {
            // either pop the call stack, or end the run and compare the result.
            currentTrace.addLast( nd );
            
            if ( nodeStack.isEmpty() ) {
                // compare, possibly store trace
                if ( valueStack.peek().isSupersetOf(value) ) {
                    // found!
                    listener.matchFound(FindSupertypeResultsDgq.this);
                } else {
                    listener.nonMatchFound(FindSupertypeResultsDgq.this);
                }
            } else {
                ThroughNode lastCall = nodeStack.pop();
                lastCall.getNextNode().accept(this);
                nodeStack.push(lastCall);
                
            }
            currentTrace.removeLast();        
        }
        
        @Override
        public void visitImpl(SectionNode nd) throws DataTagsRuntimeException{
            currentTrace.addLast( nd );
            nodeStack.push(nd);
            nd.getStartNode().accept(this);
            nodeStack.pop();
            currentTrace.removeLast();
        }
    }
    
}
