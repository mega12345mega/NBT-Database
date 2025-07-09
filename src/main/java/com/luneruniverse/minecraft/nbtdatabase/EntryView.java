package com.luneruniverse.minecraft.nbtdatabase;

public class EntryView {
	
	public enum Order {
		NAME("Name (A -> Z)", "LOWER(`name`)", false),
		DATA_VERSION("Data Version (Recent -> Old)", "`data_version`", true),
		AUTHOR_UUID("Author UUID (0 -> F)", "`author_uuid`", false),
		AUTHOR_USERNAME("Author Username (A -> Z)", "LOWER(`author_username`)", false),
		CREATED("Created (Recent -> Old)", "`created`", true),
		MODIFIED("Modified (Recent -> Old)", "`modified`", true);
		
		private final String toString;
		private final String column;
		private final boolean defaultDesc;
		
		private Order(String toString, String column, boolean defaultDesc) {
			this.toString = toString;
			this.column = column;
			this.defaultDesc = defaultDesc;
		}
		
		public String getColumn() {
			return column;
		}
		public boolean isDefaultDesc() {
			return defaultDesc;
		}
		
		@Override
		public String toString() {
			return toString;
		}
	}
	
	private Order order;
	private boolean reversedOrder;
	private int offset;
	
	public EntryView() {
		this.order = Order.CREATED;
	}
	public EntryView(Order order, boolean reversedOrder, int offset) {
		this.order = order;
		this.reversedOrder = reversedOrder;
		this.offset = offset;
	}
	
	public EntryView setOrder(Order order) {
		this.order = order;
		return this;
	}
	public EntryView setReversedOrder(boolean reversedOrder) {
		this.reversedOrder = reversedOrder;
		return this;
	}
	public EntryView setOffset(int offset) {
		this.offset = offset;
		return this;
	}
	
	public Order getOrder() {
		return order;
	}
	public boolean isReversedOrder() {
		return reversedOrder;
	}
	public int getOffset() {
		return offset;
	}
	
}
