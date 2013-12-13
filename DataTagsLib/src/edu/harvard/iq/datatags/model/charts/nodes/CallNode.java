package edu.harvard.iq.datatags.model.charts.nodes;

import edu.harvard.iq.datatags.runtime.exceptions.DataTagsRuntimeException;

/**
 * A node that calls another node, possibly in another chart as well.
 * @author michael
 */
public class CallNode extends ThroughNode {
	
	private String calleeChartId;
	private String calleeNodeId;
	
	public CallNode(String id) {
		super(id);
	}

	public CallNode(String id, String calleeChartId, String calleeNodeId ) {
		super(id);
		this.calleeChartId = calleeChartId;
		this.calleeNodeId = calleeNodeId;
	}
	
	@Override
	public <R> R accept(Visitor<R> vr) throws DataTagsRuntimeException {
		return vr.visitCallNode(this);
	}
	
	public String getCalleeChartId() {
		return calleeChartId;
	}

	public String setCalleeChartId(String calleeChartId) {
		this.calleeChartId = calleeChartId;
		return calleeChartId;
	}	

	public String getCalleeNodeId() {
		return calleeNodeId;
	}

	public String setCalleeNodeId(String calleeId) {
		this.calleeNodeId = calleeId;
		return calleeId;
	}
	
}