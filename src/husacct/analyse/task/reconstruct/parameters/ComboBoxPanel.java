package husacct.analyse.task.reconstruct.parameters;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;


public class ComboBoxPanel extends ParameterPanel{
	private final Logger logger = Logger.getLogger(ComboBoxPanel.class);
	private ComboBoxValue[] comboBoxValues = new ComboBoxValue[]{};
	private JComboBox<ComboBoxValue> comboBox;
	
	public ComboBoxPanel(String label, String parameterConstant, ComboBoxValue[] comboBoxValues) {
		super(label, parameterConstant);
		this.comboBoxValues = comboBoxValues;
	}

	@Override
	public JPanel createPanel() {
		JPanel comboBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		comboBoxPanel.setPreferredSize(new Dimension(400, 30));
		JLabel jLabel = new JLabel(label);
		comboBoxPanel.add(jLabel);
		comboBox = new JComboBox<>(comboBoxValues);
		comboBoxPanel.add(comboBox);
		
		return comboBoxPanel;
	}

	@Override
	public Object getValue() {
		try{
			ComboBoxValue cbValue = (ComboBoxValue) comboBox.getSelectedItem();
			return cbValue.getValue();
		}
		catch(Exception e){
			logger.error("Invalid cast from comboBox.getSelectedItem() - " + e);
		}
		return "something went wrong";
	}
	
	
	
	public static class ComboBoxValue {
	    private final String label;
	    private final String value;
	    public ComboBoxValue(String label, String value) {
	        this.label = label;
	        this.value = value;
	    }
	    public String getLabel() {
	        return label;
	    }
	    public String getValue() {
	        return value;
	    }
	    public String toString(){
	    	return label;
	    }
	}


}
