package husacct.analyse.presentation.reconstruct;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

import org.apache.log4j.Logger;

import husacct.ServiceProvider;
import husacct.analyse.task.AnalyseTaskControl;
import husacct.analyse.task.reconstruct.AnalyseReconstructConstants.AlgorithmSettings;
import husacct.analyse.task.reconstruct.AnalyseReconstructConstants.Granularities;
import husacct.analyse.task.reconstruct.AnalyseReconstructConstants.RelationTypes;
import husacct.common.dto.ReconstructArchitectureDTO;
import husacct.common.help.presentation.HelpableJInternalFrame;
import husacct.common.locale.ILocaleService;

public class ApproachesSettingsFrame extends HelpableJInternalFrame implements ActionListener{
	private final Logger logger = Logger.getLogger(ApproachesSettingsFrame.class);
	private static final long serialVersionUID = 1L;
	private ReconstructArchitectureDTO dto;
	private JButton applyButton, cancelButton;
	private JFrame frame;
	private JTextField thresholdField;
	private AnalyseTaskControl analyseTaskControl;
	private JComboBox<String> relationTypeComboBox;
	private JComboBox<String> granularityComboBox;

	
	public ApproachesSettingsFrame(AnalyseTaskControl atc, ReconstructArchitectureDTO dto){
		this.dto = dto;
		this.analyseTaskControl = atc;
		buildFrame();
	}
	
	public void buildFrame(){
		frame = new JFrame("Approach Settings");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setSize(600, 400);
		frame.setVisible(true);
		frame.getContentPane().add(buildPanel(), BorderLayout.CENTER);
	}
	
	private JPanel buildPanel(){
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(this.buildApproachLabel(), BorderLayout.NORTH);
		
		JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		for (String s : dto.thresholdSettings){
			if (s.equals(AlgorithmSettings.Threshold)){
				settingsPanel.add(this.buildThresHoldPanel());
			}
			else if (s.equals(AlgorithmSettings.RelationType)){
				settingsPanel.add(this.buildRelationTypePanel());
			}
			else if (s.equals(AlgorithmSettings.Granularity)){
				settingsPanel.add(this.buildGranularityPanel());
			}
		}
		mainPanel.add(settingsPanel, BorderLayout.CENTER);
		
		mainPanel.add(this.buildButtonPanel(), BorderLayout.SOUTH);
		
		return mainPanel;
	}

	private JPanel buildApproachLabel(){
		JPanel approachLabelPanel = new JPanel();
		JLabel approachLabelLabel = new JLabel(getTranslation("Approach") + ": " + dto.approachConstant);
		approachLabelPanel.add(approachLabelLabel);
		return approachLabelPanel;
	}
	
	private JPanel buildThresHoldPanel(){
		JPanel thresholdPanel = new JPanel();
		JLabel thresholdLabel = new JLabel(getTranslation("Threshold"));
		thresholdField = new JTextField();
		thresholdField.setText(dto.threshold + "");
		thresholdField.setColumns(10);
		thresholdPanel.add(thresholdLabel);
		thresholdPanel.add(thresholdField);
		return thresholdPanel;
	}
	
	private JPanel buildRelationTypePanel(){
		JPanel relationTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel relationTypeLabel = new JLabel(getTranslation("RelationType"));
		relationTypePanel.add(relationTypeLabel);
		
		relationTypeComboBox = new JComboBox<String>();
		relationTypeComboBox.addItem(RelationTypes.allDependencies);
		relationTypeComboBox.addItem(RelationTypes.accessCallReferenceDependencies);
		relationTypeComboBox.addItem(RelationTypes.umlLinks);
		
		relationTypePanel.add(relationTypeComboBox);
		relationTypePanel.setPreferredSize(new Dimension(580, 70));
		
		return relationTypePanel;
	}
	
	private JPanel buildGranularityPanel(){
		JPanel granularityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel granularityLabel = new JLabel(getTranslation("Granularity"));
		granularityPanel.add(granularityLabel);
		
		granularityComboBox = new JComboBox<String>();
		granularityComboBox.addItem(Granularities.Classes);
		granularityComboBox.addItem(Granularities.PackagesInRootOnlyClasses);
		granularityComboBox.addItem(Granularities.PackagesWithAllClasses);
		
		granularityPanel.add(granularityComboBox);
		granularityPanel.setPreferredSize(new Dimension(580, 70));
		
		return granularityPanel;
	}
	
	private JPanel buildButtonPanel(){
		JPanel buttonPanel = new JPanel(new GridLayout(1,1,0,5));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 50, 5, 50));
		applyButton = new JButton("Apply");
		buttonPanel.add(applyButton);
		applyButton.addActionListener(this);
		
		cancelButton = new JButton("Cancel");
		buttonPanel.add(cancelButton);
		cancelButton.addActionListener(this);
		
		return buttonPanel;
	}
	
	private String getTranslation(String translationKey){
		ILocaleService localeService = ServiceProvider.getInstance().getLocaleService();
		return localeService.getTranslatedString(translationKey);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == applyButton) {
			for (String s : dto.thresholdSettings){
				if (s.equals(AlgorithmSettings.Threshold)){
					try{
						dto.threshold = Integer.parseInt(thresholdField.getText());
					}catch(Exception e){
						logger.error("threshold is no int: " + e);
					}
				}
				else if (s.equals(AlgorithmSettings.RelationType)){
					dto.relationType = relationTypeComboBox.getSelectedItem().toString();
				}
				else if (s.equals(AlgorithmSettings.Granularity)){
					dto.granularity = granularityComboBox.getSelectedItem().toString();
				}
			}
			
			analyseTaskControl.reconstructArchitectureListDTO.updateReconstructArchitectureDTO(dto);
			frame.dispose();
		}
		else if(event.getSource() == cancelButton){
			frame.dispose();
		}
		
	}
}
