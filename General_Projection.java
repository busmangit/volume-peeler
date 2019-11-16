import ij.*;
import ij.process.*;
import javafx.scene.control.CheckBox;
import ij.plugin.*; 
import ij.plugin.filter.PlugInFilter;
import ij.gui.*;

import java.awt.*;
import java.awt.event.*;

//import flanagan.interpolation.*;

public class Proyeccion_General_Final
implements PlugInFilter, ActionListener, KeyListener, ItemListener, ImageListener {

  private static int frames, width, height, slices;

  private ImagePlus originalImage, processedImage;
  private ImagePlus projectionsImage;

  private Button previewButton, processButton;
  private int[][] offset;
  private TextField[] tfQuadrant;
  private boolean[] frameEnabled;
  private Checkbox frameEnabledCheckbox, anteriorCheckbox, posteriorCheckbox;
  private boolean keepAnteriorPart = true;
  
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
    for (int i = 0; i < frames; i++) {
      frameEnabled[i] = false;
    }
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
    projectionsImage = new ImagePlus("Nice plugin", projectionsStack);
    projectionsImage.show();
    projectionsImage.addImageListener(this);
    buildOverlay();
    buildUI();
  }

  private void updateOffsets(int slice) {
    this.frameEnabledCheckbox.setState(frameEnabled[slice - 1]);
    this.previewButton.setEnabled(frameEnabled[slice - 1]);
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
      theNumbers.add(tfQuadrant[i]);
    }
    this.frameEnabledCheckbox = new Checkbox("Use this frame for interpolation", frameEnabled[0]);
    this.frameEnabledCheckbox.addItemListener(this);
    CheckboxGroup choice = new CheckboxGroup();
    this.anteriorCheckbox = new Checkbox("Keep anterior part", choice, keepAnteriorPart);
    this.anteriorCheckbox.addItemListener(this);
    this.posteriorCheckbox = new Checkbox("Keep posterior part", choice, !keepAnteriorPart);
    this.posteriorCheckbox.addItemListener(this);
    Panel choiceContainer = new Panel(new GridLayout(2, 1));
    previewButton = new Button("Preview this frame");
    previewButton.addActionListener(this);
    processButton = new Button("Process all frames");
    processButton.addActionListener(this);
    choiceContainer.add(anteriorCheckbox);
    choiceContainer.add(posteriorCheckbox);
    container.add(theNumbers);
    container.add(previewButton);
    container.add(choiceContainer);
    container.add(processButton);

    Panel baseThresholdPanel = new Panel();
    baseThresholdPanel.setLayout(new GridLayout(1, 3));
    baseThresholdPanel.add(new Label("Base threshold:"));
    baseThresholdPanel.add(new TextField("15"));
    baseThresholdPanel.add(new Button("Set"));

    projectionsImage.getWindow().add(frameEnabledCheckbox);
    projectionsImage.getWindow().add(baseThresholdPanel);
    projectionsImage.getWindow().add(container);
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
    double[][] pixels = new double[width][height];
    for (int k = 1; k <= slices; k++) {
      int z = f * slices + k;
      for (int i = 0; i < width; i++) {
        for (int j = 0; j < height; j++) {
          if ((keepAnteriorPart && Math.round(interps[i][j]) >= k) ||
            (!keepAnteriorPart && Math.round(interps[i][j]) <= k)) {
            processedImage.getStack().setVoxel(i, j, z, 0);
          }
          else {
            processedImage.getStack().setVoxel(i, j, z, originalImage.getStack().getVoxel(i, j, z));
          }
        }
      }
    }
    ZProjector projector = new ZProjector(processedImage); 
    projector.setStartSlice(f * slices + 1);
    projector.setStopSlice((f + 1) * slices);
    projector.doProjection();
    return projector.getProjection();
  }
  
  private void processAllFrames() {
    System.out.println("process");
    int frameInicioInterpolacion = 0;
    for (int cuadrante = 0; cuadrante < 9; cuadrante++) {
      for (int frame = 1; frame < frames; frame++) {
        if (frameEnabled[frame]) {
          for (int i = frameInicioInterpolacion + 1; i < frame - 1; i++) {
            int m = (offset[frame][cuadrante] - offset[frameInicioInterpolacion][cuadrante]) / (frame - frameInicioInterpolacion);
            offset[i][cuadrante] = offset[frameInicioInterpolacion][cuadrante] + m * (i - frameInicioInterpolacion);
          }
          frameInicioInterpolacion = frame;
        }
      }
    }
    ImageStack projectionsStack = new ImageStack(width, height);
    for(int z = 1; z <= frames; z++) {
      projectionsStack.addSlice(preview(z).getProcessor());
    }
    ImagePlus result = new ImagePlus("Nice plugin", projectionsStack);
    result.show();
  }

  @Override
  public void imageClosed(ImagePlus imp) {}

  @Override
  public void imageOpened(ImagePlus imp) {}

  @Override
  public void imageUpdated(ImagePlus imp) {
    updateOffsets(imp.getCurrentSlice());
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
      frameEnabled[projectionsImage.getCurrentSlice() - 1] = value;
      for (int i = 0; i < 9; i++) {
        tfQuadrant[i].setEnabled(value);
      }
      previewButton.setEnabled(value);
    }
  }
  
  public static void main(String[] args) {
    new ImageJ();
    ImagePlus image = IJ.openImage(args[0]);      
    IJ.runPlugIn(image, "Proyeccion_General_Final", "parameter=value");
    WindowManager.addWindow(image.getWindow());
  }  
}
