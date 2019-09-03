import ij.*;
import ij.process.*;
import ij.plugin.*; 
import ij.plugin.filter.PlugInFilter;
import ij.gui.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import flanagan.interpolation.*;

public class Proyeccion_General_Final implements PlugInFilter, ActionListener {

  private static int frames, width, height, slices;

  private ImagePlus originalImage, processedImage;
  private ImagePlus impProyecciones;
  
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
    mostrarProyecciones();
  }

  private void mostrarProyecciones() {
    ImageStack stackProyecciones = new ImageStack(width, height);     
    for(int z = 0; z < frames; z++) {
      ZProjector projector = new ZProjector(originalImage); 
      projector.setMethod(ZProjector.MAX_METHOD);
      projector.setStartSlice(z * slices);
      projector.setStopSlice((z + 1) * slices - 1);
      projector.doProjection();
      stackProyecciones.addSlice(projector.getProjection().getProcessor());          
    }
    impProyecciones = new ImagePlus("Nice plugin", stackProyecciones);
    impProyecciones.show();
    construirOverlay(impProyecciones);
    construirInterfaz(impProyecciones);
  }

  private void construirOverlay(ImagePlus imp) {
    Overlay overlay = new Overlay();
    agregarLineaAOverlay(overlay, width / 3, 0, width / 3, height);
    agregarLineaAOverlay(overlay, 2 * width / 3, 0, 2 * width / 3, height);
    agregarLineaAOverlay(overlay, 0, height / 3, width, height /3);
    agregarLineaAOverlay(overlay, 0, 2 * height / 3, width, 2 * height /3);
    imp.setOverlay(overlay);
  }

  private void agregarLineaAOverlay(Overlay overlay, int x1, int y1, int x2, int y2) {
    Roi line = new Line(x1, y1, x2, y2);
    line.setStrokeColor(Color.green);
    line.setStrokeWidth(1);
    overlay.add(line);
  }

  private void construirInterfaz(ImagePlus impProyecciones) {
    Panel botonera = construirBotonera();
    impProyecciones.getWindow().add(botonera);
    impProyecciones.getWindow().pack();
  }

  private int[][] offset;
  private int[] base;
  private Button previewButton;

  private Panel construirBotonera() {
    offset = new int[frames][9];
    base = new int[frames];
    for (int i = 0; i < frames; i++) {
      base[i] = 1;
    }
    Panel container = new Panel();
    container.setLayout(new FlowLayout());
    Panel theNumbers = new Panel();
    theNumbers.setLayout(new GridLayout(4, 3));
    for (int i = 0; i < 9; i++) {
      TextField tfQuadrant = new TextField("" + offset[0][i]);
      tfQuadrant.setName("" + i);
      tfQuadrant.addActionListener(this);
      theNumbers.add(tfQuadrant);
    }
    CheckboxGroup choice = new CheckboxGroup();
    Checkbox posterior = new Checkbox("Keep posterior part", choice, true);
    Checkbox anterior = new Checkbox("Keep anterior part", choice, false);
    Panel choiceContainer = new Panel(new GridLayout(3, 1));
    previewButton = new Button("Preview");
    previewButton.addActionListener(this);
    choiceContainer.add(anterior);
    choiceContainer.add(posterior);
    choiceContainer.add(previewButton);
    container.add(theNumbers);
    container.add(choiceContainer);
    container.add(new Button("Process all frames"));
    return container;
  }
  
  @Override
  public void actionPerformed(ActionEvent e) {
    int sliceIndex = impProyecciones.getCurrentSlice() - 1;
    if (e.getSource() == previewButton) {
      preview(sliceIndex);
    }
    else {
      int quadrantIndex = Integer.parseInt(((TextField)(e.getSource())).getName());
      offset[sliceIndex][quadrantIndex]++;
      imprimirProfundidades();
    }
  }
  
  private void preview(int frame) {
    int b = base[frame];
    double[] x1Data = { 0, width / 6, width / 2, 5 * width / 6, width };
    double[] x2Data = { 0, height / 6, height / 2, 5 * height / 6, height };
    double[][] yData = {
      { b, b                   , b                   , b                   , b },
      { b, b + offset[frame][0], b + offset[frame][1], b + offset[frame][2], b },
      { b, b + offset[frame][3], b + offset[frame][4], b + offset[frame][5], b },
      { b, b + offset[frame][6], b + offset[frame][7], b + offset[frame][8], b },
      { b, b                   , b                   , b                   , b }
    };
    BiCubicSpline surface = new BiCubicSpline(x1Data, x2Data, yData);
    double[][] interps = new double[width][height];
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        interps[i][j] = surface.interpolate(i, j);
      }
    }
    double[][] pixels = new double[width][height];
    for (int k = 0; k < slices; k++) {
      int z = frame * slices + k;
      for (int i = 0; i < width; i++) {
        for (int j = 0; j < height; j++) {
          if (interps[i][j] <= k) {
            processedImage.getStack().setVoxel(i, j, z, 0);
          }
          else {
            processedImage.getStack().setVoxel(i, j, z, originalImage.getStack().getVoxel(i, j, z));
          }
        }
      }
    }
    ZProjector projector = new ZProjector(processedImage); 
    projector.setMethod(ZProjector.MAX_METHOD);
    projector.setStartSlice(frame * slices);
    projector.setStopSlice((frame + 1) * slices - 1);
    projector.doProjection();
    impProyecciones.getStack().setProcessor(projector.getProjection().getProcessor(), frame + 1);
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
}
