package husacct.analyse.task.reconstruct.parameters;

import javax.swing.JPanel;

import husacct.ServiceProvider;
import husacct.common.dto.ReconstructArchitectureDTO;
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
}
