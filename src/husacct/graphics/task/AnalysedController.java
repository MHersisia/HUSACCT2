package husacct.graphics.task;

import husacct.ServiceProvider;
import husacct.analyse.IAnalyseService;
import husacct.common.dto.AbstractDTO;
import husacct.common.dto.AnalysedModuleDTO;
import husacct.common.dto.DependencyDTO;
import husacct.common.dto.ExternalSystemDTO;
import husacct.common.dto.ProjectDTO;
import husacct.common.dto.ViolationDTO;
import husacct.common.services.IServiceListener;
import husacct.control.IControlService;
import husacct.graphics.presentation.figures.BaseFigure;
import husacct.graphics.presentation.figures.ProjectFigure;
import husacct.graphics.util.DrawingDetail;
import husacct.validate.IValidateService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jhotdraw.draw.Figure;

public class AnalysedController extends DrawingController {
	private final Logger logger = Logger.getLogger(AnalysedController.class);
	protected IAnalyseService analyseService;
	protected IControlService controlService;
	protected IValidateService validateService;

	private ArrayList<BaseFigure> analysedContextFigures;

	public AnalysedController() {
		super();
		initializeServices();
	}

	private void initializeServices() {
		this.controlService = ServiceProvider.getInstance().getControlService();

		this.analyseService = ServiceProvider.getInstance().getAnalyseService();
		this.analyseService.addServiceListener(new IServiceListener() {
			@Override
			public void update() {
				AnalysedController.this.refreshDrawing();
			}
		});

		this.validateService = ServiceProvider.getInstance()
				.getValidateService();
		this.validateService.addServiceListener(new IServiceListener() {
			@Override
			public void update() {
				if (AnalysedController.this.areViolationsShown()) {
					AnalysedController.this.refreshDrawing();
				}
			}
		});
	}

	@Override
	public void drawArchitecture(DrawingDetail detail) {
		super.drawArchitecture(getCurrentDrawingDetail());
		super.notifyServiceListeners();
		this.resetCurrentPaths();

		if (DrawingDetail.WITH_VIOLATIONS == detail) {
			this.showViolations();
		}
		
		/*
		 * Shows all the dependencies
		 * 
		 * for(ExternalSystemDTO ex : this.analyseService.getExternalSystems()){
			for(DependencyDTO dto : ex.fromDependencies){
				System.out.println(dto.toString());
			}
		}*/

		ArrayList<ProjectDTO> projects = this.controlService.getApplicationDTO().projects;
		AbstractDTO[] projectArray = projects.toArray(new AbstractDTO[projects.size()]);
		
		this.drawModulesAndLines(projectArray);
	}

	private void getAndDrawModulesIn(String parentName) {
		AnalysedModuleDTO[] children = this.analyseService.getChildModulesInModule(parentName);
		if (parentName.equals("")) {
			this.drawArchitecture(this.getCurrentDrawingDetail());
		} else if (children.length > 0) {
			this.setCurrentPaths(new String[] { parentName });
			this.drawModulesAndLines(children);
		} else {
			this.logger.warn("Tried to draw modules for \"" + parentName + "\", but it has no children.");
		}
	}

	private void getAndDrawModulesIn(String[] parentNames) {
		if (parentNames.length == 0)
			drawArchitecture(getCurrentDrawingDetail());
		else {
			ExternalSystemDTO[] extSystems = this.analyseService.getExternalSystems();
			
			HashMap<String, ArrayList<AbstractDTO>> allChildren = new HashMap<String, ArrayList<AbstractDTO>>();
			ArrayList<String> compoundedNames = new ArrayList<String>();

			for (String parentName : parentNames) {
				compoundedNames.add(parentName);
				ArrayList<AbstractDTO> knownChildren = this.getChildrenOf(parentName);
				if (knownChildren.size() > 0) {
					allChildren.put(parentName, knownChildren);
				}
			}

			if (analysedContextFigures.size() > 0) {
				ArrayList<AbstractDTO> tmp = new ArrayList<AbstractDTO>();
				for (BaseFigure figure : analysedContextFigures)
					if (!figure.isLine() && !figure.isParent()) {

						AbstractDTO dto = this.getFigureMap().getModuleDTO(figure);
						if (null != dto) {
							tmp.add(dto);
						} else {
							this.logger.debug(figure.getName() + " -> "	+ figure);
						}
					} else if (!figure.isLine() && !figure.isModule()) {
						// NOTE: Pretty sure selected stuff that is both not a
						// module and not a line
						// is actually a ParentFigure (blue square thing)
						ArrayList<AbstractDTO> knownChildren = getChildrenOf(figure
								.getName());
						if (knownChildren.size() > 0)
							allChildren.put(figure.getName(), knownChildren);
					}
				if (tmp.size() > 0)
					allChildren.put("", tmp);
			}
			setCurrentPaths(parentNames);

			Set<String> parentNamesKeySet = allChildren.keySet();
			if (parentNamesKeySet.size() == 1) {
				String onlyParentModule = parentNamesKeySet.iterator().next();
				ArrayList<AbstractDTO> onlyParentChildren = allChildren.get(onlyParentModule);
				this.drawModulesAndLines(onlyParentChildren.toArray(new AbstractDTO[] {}), extSystems);
			} else {
				this.drawModulesAndLines(allChildren);
			}
		}
	}

	private ArrayList<AbstractDTO> getChildrenOf(String parentName) {
		AbstractDTO[] children = this.analyseService.getChildModulesInModule(parentName);

		ArrayList<AbstractDTO> knownChildren = new ArrayList<AbstractDTO>();

		if (parentName.equals(""))
			drawArchitecture(getCurrentDrawingDetail());
		else if (children.length > 0) {
			knownChildren = new ArrayList<AbstractDTO>();
			for (AbstractDTO child : children)
				knownChildren.add(child);
		} else {
			logger.warn("Tried to draw modules for \"" + parentName
					+ "\", but it has no children.");
		}
		return knownChildren;
	}

	@Override
	protected DependencyDTO[] getDependenciesBetween(BaseFigure figureFrom,	BaseFigure figureTo) {
		AnalysedModuleDTO dtoFrom = null;
		AnalysedModuleDTO dtoTo = null;
		
		ExternalSystemDTO exDtoTo = null;
		
		if(this.getFigureMap().getModuleDTO(figureFrom) instanceof ExternalSystemDTO){
			return new DependencyDTO[] {};
			
		} else if(this.getFigureMap().getModuleDTO(figureTo) instanceof ExternalSystemDTO){
			dtoFrom = (AnalysedModuleDTO) this.getFigureMap().getModuleDTO(figureFrom);
			exDtoTo = (ExternalSystemDTO) this.getFigureMap().getModuleDTO(figureTo);
			
		}else{
			dtoFrom = (AnalysedModuleDTO) this.getFigureMap().getModuleDTO(figureFrom);
			dtoTo = (AnalysedModuleDTO) this.getFigureMap().getModuleDTO(figureTo);
		}
		if (!figureFrom.equals(figureTo) && dtoFrom != null && dtoTo != null) {
			return this.analyseService.getDependencies(dtoFrom.uniqueName, dtoTo.uniqueName);
			
		}else if(!figureFrom.equals(figureTo) && dtoFrom != null && exDtoTo != null){
			return this.analyseService.getDependencies(dtoFrom.uniqueName, exDtoTo.systemPackage);
			
		}
		return new DependencyDTO[] {};
	}
	
	
	@Override
	protected ExternalSystemDTO[] getToShowExternalSystems(ExternalSystemDTO[] extSystems, BaseFigure[] shownFigures) {
		ArrayList<ExternalSystemDTO> foundExtSystems = new ArrayList<ExternalSystemDTO>();
		
		for(ExternalSystemDTO extSystem : extSystems){
			DependencyDTO[] extSystemDependencies = this.analyseService.getDependenciesTo(extSystem.systemPackage);
			
			for(BaseFigure figure : shownFigures){
				AnalysedModuleDTO dtoFrom = (AnalysedModuleDTO) this.getFigureMap().getModuleDTO(figure);
				
				for(DependencyDTO dependency : extSystemDependencies){
					if(dependency.from.equals(dtoFrom.uniqueName)){
						if(!foundExtSystems.contains(extSystem)){
							foundExtSystems.add(extSystem);
						}
					}
				}
			}
		}
		
		return foundExtSystems.toArray(new ExternalSystemDTO[foundExtSystems.size()] );
	}

	@Override
	protected ViolationDTO[] getViolationsBetween(BaseFigure figureFrom, BaseFigure figureTo) {
		AnalysedModuleDTO dtoFrom = (AnalysedModuleDTO) this.getFigureMap().getModuleDTO(figureFrom);
		AnalysedModuleDTO dtoTo = (AnalysedModuleDTO) this.getFigureMap().getModuleDTO(figureTo);
		return this.validateService.getViolationsByPhysicalPath(dtoFrom.uniqueName, dtoTo.uniqueName);
	}

	@Override
	public void moduleOpen(String[] paths) {
		super.notifyServiceListeners();
		saveSingleLevelFigurePositions();
		resetContextFigures();
		if (paths.length == 0)
			drawArchitecture(getCurrentDrawingDetail());
		else
			this.getAndDrawModulesIn(paths);
	}

	@Override
	public void moduleZoom(String zoomType){

		BaseFigure[] selection = super.getSelectedFigures();

		super.notifyServiceListeners();
		resetContextFigures();
		ArrayList<String> parentNames = sortFiguresBasedOnZoomability(selection);

		if (parentNames.size() > 0) {
			saveSingleLevelFigurePositions();
			this.getAndDrawModulesIn(parentNames.toArray(new String[] {}));
		}
	}

	@Override
	public void moduleZoom(BaseFigure[] figures) {
		super.notifyServiceListeners();
		this.resetContextFigures();

		boolean allProjects = false;
		for(BaseFigure fig : figures){
			if(!(fig instanceof ProjectFigure)){
				allProjects = false;
			} else{
				allProjects = true;
			}
		}

		if(allProjects){
			setCurrentPathsForProjects(figures);
			ProjectDTO project = (ProjectDTO) this.getFigureMap().getModuleDTO(figures[0]);
			AbstractDTO[] abstractDTOs = project.analysedModules.toArray(new AbstractDTO[project.analysedModules.size()] );
			if(abstractDTOs.length != 0){
				
				this.drawModulesAndLines(abstractDTOs);
			}
		}

		ArrayList<String> parentNames = this.sortFiguresBasedOnZoomability(figures);
		if (parentNames.size() > 0) {
			saveSingleLevelFigurePositions();
			this.getAndDrawModulesIn(parentNames.toArray(new String[] {}));
		}
	}

	//TODO Needs to be removed as soon as uniqueName of a AnalysedDTO contains a project
	private void setCurrentPathsForProjects(BaseFigure[] figures){
		String[] paths = new String[1];
		paths[0] = figures[0].getName();
		super.setCurrentPaths(paths);
	}

	@Override
	public void moduleZoomOut() {
		super.notifyServiceListeners();
		if (getCurrentPaths().length > 0) {
			saveSingleLevelFigurePositions();
			resetContextFigures();
			String firstCurrentPaths = getCurrentPaths()[0];
			AnalysedModuleDTO parentDTO = analyseService
					.getParentModuleForModule(firstCurrentPaths);

			if (parentDTO != null)
				this.getAndDrawModulesIn(parentDTO.uniqueName);
			else
				zoomOutFailed();
		} else
			zoomOutFailed();
	}

	public void zoomOutFailed(){
		this.logger.warn("Tried to zoom out from \"" + this.getCurrentPaths()
				+ "\", but it has no parent (could be root if it's an empty string).");
		this.logger.debug("Reverting to the root of the application.");
		this.drawArchitecture(this.getCurrentDrawingDetail());
	}

	@Override
	public void refreshDrawing() {
		super.notifyServiceListeners();
		this.getAndDrawModulesIn(getCurrentPaths());
	}

	private void resetContextFigures() {
		analysedContextFigures = new ArrayList<BaseFigure>();
	}

	@Override
	public void showViolations() {
		if (validateService.isValidated())
			super.showViolations();
	}
	

	@Override
	public void showExternalSystems() {
		if (analyseService.isAnalysed())
			super.showExternalSystems();
	}

	protected ArrayList<String> sortFiguresBasedOnZoomability(BaseFigure[] figures) {
		ArrayList<String> parentNames = new ArrayList<String>();
		for (BaseFigure figure : figures)
			if (figure.isModule() && !figure.isContext())
				try {
					if(!(figure instanceof ProjectFigure)){
						AnalysedModuleDTO parentDTO = (AnalysedModuleDTO) this.getFigureMap().getModuleDTO(figure);
						parentNames.add(parentDTO.uniqueName);
					}
				} catch (Exception e) {
					e.printStackTrace();
					logger.warn("Could not zoom on this object: "
							+ figure.getName()
							+ ". Expected a different DTO type.");
				}
			else if (!figure.isLine() || figure.isContext()) {
				analysedContextFigures.add(figure);
				logger.warn("Figure: " + figure.getName()
						+ " is accepted as context for multizoom.");
			} else
				logger.warn("Could not zoom on this object: "
						+ figure.getName() + ". Not a module to zoom on.");

		return parentNames;
	}
}
