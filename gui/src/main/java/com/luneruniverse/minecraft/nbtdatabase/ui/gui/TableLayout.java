package com.luneruniverse.minecraft.nbtdatabase.ui.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.io.Serializable;
import java.util.Arrays;
import java.util.function.Function;

import javax.swing.SizeRequirements;

/**
 * Similar to {@link GridLayout}, but the columns and rows don't have to have the same size
 */
@SuppressWarnings("serial")
public class TableLayout implements LayoutManager, Serializable {
	
	public static TableLayout ofRows(int rows, int hgap, int vgap) {
		return new TableLayout(rows, false, hgap, vgap);
	}
	public static TableLayout ofRows(int rows, int gap) {
		return ofRows(rows, gap, gap);
	}
	public static TableLayout ofRows(int rows) {
		return ofRows(rows, 0, 0);
	}
	
	public static TableLayout ofColumns(int columns, int hgap, int vgap) {
		return new TableLayout(columns, true, hgap, vgap);
	}
	public static TableLayout ofColumns(int columns, int gap) {
		return ofColumns(columns, gap, gap);
	}
	public static TableLayout ofColumns(int columns) {
		return ofColumns(columns, 0, 0);
	}
	
	private final int groups;
	private final boolean verticalGroups;
	private final int hgap;
	private final int vgap;
	private final SizeRequirements hgapRequirements;
	private final SizeRequirements vgapRequirements;
	
	private TableLayout(int groups, boolean verticalGroups, int hgap, int vgap) {
		this.groups = groups;
		this.verticalGroups = verticalGroups;
		this.hgap = hgap;
		this.vgap = vgap;
		this.hgapRequirements = new SizeRequirements(hgap, hgap, hgap, 0);
		this.vgapRequirements = new SizeRequirements(vgap, vgap, vgap, 0);
	}
	
	@Override
	public void addLayoutComponent(String name, Component component) {}
	
	@Override
	public void removeLayoutComponent(Component component) {}
	
	@Override
	public Dimension preferredLayoutSize(Container parent) {
		return layoutSize(parent, Component::getPreferredSize);
	}
	
	@Override
	public Dimension minimumLayoutSize(Container parent) {
		return layoutSize(parent, Component::getMinimumSize);
	}
	
	private Dimension layoutSize(Container parent, Function<Component, Dimension> componentSize) {
		synchronized (parent.getTreeLock()) {
			RowAndColumnCount counts = calculateRowAndColumnCount(parent);
			RowAndColumnSizes sizes = calculateRowAndColumnSizes(parent, counts, componentSize);
			Insets insets = parent.getInsets();
			
			return new Dimension(
					insets.left + insets.right + Arrays.stream(sizes.columnWidths).sum() + Math.max(counts.columns - 1, 0) * hgap,
					insets.top + insets.bottom + Arrays.stream(sizes.rowHeights).sum() + Math.max(counts.rows - 1, 0) * vgap);
		}
	}
	
	@Override
	public void layoutContainer(Container parent) {
		synchronized (parent.getTreeLock()) {
			RowAndColumnCount counts = calculateRowAndColumnCount(parent);
			RowAndColumnSizes minimumSizes = calculateRowAndColumnSizes(parent, counts, Component::getMinimumSize);
			RowAndColumnSizes preferredSizes = calculateRowAndColumnSizes(parent, counts, Component::getPreferredSize);
			RowAndColumnSizes maximumSizes = calculateRowAndColumnSizes(parent, counts, Component::getMaximumSize);
			
			SizeRequirements[] rowsAndGaps = new SizeRequirements[counts.rows + Math.max(counts.rows - 1, 0)];
			SizeRequirements[] columnsAndGaps = new SizeRequirements[counts.columns + Math.max(counts.columns - 1, 0)];
			for (int i = 0; i < rowsAndGaps.length; i++) {
				if (i % 2 == 0) {
					rowsAndGaps[i] = new SizeRequirements(
							minimumSizes.rowHeights[i / 2], preferredSizes.rowHeights[i / 2], maximumSizes.rowHeights[i / 2], 0);
				} else {
					rowsAndGaps[i] = vgapRequirements;
				}
			}
			for (int i = 0; i < columnsAndGaps.length; i++) {
				if (i % 2 == 0) {
					columnsAndGaps[i] = new SizeRequirements(
							minimumSizes.columnWidths[i / 2], preferredSizes.columnWidths[i / 2], maximumSizes.columnWidths[i / 2], 0);
				} else {
					columnsAndGaps[i] = hgapRequirements;
				}
			}
			
			int[] rowOffsets = new int[rowsAndGaps.length];
			int[] rowSpans = new int[rowsAndGaps.length];
			int[] columnOffsets = new int[columnsAndGaps.length];
			int[] columnSpans = new int[columnsAndGaps.length];
			
			Insets insets = parent.getInsets();
			SizeRequirements.calculateTiledPositions(parent.getWidth() - insets.left - insets.right,
					SizeRequirements.getTiledSizeRequirements(columnsAndGaps), columnsAndGaps, columnOffsets, columnSpans);
			SizeRequirements.calculateTiledPositions(parent.getHeight() - insets.top - insets.bottom,
					SizeRequirements.getTiledSizeRequirements(rowsAndGaps), rowsAndGaps, rowOffsets, rowSpans);
			
			int[] xPositions = new int[counts.columns];
			int[] yPositions = new int[counts.rows];
			for (int i = 0; i < xPositions.length; i++)
				xPositions[i] = insets.left + columnOffsets[i * 2];
			for (int i = 0; i < yPositions.length; i++)
				yPositions[i] = insets.top + rowOffsets[i * 2];
			
			for (int i = 0; i < parent.getComponentCount(); i++) {
				int x = i % counts.columns;
				int y = i / counts.columns;
				parent.getComponent(i).setBounds(xPositions[x], yPositions[y], columnSpans[x * 2], rowSpans[y * 2]);
			}
		}
	}
	
	private static class RowAndColumnCount {
		public final int rows;
		public final int columns;
		public RowAndColumnCount(int rows, int columns) {
			this.rows = rows;
			this.columns = columns;
		}
	}
	private RowAndColumnCount calculateRowAndColumnCount(Container parent) {
		int rows;
		int columns;
		if (verticalGroups) {
			rows = (int) Math.ceil((double) parent.getComponentCount() / groups);
			columns = groups;
		} else {
			rows = groups;
			columns = (int) Math.ceil((double) parent.getComponentCount() / groups);
		}
		return new RowAndColumnCount(rows, columns);
	}
	
	private static class RowAndColumnSizes {
		public final int[] rowHeights;
		public final int[] columnWidths;
		public RowAndColumnSizes(int[] rowHeights, int[] columnWidths) {
			this.rowHeights = rowHeights;
			this.columnWidths = columnWidths;
		}
	}
	private RowAndColumnSizes calculateRowAndColumnSizes(Container parent, RowAndColumnCount counts, Function<Component, Dimension> componentSize) {
		int[] rowHeights = new int[counts.rows];
		int[] columnWidths = new int[counts.columns];
		for (int i = 0; i < parent.getComponentCount(); i++) {
			int x = i % counts.columns;
			int y = i / counts.columns;
			Dimension size = componentSize.apply(parent.getComponent(i));
			if (columnWidths[x] < size.width)
				columnWidths[x] = size.width;
			if (rowHeights[y] < size.height)
				rowHeights[y] = size.height;
		}
		return new RowAndColumnSizes(rowHeights, columnWidths);
	}
	
}
