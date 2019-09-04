import ij.*;
import ij.process.*;
import ij.plugin.*; 
import ij.plugin.filter.PlugInFilter;
import ij.gui.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import flanagan.interpolation.*;

public class Proyeccion_General_Final implements PlugInFilter, ActionListener, KeyListener, ImageListener {

  private static int frames, width, height, slices;

  private ImagePlus originalImage, processedImage;
  private ImagePlus impProyecciones;

  private Button previewButton;
  private int[][] offset;
  private TextField[] tfQuadrant;
  
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
    initOffsetsMatrix();
    mostrarProyecciones();
  }

  private void initOffsetsMatrix() {
    offset = new int[frames][9];
    for (int frame = 0; frame < frames; frame++) {
      for (int i = 0; i < 9; i++) {
        offset[frame][i] = 1;//slices;// / 2;
      }
    }
  }

  private void mostrarProyecciones() {
    ImageStack stackProyecciones = new ImageStack(width, height);
    ZProjector projector = new ZProjector(originalImage); 
    for(int z = 0; z < frames; z++) {
      projector.setStartSlice(z * slices);
      projector.setStopSlice((z + 1) * slices - 1);
      projector.doProjection();
      stackProyecciones.addSlice(projector.getProjection().getProcessor());          
    }
    impProyecciones = new ImagePlus("Nice plugin", stackProyecciones);
    impProyecciones.show();
    impProyecciones.addImageListener(this);
    construirOverlay();
    buildUI();
  }

  private void updateOffsets(int slice) {
    for (int i = 0; i < 9; i++) {
      tfQuadrant[i].setText("" + offset[slice - 1][i]);
    }
  }

  private void construirOverlay() {
    Overlay overlay = new Overlay();
    agregarLineaAOverlay(overlay, width / 3, 0, width / 3, height);
    agregarLineaAOverlay(overlay, 2 * width / 3, 0, 2 * width / 3, height);
    agregarLineaAOverlay(overlay, 0, height / 3, width, height /3);
    agregarLineaAOverlay(overlay, 0, 2 * height / 3, width, 2 * height /3);
    impProyecciones.setOverlay(overlay);
  }

  private void agregarLineaAOverlay(Overlay overlay, int x1, int y1, int x2, int y2) {
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
    CheckboxGroup choice = new CheckboxGroup();
    Checkbox posterior = new Checkbox("Keep posterior part", choice, true);
    Checkbox anterior = new Checkbox("Keep anterior part", choice, false);
    Panel choiceContainer = new Panel(new GridLayout(2, 1));
    previewButton = new Button("Preview");
    previewButton.addActionListener(this);
    choiceContainer.add(anterior);
    choiceContainer.add(posterior);
    container.add(theNumbers);
    container.add(choiceContainer);
    container.add(previewButton);
    container.add(new Button("Process all frames"));
    impProyecciones.getWindow().add(container);
    impProyecciones.getWindow().pack();
  }
  
  @Override
  public void actionPerformed(ActionEvent e) {
    int sliceIndex = impProyecciones.getCurrentSlice();
    if (e.getSource() == previewButton) {
      preview(sliceIndex);
    }
  }
  
  private void preview(int frame) {
    int f = frame - 1;
    double[] x1Data = { 0, width / 6, width / 2, 5 * width / 6, width };
    double[] x2Data = { 0, height / 6, height / 2, 5 * height / 6, height };
    double[][] yData = {
      { offset[f][0], offset[f][0], offset[f][1], offset[f][2], offset[f][2] },
      { offset[f][0], offset[f][0], offset[f][1], offset[f][2], offset[f][2] },
      { offset[f][3], offset[f][3], offset[f][4], offset[f][5], offset[f][5] },
      { offset[f][6], offset[f][6], offset[f][7], offset[f][8], offset[f][8] },
      { offset[f][6], offset[f][6], offset[f][7], offset[f][8], offset[f][8] }
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
          if (Math.round(interps[i][j]) >= k) {
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
    impProyecciones.getStack().setProcessor(projector.getProjection().getProcessor(), f + 1);
    impProyecciones.updateAndDraw();
  }
  
  private void imprimirProfundidades() {
    for (int i = 0; i < offset.length; i++) {
      for (int j = 0; j < offset[i].length; j++) {
        System.out.print(offset[i][j] + ", ");
      }
      System.out.println();
    }
  }
  
  public static void main(String[] args) {
    new ImageJ();
    ImagePlus image = IJ.openImage(args[0]);      
    IJ.runPlugIn(image, "Proyeccion_General_Final", "parameter=value");
    WindowManager.addWindow(image.getWindow());
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
    int sliceIndex = impProyecciones.getCurrentSlice();
    switch (e.getKeyChar()) {
      case '+':
        offset[sliceIndex - 1][quadrantIndex]++;
        break;
      case '-':
        offset[sliceIndex - 1][quadrantIndex]--;
        break;
      default:
        break;
    }
    imprimirProfundidades();
  }

  @Override
  public void keyTyped(KeyEvent e) {}

  private void print(String s) {
    System.out.println(s);
  }
}
