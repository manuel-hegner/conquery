package com.bakdata.conquery.models.query.queryplan;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.specific.Leaf;

import lombok.Getter;

public abstract class QPChainNode extends QPNode {
	
	@Getter
	private QPNode child;
	
	public QPChainNode() {
		this(null);
	}
	
	public QPChainNode(QPNode child) {
		setChild(child);
	}
	
	public void setChild(QPNode child) {
		if(child == null)
			this.child = new Leaf();
		else
			this.child = child;
	}
	
	@Override
	public void init(Entity entity) {
		super.init(entity);
		child.init(entity);
	}
	
	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		super.nextTable(ctx, currentTable);
		child.nextTable(ctx, currentTable);
	}
	
	@Override
	public void nextBlock(Bucket bucket) {
		child.nextBlock(bucket);
	}
	
	@Override
	public List<QPNode> getChildren() {
		return Collections.singletonList(child);
	}
	
	
	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		child.collectRequiredTables(requiredTables);
	}
	
	@Override
	public String toString() {
		return super.toString()+"[child = "+child+"]";
	}
	
	@Override
	public boolean isContained() {
		return child.isContained();
	}
	
	@Override
	public boolean isOfInterest(Bucket bucket) {
		return child.isOfInterest(bucket);
	}
}
