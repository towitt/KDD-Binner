package org.knime.base.node.preproc.binner.lucs_kdd;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the model implementation of KDD. LUCS-KDD DN (Discretisation/
 * Normalisation)
 *
 * @author Tobias Witt, University of Konstanz
 */
public class LucsKddDnNodeModel extends NodeModel {

	// the logger instance
	private static final NodeLogger LOGGER = NodeLogger.getLogger(LucsKddDnNodeModel.class);

	/**
	 * the settings key which is used to retrieve and store the settings (from
	 * the dialog or from a settings file) (package visibility to be usable from
	 * the dialog).
	 */

	// class attribute
	private static final SettingsModelString m_class = createClassColModel();

	protected static SettingsModelString createClassColModel() {
		return new SettingsModelString("Class Column", "");
	}

	// desired number of divisions
	private static SettingsModelIntegerBounded m_divisions = createNumDivisionsModel();

	protected static SettingsModelIntegerBounded createNumDivisionsModel() {
		return new SettingsModelIntegerBounded("Divisions", 5, 1, Integer.MAX_VALUE);
	}

	// select variables
	private static final SettingsModelColumnFilter2 m_features = createIncludedFeaturesModel();

	@SuppressWarnings("unchecked")
	protected static SettingsModelColumnFilter2 createIncludedFeaturesModel() {
		return new SettingsModelColumnFilter2("Included features", DoubleValue.class, IntValue.class);
	}

	/**
	 * Constructor for the node model.
	 */
	protected LucsKddDnNodeModel() {
		super(1, 1);
	}

	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		LOGGER.info("Starting LucsKddDnNodeModel.execute()");

		// input data
		BufferedDataTable input = inData[0];

		// input table scheme
		DataTableSpec inSpec = input.getDataTableSpec();

		// get features for discretization
		String[] features = m_features.applyTo(inSpec).getIncludes();

		// create the column re-arranger for the output table
		ColumnRearranger outputTable = new ColumnRearranger(inSpec);

		// cell factory
		AbstractCellFactory cellFactory;

		LOGGER.debug("\n\nCREATED BUCKETS");
		LOGGER.debug("------------------------------------------------");

		// loop over features
		for (String feature : features) {

			// create initial buckets and fill them
			BucketList buckets = new BucketList(feature, m_class.getStringValue(), inData[0]);
			buckets.fill();

			// for each bucket determine the dominant class
			// for buckets without dominant class, the dominant class is imputed
			// from the nearest buckets
			buckets.determineDominantClasses();

			// form division by merging subsequent buckets with the same
			// dominant class
			buckets.formDivisions();

			// merge divisions until number of division is smaller or equal than
			// the user-defined maximal number
			while (buckets.size() > m_divisions.getIntValue()) {
				buckets.mergeDivisions();
			}

			// create cell factory
			cellFactory = new LucsKddDnCellFactory(createOutputColumnSpec(feature), buckets);

			// replace column
			outputTable.replace(cellFactory, feature);

			int i = 0;
			LOGGER.debug(feature + ":");
			LOGGER.debugWithFormat("%8s%20s%20s\n", "Category", "Class", "Range");
			for (Bucket b : buckets.getBuckets()) {
				LOGGER.debugWithFormat("%8s%20s%20s\n", i, b.getDominantClass(), b.getRange().toString());
				i++;
			}
			LOGGER.debug("------------------------------------------------");
		}

		// create the output table
		BufferedDataTable bufferedOutput = exec.createColumnRearrangeTable(input, outputTable, exec);

		return new BufferedDataTable[] { bufferedOutput };

	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		// check if user settings are available, fit to the incoming
		// table structure, and the incoming types are feasible for the node
		// to execute. If the node can execute in its current state return
		// the spec of its output data table(s) (if you can, otherwise an array
		// with null elements), or throw an exception with a useful user message
		DataTableSpec inputSpec = inSpecs[0];

		// check if input table contains nominal class attribute
		DataColumnSpec columnSpec = inputSpec.getColumnSpec(m_class.getStringValue());
		if (columnSpec == null || !columnSpec.getType().isCompatible(NominalValue.class)) {
			// if no useful column is selected guess one
			// get the first useful one starting at the end of the table
			for (int i = inputSpec.getNumColumns() - 1; i >= 0; i--) {
				if (inputSpec.getColumnSpec(i).getType().isCompatible(NominalValue.class)) {
					m_class.setStringValue(inputSpec.getColumnSpec(i).getName());
					break;
				}
				throw new InvalidSettingsException("Table contains no nominal class attribute.");
			}
		}
		return new DataTableSpec[] { null };
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_divisions.saveSettingsTo(settings);
		m_class.saveSettingsTo(settings);
		m_features.saveSettingsTo(settings);

	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_divisions.loadSettingsFrom(settings);
		m_class.loadSettingsFrom(settings);
		m_features.loadSettingsFrom(settings);
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_divisions.validateSettings(settings);
		m_class.validateSettings(settings);
		m_features.validateSettings(settings);
	}

	@Override
	protected void loadInternals(final File internDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// Nothing to do.
	}

	@Override
	protected void saveInternals(final File internDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// Nothing to do.
	}
	
	@Override
	protected void reset() {
		 // Nothing to do.
	}


	private static DataColumnSpec createOutputColumnSpec(String feature) {

		// creator for the discretized feature
		DataColumnSpecCreator colSpecCreator = new DataColumnSpecCreator(feature, StringCell.TYPE);

		// create the specification for the new column
		DataColumnSpec newColumnSpec = colSpecCreator.createSpec();

		return newColumnSpec;
	}

}
