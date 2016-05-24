package husacct.analyse.task.reconstruct.parameters;

import javax.swing.JPanel;

import husacct.ServiceProvider;
import husacct.analyse.task.reconstruct.AnalyseReconstructConstants.AlgorithmParameter;
import husacct.analyse.task.reconstruct.AnalyseReconstructConstants.Granularities;
import husacct.analyse.task.reconstruct.AnalyseReconstructConstants.RelationTypes;
import husacct.analyse.task.reconstruct.parameters.ComboBoxPanel.ComboBoxValue;
import husacct.common.locale.ILocaleService;

public abstract class ParameterPanel {
	public Object value = null;
	public Object minimumValue = null;
	public Object maximumValue = null;
	public String parameterConstant;
	protected String label; 
	
	public ParameterPanel (String label, String parameterConstant){
		this.label = label;
		this.parameterConstant = parameterConstant;
	}
	
	public abstract JPanel createPanel();
	
	public abstract Object getValue();
	
	protected String getTranslation(String translationKey){
		ILocaleService localeService = ServiceProvider.getInstance().getLocaleService();
		return localeService.getTranslatedString(translationKey);
	}
	
	
	public static class DefaultParameterPanels{
		public static ParameterPanel createThresholdParameter(int defaultValue){
			ParameterPanel numberField = new NumberFieldPanel("Threshold", AlgorithmParameter.Threshold, 10);
			numberField.value = defaultValue;
			numberField.minimumValue = 0;
			numberField.maximumValue = 100;
			return numberField;
		}
		public static ParameterPanel createRelationTypeParameter(String defaultValue){
			ComboBoxValue[] comboBoxValues = new ComboBoxValue[]{
					new ComboBoxValue(getTranslation(RelationTypes.allDependencies), RelationTypes.allDependencies),
					new ComboBoxValue(getTranslation(RelationTypes.umlLinks), RelationTypes.umlLinks),
					new ComboBoxValue(getTranslation(RelationTypes.accessCallReferenceDependencies), RelationTypes.accessCallReferenceDependencies)};
			ParameterPanel comboBoxPanel = new ComboBoxPanel(getTranslation(AlgorithmParameter.RelationType), AlgorithmParameter.RelationType, comboBoxValues);
			comboBoxPanel.value = defaultValue;
			return comboBoxPanel;
		}
		public static ParameterPanel createGranularityPanel(String defaultValue){
			ComboBoxValue[] comboBoxValues = new ComboBoxValue[]{
					new ComboBoxValue(getTranslation(Granularities.Classes), Granularities.Classes),
					new ComboBoxValue(getTranslation(Granularities.PackagesInRootOnlyClasses), Granularities.PackagesInRootOnlyClasses),
					new ComboBoxValue(getTranslation(Granularities.PackagesWithAllClasses), Granularities.PackagesWithAllClasses)};
			ParameterPanel comboBoxPanel = new ComboBoxPanel(getTranslation(AlgorithmParameter.Granularity), AlgorithmParameter.Granularity, comboBoxValues);
			comboBoxPanel.value = defaultValue;
			return comboBoxPanel;
		}
		private static String getTranslation(String translationKey){
			ILocaleService localeService = ServiceProvider.getInstance().getLocaleService();
			return localeService.getTranslatedString(translationKey);
		}
	}
	
}
