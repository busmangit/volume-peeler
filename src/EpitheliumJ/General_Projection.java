package EpitheliumJ;
import ij.*;
import ij.process.*;
import ij.plugin.*; 
import ij.plugin.filter.PlugInFilter;
import ij.gui.*;
import ij.measure.ResultsTable;

import java.awt.*;
import java.awt.event.*;

public class General_Projection
implements PlugInFilter, ActionListener, KeyListener, ItemListener, ImageListener, MouseListener {

  private static int frames, width, height, slices;
  private final static String PLUGIN_NAME = "EpitheliumJ.General_Projection";
  private final static String WINDOW_TITLE = PLUGIN_NAME;

  private ImagePlus originalImage, processedImage;
  private ImagePlus projectionsImage;

  private Button previewButton, processButton, repeatValuesButton, copyMatrixButton;
  private int toRepeatValue=0;
  private int[][] offset;
  private TextField[] tfQuadrant;
  private boolean[] frameEnabled;
  private Checkbox frameEnabledCheckbox, anteriorCheckbox, posteriorCheckbox;
  private boolean keepAnteriorPart = false;
  
  public int setup(String arg, ImagePlus imp) {
    return STACK_REQUIRED | DOES_ALL;
  }
  
  public void run(ImageProcessor ip) {
    ImagePlus image = WindowManager.getCurrentImage(); 
    originalImage = image.duplicate();
    processedImage = image.duplicate();
    frames = image.getNFrames();
    slices = image.getStack().getSize() / frames;
    width = image.getStack().getWidth();
    height = image.getStack().getHeight();
    frameEnabled = new boolean[frames];
    frameEnabled[0] = frameEnabled[frames - 1] = true;
    initOffsetsMatrix();
    showProjections();
  }

  private void initOffsetsMatrix() {
    offset = new int[frames][9];
    for (int frame = 0; frame < frames; frame++) {
      for (int i = 0; i < 9; i++) {
        offset[frame][i] = slices;
      }
    }
  }

  private void showProjections() {
    ImageStack projectionsStack = new ImageStack(width, height);
    ZProjector projector = new ZProjector(originalImage); 
    for(int z = 0; z < frames; z++) {
      projector.setStartSlice(z * slices);
      projector.setStopSlice((z + 1) * slices - 1);
      projector.doProjection();
      projectionsStack.addSlice(projector.getProjection().getProcessor());          
    }
    projectionsImage = new ImagePlus(WINDOW_TITLE, projectionsStack);
    projectionsImage.show();
    projectionsImage.addImageListener(this);
    buildOverlay();
    buildUI();
  }

  private void updateOffsets(int slice) {
	  
    this.frameEnabledCheckbox.setState(frameEnabled[slice - 1]);
    this.previewButton.setEnabled(frameEnabled[slice - 1]);
    this.repeatValuesButton.setEnabled(false);
    
    //saca el cursos
    for (int i = 0; i < 9; i++) {
      tfQuadrant[i].transferFocus();
    }
    
    //desactiva/activa los textField y cambia los valores segun el frame
    for (int i = 0; i < 9; i++) {
        tfQuadrant[i].setEnabled(frameEnabled[slice - 1]);
        tfQuadrant[i].setText("" + offset[slice - 1][i]);
      }
  }

  private void buildOverlay() {
    Overlay overlay = new Overlay();
    drawLineInOverlay(overlay, width / 3, 0, width / 3, height);
    drawLineInOverlay(overlay, 2 * width / 3, 0, 2 * width / 3, height);
    drawLineInOverlay(overlay, 0, height / 3, width, height / 3);
    drawLineInOverlay(overlay, 0, 2 * height / 3, width, 2 * height / 3);
    projectionsImage.setOverlay(overlay);
  }

  private void drawLineInOverlay(Overlay overlay, int x1, int y1, int x2, int y2) {
    Roi line = new Line(x1, y1, x2, y2);
    line.setStrokeColor(Color.green);
    line.setStrokeWidth(1);
    overlay.add(line);
  }

  private void buildUI() {
    tfQuadrant = new TextField[9];
    Panel container = new Panel();
    container.setLayout(new FlowLayout());
    Panel theNumbers = new Panel();
    theNumbers.setLayout(new GridLayout(3, 3));
    for (int i = 0; i < 9; i++) {
      tfQuadrant[i] = new TextField("" + offset[0][i]);
      tfQuadrant[i].setName("" + i);
      tfQuadrant[i].addKeyListener(this);
      tfQuadrant[i].addMouseListener(this);
      theNumbers.add(tfQuadrant[i]);
    }
    this.frameEnabledCheckbox = new Checkbox("Use this frame for interpolation", frameEnabled[0]);
    this.frameEnabledCheckbox.addItemListener(this);
    
    CheckboxGroup choice = new CheckboxGroup();
    this.anteriorCheckbox = new Checkbox("Keep higher z", choice, keepAnteriorPart);
    this.anteriorCheckbox.addItemListener(this);
    this.posteriorCheckbox = new Checkbox("Keep lower z", choice, !keepAnteriorPart);
    this.posteriorCheckbox.addItemListener(this);
    
    Panel choiceContainer = new Panel(new GridLayout(2, 1));
    choiceContainer.add(anteriorCheckbox);
    choiceContainer.add(posteriorCheckbox);
    
    previewButton = new Button("Preview this frame");
    previewButton.addActionListener(this);
    processButton = new Button("Process all frames");
    processButton.addActionListener(this);
    Panel buttonsContainer = new Panel(new GridLayout(2, 1));
    buttonsContainer.add(previewButton);
    buttonsContainer.add(processButton);
    

    repeatValuesButton = new Button("Repeat z in this frame");
    repeatValuesButton.setEnabled(false);
    repeatValuesButton.addActionListener(this);
    copyMatrixButton   = new Button("Repeat matrix in all frames");
    copyMatrixButton.addActionListener(this);
    
    Panel buttonsShortcuts = new Panel(new GridLayout(2, 1));
    buttonsShortcuts.add(repeatValuesButton);
    buttonsShortcuts.add(copyMatrixButton);
    
    Panel labelsContainer = new Panel(new GridLayout(4, 1));
    labelsContainer.add(new Label("Modify Z  "));
    labelsContainer.add(new Label("matrix to "));
    labelsContainer.add(new Label("improve   "));
    labelsContainer.add(new Label("visualization."));
    
    container.add(labelsContainer);
    container.add(theNumbers);
    container.add(buttonsShortcuts);
    container.add(choiceContainer);
    container.add(buttonsContainer);

    projectionsImage.getWindow().add(frameEnabledCheckbox);
    projectionsImage.getWindow().add(container);
    
    String html="v1.1, by SCIAN-Lab 2022, Mauricio.Cerda@uchile.cl";
   
    projectionsImage.getWindow().add( new Label(html) );
    projectionsImage.getWindow().pack();
  }
  
  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == previewButton) {
      int sliceIndex = projectionsImage.getCurrentSlice();
      projectionsImage.getStack().setProcessor(preview(sliceIndex).getProcessor(), sliceIndex);
      projectionsImage.updateAndDraw();
    }
    else if (e.getSource() == processButton) {
      processAllFrames();
    }
    else if (e.getSource() == repeatValuesButton) {
    	//toRepeatValue
    	int currentFrame = projectionsImage.getCurrentSlice() - 1;
        for (int i = 0; i < 9; i++) {
            offset[currentFrame][i] = toRepeatValue;
            tfQuadrant[i].setText("" + toRepeatValue );
        }
        repeatValuesButton.setEnabled(false);

    }
    else if ( e.getSource() == copyMatrixButton) {
    	
    	int currentFrame = projectionsImage.getCurrentSlice() - 1;
    	
        for (int frame = 0; frame < frames; frame++) {
            for (int i = 0; i < 9; i++) {
              offset[frame][i] = offset[currentFrame][i];
            }
          }
    }
  }
  
  private ImagePlus preview(int frame) {
    int f = frame - 1;
    
    double[] x1Data = { width / 6, width / 2, 5 * width / 6 };
    double[] x2Data = { height / 6, height / 2, 5 * height / 6 };
    double[][] yData = {
      { offset[f][0], offset[f][3], offset[f][6] },
      { offset[f][1], offset[f][4], offset[f][7] },
      { offset[f][2], offset[f][5], offset[f][8] }
    };
    BiCubicSpline surface = new BiCubicSpline(x1Data, x2Data, yData);
    double[][] interps = new double[width][height];
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        interps[i][j] = surface.interpolate(i, j);
      }
    }
    //double[][] pixels = new double[width][height];
    ImageStack stack = processedImage.getStack();
    for (int k = 0; k < slices; k++) {
      int z = f * slices + k;
      
      for (int i = 0; i < width; i++) {
        for (int j = 0; j < height; j++) {
          if ((keepAnteriorPart && Math.round(interps[i][j]) >= k+1) ||
             (!keepAnteriorPart && Math.round(interps[i][j]) <= k+1)) {
            stack.setVoxel(i, j, z, 0);
          }
          else {
            stack.setVoxel(i, j, z, originalImage.getStack().getVoxel(i, j, z));
          }
        }
      }
    }
    processedImage.setStack(stack);
    
    ZProjector projector = new ZProjector(processedImage); 
    projector.setMethod(ZProjector.MAX_METHOD);
    projector.setStartSlice(f * slices + 1);
    projector.setStopSlice((f + 1) * slices);
    projector.doProjection();
    
    ImagePlus result = projector.getProjection();
    
    //normalizacion?
    //ContrastEnhancer ce = new ContrastEnhancer();
    //ce.setNormalize(true);
    //ce.stretchHistogram(result, 0.1);
    
    return result;
  }
  
  private void processAllFrames() {

	  ResultsTable table = new ResultsTable();

	  for (int cuadrante = 0; cuadrante < 9; cuadrante++) {
		  int frameInicioInterpolacion = 0;
		  for (int frame = 1; frame < frames; frame++) {
			  if (frameEnabled[frame]) {
				  double m = (offset[frame][cuadrante] - offset[frameInicioInterpolacion][cuadrante]) / (1.0 * (frame - frameInicioInterpolacion));
				  for (int i = frameInicioInterpolacion + 1; i < frame; i++) {
					  offset[i][cuadrante] = (int)(offset[frameInicioInterpolacion][cuadrante] + m * (i - frameInicioInterpolacion));
				  }
				  frameInicioInterpolacion = frame;
			  }
		  }
	  }
	  ImageStack projectionsStack = new ImageStack(width, height);
	  for (int frame = 1; frame <= frames; frame++) {
		  projectionsStack.addSlice(preview(frame).getProcessor());
		  addRowToTable(table, frame);
	  }
	  
	  
	  ImagePlus result = new ImagePlus("Result", projectionsStack);
	  result.show();
	  table.show("General Projection data");
  }

  private void addRowToTable(ResultsTable table, int frame) {
	  table.incrementCounter();
	  table.addValue("Frame", frame);
	  table.addValue("Z11", offset[frame-1][0]);
	  table.addValue("Z12", offset[frame-1][1]);
	  table.addValue("Z13", offset[frame-1][2]);
	  table.addValue("Z21", offset[frame-1][3]);
	  table.addValue("Z22", offset[frame-1][4]);
	  table.addValue("Z23", offset[frame-1][5]);
	  table.addValue("Z31", offset[frame-1][6]);
	  table.addValue("Z32", offset[frame-1][7]);
	  table.addValue("Z33", offset[frame-1][8]);
	  table.addValue("Higher", ""+this.keepAnteriorPart);

  }
  
  @Override
  public void imageClosed(ImagePlus imp) {}

  @Override
  public void imageOpened(ImagePlus imp) {}

  @Override
  public void imageUpdated(ImagePlus imp) {
    if (WindowManager.getCurrentImage().getTitle().indexOf(WINDOW_TITLE) == 0) {
      updateOffsets(imp.getCurrentSlice());
    }
  }

  @Override
  public void keyPressed(KeyEvent e) {}

  @Override
  public void keyReleased(KeyEvent e) {
    int quadrantIndex = Integer.parseInt(((TextField)(e.getSource())).getName());
    int sliceIndex = projectionsImage.getCurrentSlice();
    try {
      offset[sliceIndex - 1][quadrantIndex] = Integer.parseInt(((TextField)e.getSource()).getText());
    }
    catch (Exception ex) {
    }
    
    try {
	toRepeatValue=Integer.parseInt( ((TextField)(e.getSource())).getText() );
    }
    catch (Exception ex) {
    	toRepeatValue=slices;
    }

    
	if (toRepeatValue > slices) {
		toRepeatValue = slices;
	}
	if ( toRepeatValue < 1) {
		toRepeatValue = 1;
	}
  }

  @Override
  public void keyTyped(KeyEvent e) {}

  @Override
  public void itemStateChanged(ItemEvent e) {
    if (e.getSource() == anteriorCheckbox) {
      keepAnteriorPart = true;
    }
    else if (e.getSource() == posteriorCheckbox) {
      keepAnteriorPart = false;
    }
    else if (e.getSource() == frameEnabledCheckbox) {
      boolean value = frameEnabledCheckbox.getState();
      int currentFrame = projectionsImage.getCurrentSlice() - 1;
      if (currentFrame == 0 || currentFrame == frames - 1) {
        frameEnabledCheckbox.setState(true);
        return;
      }
      frameEnabled[currentFrame] = value;
      for (int i = 0; i < 9; i++) {
        tfQuadrant[i].setEnabled(value);
      }
      previewButton.setEnabled(value);
    }
  }
  
  public static void main(String[] args) {
    new ImageJ();
    ImagePlus image = IJ.openImage(args[0]);      
    IJ.runPlugIn(image, PLUGIN_NAME, "parameter=value");
    WindowManager.addWindow(image.getWindow());
  }

@Override
public void mouseClicked(MouseEvent e) {

    try {
	toRepeatValue=Integer.parseInt( ((TextField)(e.getSource())).getText() );
    }
    catch (Exception ex) {
    	toRepeatValue=slices;
    }
    
	repeatValuesButton.setEnabled(true);

}

@Override
public void mousePressed(MouseEvent e) {
	// TODO Auto-generated method stub
	
}

@Override
public void mouseReleased(MouseEvent e) {
	
    try {
	toRepeatValue=Integer.parseInt( ((TextField)(e.getSource())).getText() );
    }
    catch (Exception ex) {
    	toRepeatValue=slices;
    }
    
	repeatValuesButton.setEnabled(true);
	
}

@Override
public void mouseEntered(MouseEvent e) {
	// TODO Auto-generated method stub
	
}

@Override
public void mouseExited(MouseEvent e) {
	// TODO Auto-generated method stub
	
}  
}
