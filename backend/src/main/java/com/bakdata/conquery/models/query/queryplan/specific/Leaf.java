package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

public class Leaf extends QPNode {

	private boolean triggered = false;
	
	@Override
	public QPNode doClone(CloneContext ctx) {
		return new Leaf();
	}
	
	@Override
	public void nextEvent(Bucket bucket, int event) {
		triggered = true;
	}

	@Override
	public boolean isContained() {
		return triggered;
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		return true;
	}
}
