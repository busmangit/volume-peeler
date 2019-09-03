import ij.*;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import ij.process.*;
import ij.plugin.Duplicator;
import ij.plugin.ZProjector;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ExtendedPlugInFilter;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.measure.SplineFitter;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Line;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.gui.TextRoi;
import ij.plugin.PlugIn; 
import ij.plugin.*; 

import java.awt.*;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;

public class Proyeccion_General_Final implements PlugInFilter, ActionListener {

  private static int tiempos;
  private static int width;
  private static int height;
  private static int zs;

  private ImagePlus image;
  private ImageStack stack;
  
  public int setup(String arg, ImagePlus imp) {
    return STACK_REQUIRED | DOES_ALL;
  }
      
  public void run(ImageProcessor ip) {
    image = WindowManager.getCurrentImage(); 
    stack = image.getStack();
    tiempos = image.getNFrames();
    zs = stack.getSize()/tiempos;
    width = stack.getWidth();
    height = stack.getHeight();
    mostrarProyecciones();
  }

  private ImagePlus impProyecciones;

  private void mostrarProyecciones() {
    ImageStack stackProyecciones = new ImageStack(width, height);     
    for(int z = 0; z < tiempos; z++) {
      ZProjector projector = new ZProjector(image); 
      projector.setMethod(ZProjector.MAX_METHOD);
      projector.setStartSlice(z * zs);
      projector.setStopSlice((z + 1) * zs - 1);
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

  private int[][] profundidades;
  private Button previewButton;

  private Panel construirBotonera() {
    profundidades = new int[tiempos][9];
    Panel container = new Panel();
    container.setLayout(new FlowLayout());
    Panel theNumbers = new Panel();
    theNumbers.setLayout(new GridLayout(4, 3));
    for (int i = 0; i < 9; i++) {
      TextField tfQuadrant = new TextField("" + profundidades[0][i]);
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
    if (e.getSource() == previewButton) {
      preview();
    }
    else {
      int sliceIndex = impProyecciones.getCurrentSlice() - 1;
      int quadrantIndex = Integer.parseInt(((TextField)(e.getSource())).getName());
      profundidades[sliceIndex][quadrantIndex]++;
      imprimirProfundidades();
    }
  }
  
  private void preview() {
    System.out.println("preview");
    for (int i = 0; i < profundidades.length; i++) {
      for (int j = 0; j < profundidades[i].length; j++) {
        if (profundidades[i][j] > 0) {
          System.out.println("Hay que procesar la slice " + i);
          break;
        }
      }
    }
  }
  
  private void imprimirProfundidades() {
    for (int i = 0; i < profundidades.length; i++) {
      for (int j = 0; j < profundidades[i].length; j++) {
        System.out.print(profundidades[i][j] + ", ");
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
