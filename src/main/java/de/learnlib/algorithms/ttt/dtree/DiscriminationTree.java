package de.learnlib.algorithms.ttt.dtree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.automatalib.graphs.abstractimpl.AbstractGraph;
import net.automatalib.graphs.concepts.NodeIDs;
import net.automatalib.graphs.dot.DOTPlottableGraph;
import net.automatalib.graphs.dot.DefaultDOTHelper;
import net.automatalib.graphs.dot.GraphDOTHelper;
import net.automatalib.words.Word;
import de.learnlib.algorithms.ttt.hypothesis.HypothesisState;
import de.learnlib.algorithms.ttt.stree.STNode;
import de.learnlib.api.MembershipOracle;
import de.learnlib.oracles.MQUtil;

public class DiscriminationTree<I,O,SP,TP> extends AbstractGraph<DTNode<I,O,SP,TP>,Map.Entry<O,DTNode<I,O,SP,TP>>>
		implements DOTPlottableGraph<DTNode<I,O,SP,TP>,Map.Entry<O,DTNode<I,O,SP,TP>>>, NodeIDs<DTNode<I,O,SP,TP>> {

	private final DTNode<I,O,SP,TP> root;
	private final List<DTNode<I,O,SP,TP>> nodes = new ArrayList<DTNode<I,O,SP,TP>>();
	
	private DTNode<I,O,SP,TP> createLeaf(HypothesisState<I,O,SP,TP> state) {
		DTNode<I,O,SP,TP> node = new DTNode<>(nodes.size(), state);
		nodes.add(node);
		return node;
	}
	
	public DTNode<I,O,SP,TP> getRoot() {
		return root;
	}
	
	private DTNode<I,O,SP,TP> createInner(STNode<I> discriminator) {
		DTNode<I,O,SP,TP> node = new DTNode<>(nodes.size(), discriminator);
		nodes.add(node);
		return node;
	}
	
	public DiscriminationTree(HypothesisState<I,O,SP,TP> state) {
		this.root = createLeaf(state);
	}
	
	public DiscriminationTree(STNode<I> discriminator) {
		this.root = createInner(discriminator);
	}
	
	
	public void split(DTNode<I,O,SP,TP> leaf, STNode<I> discriminator, O oldOutcome, HypothesisState<I,O,SP,TP> newState, O newOutcome) {
		HypothesisState<I,O,SP,TP> oldState = leaf.makeInner(discriminator);
		leaf.addChild(oldOutcome, createLeaf(oldState));
		leaf.addChild(newOutcome, createLeaf(newState));
	}
	
	
	public DTNode<I,O,SP,TP> sift(Word<I> word, MembershipOracle<I, O> oracle) {
		return sift(root, word, oracle);
	}
	
	public DTNode<I,O,SP,TP> sift(DTNode<I,O,SP,TP> start, Word<I> word, MembershipOracle<I,O> oracle) {
		DTNode<I,O,SP,TP> curr = start;
		
		while(!curr.isLeaf()) {
			O outcome = MQUtil.query(oracle, word, curr.getDiscriminator().getSuffix());
			DTNode<I,O,SP,TP> child = curr.getChild(outcome);
			
			if(child == null) {
				child = createLeaf(null);
				curr.addChild(outcome, child);
				return child;
			}
			
			curr = child;
		}
		
		return curr;
	}

	@Override
	public Collection<DTNode<I, O, SP, TP>> getNodes() {
		return Collections.unmodifiableCollection(nodes);
	}

	@Override
	public NodeIDs<DTNode<I, O, SP, TP>> nodeIDs() {
		return this;
	}

	@Override
	public Collection<Entry<O, DTNode<I, O, SP, TP>>> getOutgoingEdges(
			DTNode<I, O, SP, TP> node) {
		Map<O,DTNode<I,O,SP,TP>> map = node.getChildMap();
		if(map == null)
			return Collections.emptySet();
		return map.entrySet();
	}

	@Override
	public DTNode<I, O, SP, TP> getTarget(Entry<O, DTNode<I, O, SP, TP>> edge) {
		return edge.getValue();
	}

	@Override
	public DTNode<I, O, SP, TP> getNode(int id) {
		return nodes.get(id);
	}

	@Override
	public int getNodeId(DTNode<I, O, SP, TP> node) {
		return node.getId();
	}

	@Override
	public GraphDOTHelper<DTNode<I, O, SP, TP>, Entry<O, DTNode<I, O, SP, TP>>> getGraphDOTHelper() {
		return new DefaultDOTHelper<DTNode<I,O,SP,TP>,Entry<O,DTNode<I,O,SP,TP>>>() {

			/* (non-Javadoc)
			 * @see net.automatalib.graphs.dot.DefaultDOTHelper#getNodeProperties(java.lang.Object, java.util.Map)
			 */
			@Override
			public boolean getNodeProperties(DTNode<I, O, SP, TP> node,
					Map<String, String> properties) {
				if(!super.getNodeProperties(node, properties))
					return false;
				STNode<I> d = node.getDiscriminator();
				if(d == null) {
					properties.put(SHAPE, "box");
					properties.put(LABEL, node.getHypothesisState().toString());
				}
				else {
					properties.put(LABEL, node.getDiscriminator().getSuffix().toString());
				}
				return true;
			}

			/* (non-Javadoc)
			 * @see net.automatalib.graphs.dot.DefaultDOTHelper#getEdgeProperties(java.lang.Object, java.lang.Object, java.lang.Object, java.util.Map)
			 */
			@Override
			public boolean getEdgeProperties(DTNode<I, O, SP, TP> src,
					Entry<O, DTNode<I, O, SP, TP>> edge, DTNode<I, O, SP, TP> tgt,
					Map<String, String> properties) {
				if(!super.getEdgeProperties(src, edge, tgt, properties))
					return false;
				properties.put(LABEL, String.valueOf(edge.getKey()));
				return true;
			}
		};
	}

}
