package husacct.graphics.task;

import husacct.ServiceProvider;
import husacct.common.dto.AbstractDTO;
import husacct.common.dto.AnalysedModuleDTO;
import husacct.common.dto.DependencyDTO;
import husacct.common.dto.ModuleDTO;
import husacct.common.dto.ViolationDTO;
import husacct.common.locale.ILocaleService;
import husacct.common.services.IServiceListener;
import husacct.graphics.presentation.Drawing;
import husacct.graphics.presentation.DrawingView;
import husacct.graphics.presentation.GraphicsFrame;
import husacct.graphics.presentation.figures.BaseFigure;
import husacct.graphics.presentation.figures.FigureFactory;
import husacct.graphics.presentation.figures.RelationFigure;
import husacct.graphics.task.layout.BasicLayoutStrategy;
import husacct.graphics.task.layout.DrawingState;
import husacct.graphics.task.layout.LayeredLayoutStrategy;
import husacct.graphics.task.layout.LayoutStrategy;
import husacct.graphics.task.layout.NoLayoutStrategy;
import husacct.graphics.util.DrawingDetail;
import husacct.graphics.util.DrawingLayoutStrategy;
import husacct.graphics.util.FigureMap;
import husacct.graphics.util.register.DrawingRegister;
import husacct.graphics.util.register.NewDrawingState;
import husacct.graphics.util.threads.DrawingFiguresThread;
import husacct.graphics.util.threads.DrawingLinesThread;
import husacct.graphics.util.threads.ThreadMonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import javax.swing.JInternalFrame;

import org.apache.log4j.Logger;
import org.jhotdraw.draw.Figure;

public abstract class DrawingController extends DrawingSettingsController {
	protected static final boolean debugPrint = true;
	protected DrawingLayoutStrategy layoutStrategyOption;

	private HashMap<String, DrawingState> storedStates = new HashMap<String, DrawingState>();

	private Drawing drawing;
	private DrawingView drawingView;
	private GraphicsFrame graphicsFrame;

	protected ILocaleService localeService;
	protected Logger logger = Logger.getLogger(DrawingController.class);

	private FigureFactory figureFactory;
	private LayoutStrategy layoutStrategy;

	protected ThreadMonitor threadMonitor;
	@Deprecated
	private FigureMap figureMap = new FigureMap();
	private DrawingRegister register = new DrawingRegister();

	public DrawingController() {
		super();
		layoutStrategyOption = DrawingLayoutStrategy.BASIC_LAYOUT;

		figureFactory = new FigureFactory();

		localeService = ServiceProvider.getInstance().getLocaleService();
		localeService.addServiceListener(new IServiceListener() {
			@Override
			public void update() {
				refreshFrame();
			}
		});

		initializeComponents();
		switchLayoutStrategy();
		loadDefaultSettings();
	}

	private void initializeComponents() {
		drawing = new Drawing();
		drawingView = new DrawingView(drawing);
		drawingView.addListener(this);

		graphicsFrame = new GraphicsFrame(drawingView);
		graphicsFrame.addListener(this);
		graphicsFrame.setSelectedLayout(layoutStrategyOption);

		threadMonitor = new ThreadMonitor(this);
	}

	private void runThread(Runnable runnable) {
		if (!threadMonitor.add(runnable)) {
			logger.warn("A drawing thread is already running. Wait until it has finished before running another.");
			graphicsFrame.setOutOfDate();
		}
	}

	@Deprecated
	public FigureMap getFigureMap() {
		return figureMap;
	}

	@Deprecated
	public Drawing getDrawing() {
		return drawing;
	}

	private void switchLayoutStrategy() {
		switch (layoutStrategyOption) {
		case BASIC_LAYOUT:
			layoutStrategy = new BasicLayoutStrategy(drawing);
			break;
		case LAYERED_LAYOUT:
			layoutStrategy = new LayeredLayoutStrategy(drawing);
			break;
		default:
			layoutStrategy = new NoLayoutStrategy();
			break;
		}
	}

	public DrawingLayoutStrategy getLayoutStrategy() {
		return layoutStrategyOption;
	}

	public void changeLayoutStrategy(DrawingLayoutStrategy selectedStrategyEnum) {
		layoutStrategyOption = selectedStrategyEnum;
		switchLayoutStrategy();
		updateLayout();
	}

	@Override
	public void showDependencies() {
		super.showDependencies();
		graphicsFrame.turnOnDependencies();
	}

	@Override
	public void hideDependencies() {
		super.hideDependencies();
		graphicsFrame.turnOffDependencies();
	}

	@Override
	public void showViolations() {
		super.showViolations();
		graphicsFrame.turnOnViolations();
	}

	@Override
	public void hideViolations() {
		super.hideViolations();
		graphicsFrame.turnOffViolations();
		drawing.setFiguresNotViolated(figureMap.getViolatedFigures());
	}

	public void showSmartLines() {
		super.showSmartLines();
		graphicsFrame.turnOnSmartLines();
	}

	public void hideSmartLines() {
		super.hideSmartLines();
		graphicsFrame.turnOffSmartLines();
	}

	public JInternalFrame getGUI() {
		return graphicsFrame;
	}

	public void clearDrawing() {
		figureMap.clearAll();
		drawing.clearAll();

		drawingView.clearSelection();
		drawingView.invalidate();

		drawing.setState(getCurrentState());
	}

	public void clearLines() {
		drawing.clearAllLines();
	}

	@Deprecated
	public void setCurrentPaths(String[] paths) {
		super.setCurrentPaths(paths);
		if (!getCurrentPaths()[0].isEmpty()) {
			drawingView.canZoomOut();
		} else {
			drawingView.cannotZoomOut();
		}
	}

	@Override
	public void figureSelected(BaseFigure[] figures) {
		BaseFigure selectedFigure = figures[0];
		if (figureMap.isViolatedFigure(selectedFigure)) {
			graphicsFrame.showViolationsProperties(figureMap.getViolatedDTOs(selectedFigure));
		} else if (figureMap.isViolationLine(selectedFigure)) {
			graphicsFrame.showViolationsProperties(figureMap.getViolationDTOs(selectedFigure));
		} else if (figureMap.isDependencyLine(selectedFigure)) {
			graphicsFrame.showDependenciesProperties(figureMap.getDependencyDTOs(selectedFigure));
		} else {
			graphicsFrame.hideProperties();
		}
	}

	@Override
	public void figureDeselected(BaseFigure[] figures) {
		if (drawingView.getSelectionCount() == 0) {
			graphicsFrame.hideProperties();
		}
	}

	public void drawArchitecture(DrawingDetail detail) {
		drawingView.cannotZoomOut();
	}

	protected void updateLayout() {
		String currentPaths = getCurrentPathsToString();

		if (hasSavedFigureStates(currentPaths)) {
			restoreFigurePositions(currentPaths);
		} else {
			layoutStrategy.doLayout();
			drawingView.setHasHiddenFigures(false);
		}

		drawing.updateLines();
	}

	protected void drawLinesBasedOnSettingInTask() {
		clearLines();
		setDrawingViewNonVisible();
		runDrawLinesTask();
	}

	private void runDrawLinesTask() {
		runThread(new DrawingLinesThread(this));
	}

	public void drawLines() {
		if (areDependenciesShown()) {
			drawDependencies();
		}
		if (areViolationsShown()) {
			drawViolations();
		}
		if (areSmartLinesOn()) {
			drawing.updateLineFigureToContext();
		}
	}

	public void drawDependencies() {
		NewDrawingState state = getCurrentState();
		ArrayList<String> paths = state.getPaths();
		for (String path : paths) {
			ArrayList<BaseFigure> figures = state.getFiguresByPath(path);
			for (BaseFigure figureFrom : figures) {
				for (BaseFigure figureTo : figures) {
					drawDependenciesBetween(figureFrom, figureTo);
				}
			}
		}
	}

	private void drawDependenciesBetween(BaseFigure figureFrom, BaseFigure figureTo) {
		if (!figureFrom.equals(figureTo)) {
			NewDrawingState state = getCurrentState();
			AbstractDTO dtoFrom = state.getFigureDTO(figureFrom);
			AbstractDTO dtoTo = state.getFigureDTO(figureTo);
			DependencyDTO[] dependencies = getDependenciesBetween(dtoFrom, dtoTo);
			if (dependencies.length > 0) {
				addDependency(dependencies, figureFrom, figureTo);
			}
		}
	}

	protected abstract DependencyDTO[] getDependenciesBetween(AbstractDTO dtoFrom, AbstractDTO dtoTo);

	public void drawViolations() {
		NewDrawingState state = getCurrentState();
		ArrayList<String> paths = state.getPaths();
		for (String path : paths) {
			ArrayList<BaseFigure> figures = state.getFiguresByPath(path);
			for (BaseFigure figureFrom : figures) {
				for (BaseFigure figureTo : figures) {
					drawViolationsBetween(figureFrom, figureTo);
				}
			}
		}
	}

	private void drawViolationsBetween(BaseFigure figureFrom, BaseFigure figureTo) {
		NewDrawingState state = getCurrentState();
		AbstractDTO dtoFrom = state.getFigureDTO(figureFrom);
		AbstractDTO dtoTo = state.getFigureDTO(figureTo);
		ViolationDTO[] violations = getViolationsBetween(dtoFrom, dtoTo);
		if (violations.length > 0) {
			addViolation(violations, figureFrom, figureTo);
		}
	}

	protected abstract ViolationDTO[] getViolationsBetween(AbstractDTO dtoFrom, AbstractDTO dtoTo);

	public abstract void refreshDrawing();

	public void refreshFrame() {
		graphicsFrame.refreshFrame();
	}

	@Override
	public void exportToImage() {
		drawing.showExportToImagePanel();
	}

	@Deprecated
	public void saveSingleLevelFigurePositions() {
		if (getCurrentPaths().length < 2) {
			saveFigurePositions();
		}
	}

	@Deprecated
	protected void saveFigurePositions() {
		String paths = getCurrentPathsToString();
		DrawingState state;
		if (storedStates.containsKey(paths))
			state = storedStates.get(paths);
		else
			state = new DrawingState(drawing);

		state.save(figureMap);
		storedStates.put(paths, state);
	}

	@Deprecated
	protected boolean hasSavedFigureStates(String paths) {
		return storedStates.containsKey(paths);
	}

	@Deprecated
	protected void restoreFigurePositions(String paths) {
		if (storedStates.containsKey(paths)) {
			DrawingState state = storedStates.get(paths);
			state.restore(figureMap);
			drawingView.setHasHiddenFigures(state.hasHiddenFigures());
		}
	}

	@Deprecated
	protected void resetAllFigurePositions() {
		storedStates.clear();
	}

	@Override
	public void drawingZoomChanged(double zoomFactor) {
		drawingView.setScaleFactor(zoomFactor);
	}

	@Override
	public void hideModules() {
		drawingView.hideSelectedFigures();
	}

	@Override
	public void restoreModules() {
		drawingView.restoreHiddenFigures();
	}

	@Override
	public void moduleZoom() {
		Set<Figure> selection = drawingView.getSelectedFigures();
		if (selection.size() > 0) {
			BaseFigure[] selectedFigures = selection.toArray(new BaseFigure[selection.size()]);
			moduleZoom(selectedFigures);
		}
	}

	public void setDrawingViewVisible() {
		graphicsFrame.hideLoadingScreen();
		drawingView.setVisible(true);
	}

	public void setDrawingViewNonVisible() {
		drawingView.setVisible(false);
		graphicsFrame.showLoadingScreen();
	}

	public boolean isDrawingVisible() {
		return drawingView.isVisible();
	}

	protected NewDrawingState getCurrentState() {
		return register.getCurrentState();
	}

	protected void createState(String combinedPath) {
		register.addState(new NewDrawingState(combinedPath));
	}

	protected void drawDrawing() {
		runThread(new DrawingFiguresThread(this));
	}

	public void drawDrawingReal() {
		drawFigures();
		updateLayout();
		drawLines();
		// graphicsFrame.setCurrentPaths(getCurrentPaths());
		graphicsFrame.updateGUI();
	}

	public void drawFigures() {
		NewDrawingState state = getCurrentState();
		ArrayList<String> paths = state.getPaths();
		boolean mulipleParents = paths.size() > 1;
		for (String path : paths) {
			BaseFigure parentFigure = null;
			if (mulipleParents) {
				parentFigure = figureFactory.createParentFigure(path);
				drawing.add(parentFigure);
			}
			ArrayList<BaseFigure> figures = state.getFiguresByPath(path);
			for (BaseFigure figure : figures) {
				drawing.add(figure);
				if (mulipleParents) {
					parentFigure.add(figure);
				}
			}
		}
	}

	protected void addFigure(String parentPath, AbstractDTO dto) {
		try {
			BaseFigure figure = figureFactory.createFigure(dto);
			getCurrentState().addFigure(parentPath, figure, dto);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	protected void addContextFigure(BaseFigure contextFigure) {
		getCurrentState().addContextFigure(contextFigure);
	}

	protected void addDependency(DependencyDTO[] dependencyDTOs, BaseFigure figureFrom, BaseFigure figureTo) {
		try {
			RelationFigure dependencyFigure = figureFactory.createFigure(dependencyDTOs);
			getCurrentState().addDependency(dependencyFigure, dependencyDTOs);
			drawing.addRelation(dependencyFigure, figureFrom, figureTo);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	protected void addViolation(ViolationDTO[] violationDTOs, BaseFigure figureFrom, BaseFigure figureTo) {
		NewDrawingState state = getCurrentState();
		try {
			if (figureFrom.equals(figureTo)) {
				try {
					figureFrom.addDecorator(figureFactory.createViolationsDecorator(violationDTOs));
					state.addViolatedFigure(figureFrom, violationDTOs);
				} catch (Exception e) {
					logger.error("Could not attach decorator to figure to indicate internal violations.", e);
				}
			}else{
				RelationFigure violationFigure = figureFactory.createFigure(violationDTOs);
				state.addViolation(violationFigure, violationDTOs);
				drawing.addRelation(violationFigure, figureFrom, figureTo);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	protected ArrayList<String> getParentNamesHelper(ArrayList<BaseFigure> figures) {
		ArrayList<String> names = new ArrayList<String>();
		for (BaseFigure figure : figures) {
			AbstractDTO dto = getCurrentState().getFigureDTO(figure);
			if (dto instanceof ModuleDTO) {
				names.add(((ModuleDTO) dto).logicalPath);
			} else if (dto instanceof AnalysedModuleDTO) {
				names.add(((AnalysedModuleDTO) dto).uniqueName);
			}
		}
		return names;
	}

	protected String createCombinedPathHelper(ArrayList<String> parents) {
		String s = "";
		for (String parent : parents) {
			if (!parent.isEmpty()) {
				s += parent + "+";
			}
		}
		if (s.length() > 0) {
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}
}
